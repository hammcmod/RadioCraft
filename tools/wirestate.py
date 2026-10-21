"""Reads the blockstate of RadioCraft wire blocks on the dev server over RCON.

    python tools/wirestate.py <x> <y> <z> [<x> <y> <z> ...]

Prints one line per position: the wire kind and every property that is set (flags that are true, hug/arrive/end
when not none). Uses `execute if block` probes, so it needs no mod support; a non-wire block prints as such.
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
WIRES = ["coax_wire", "wire", "waterproof_wire"]
FLAGS = ["north", "east", "south", "west", "up", "down"]
EDGES = ["north", "east", "south", "west"]
ENDS = ["climb_wire", "climb_device", "up_device"]


def rcon(cmd):
    out = subprocess.run([sys.executable, os.path.join(HERE, "rcon.py"), cmd], capture_output=True, text=True)
    return (out.stdout + out.stderr).strip()


def is_block(pos, spec):
    return "Test passed" in rcon(f"execute if block {pos} {spec}")


def describe(x, y, z):
    pos = f"{x} {y} {z}"
    kind = next((w for w in WIRES if is_block(pos, f"radiocraft:{w}")), None)
    if kind is None:
        return f"{pos}: not a wire"
    parts = [f for f in FLAGS if is_block(pos, f"radiocraft:{kind}[{f}=true]")]
    for prop in ("hug", "arrive"):
        for e in EDGES:
            if is_block(pos, f"radiocraft:{kind}[{prop}={e}]"):
                parts.append(f"{prop}={e}")
    for e in ENDS:
        if is_block(pos, f"radiocraft:{kind}[end={e}]"):
            parts.append(f"end={e}")
    return f"{pos}: {kind} " + " ".join(parts)


if __name__ == "__main__":
    args = sys.argv[1:]
    for i in range(0, len(args), 3):
        print(describe(*args[i:i + 3]))
