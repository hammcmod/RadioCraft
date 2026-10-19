#!/usr/bin/env python3
"""Minimal RCON client for the RadioCraft dev server (no dependencies).

Usage:
    python tools/rcon.py "rcdebug solar" "time query daytime"   # one or more commands
    echo "list" | python tools/rcon.py                           # commands from stdin, one per line

Connection defaults match tools/devserver.sh; override with RCON_HOST, RCON_PORT, RCON_PASSWORD.
Exit code is 1 if the server is unreachable or authentication fails.
"""
import os
import re
import socket
import struct
import sys

HOST = os.environ.get("RCON_HOST", "127.0.0.1")
PORT = int(os.environ.get("RCON_PORT", "25575"))
PASSWORD = os.environ.get("RCON_PASSWORD", "radiocraft-dev")

AUTH, COMMAND = 3, 2
COLOR_CODES = re.compile("§.")


def send(sock, req_id, kind, body):
    payload = struct.pack("<ii", req_id, kind) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def recv_exact(sock, n):
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("connection closed")
        data += chunk
    return data


def recv(sock):
    length = struct.unpack("<i", recv_exact(sock, 4))[0]
    data = recv_exact(sock, length)
    req_id, _kind = struct.unpack("<ii", data[:8])
    return req_id, data[8:-2].decode("utf-8", "replace")


def run(commands):
    with socket.create_connection((HOST, PORT), timeout=10) as sock:
        send(sock, 1, AUTH, PASSWORD)
        if recv(sock)[0] == -1:
            print("rcon: authentication failed", file=sys.stderr)
            return 1
        for i, cmd in enumerate(commands, start=2):
            send(sock, i, COMMAND, cmd.lstrip("/"))
            # Minecraft splits long replies across packets; read until the socket goes quiet.
            parts = [recv(sock)[1]]
            sock.settimeout(0.3)
            try:
                while True:
                    parts.append(recv(sock)[1])
            except (socket.timeout, TimeoutError):
                pass
            sock.settimeout(10)
            reply = COLOR_CODES.sub("", "".join(parts)).strip()
            print(f"> {cmd}")
            if reply:
                print(reply)
    return 0


if __name__ == "__main__":
    cmds = sys.argv[1:] or [line.strip() for line in sys.stdin if line.strip()]
    if not cmds:
        print(__doc__)
        sys.exit(2)
    try:
        sys.exit(run(cmds))
    except OSError as e:
        print(f"rcon: cannot reach {HOST}:{PORT} ({e}). Is the dev server running? (tools/devserver.sh start)", file=sys.stderr)
        sys.exit(1)
