#!/usr/bin/env python3
"""Launch the built mod jar in a real, non-dev NeoForge install, the way players run it.

The dev runs (runClient, runGameTestServer) load the mod from build/classes, so they never see the shipped jar: a
broken jar-in-jar (1.9.1, "Failed to load mod file META-INF/jarjar/sable-companion-...") or a module clash with the
client's own libraries (1.9.0, "reads more than one module named jorbis", issue #101) passed them. This test installs
NeoForge from its installer, puts the shipped jar and only the required mods into mods/ (no Sable, Create or other
optional mods), and starts it:

  server  a headless dedicated server: waits for "Done", places a RadioCraft block from the console, stops it.
  client  a production client launched from the version json like a launcher does (offline account, no sound
          assets): waits for the resource reload after mod loading, then closes it. Needs a display.

Fails on an early exit, a timeout, a crash report or any ERROR from the mod loader. Usually run through Gradle:
    ./gradlew prodLaunchTest                     (server and client)
    ./gradlew prodLaunchTest -PlaunchSide=server (CI, no display)
Directly:
    python tools/prod_launch_test.py --jar build/libs/x.jar --mod voicechat.jar --mod geckolib.jar \\
        --neoforge 21.1.248 --mc 1.21.1 --side both
Installs are cached under --work (build/prod-launch) per NeoForge version.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import queue
import re
import shutil
import socket
import subprocess
import sys
import threading
import time
import urllib.request
import zipfile
from pathlib import Path

NEO_MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge/{v}/neoforge-{v}-installer.jar"
MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

# Log lines that mean the mod did not load, whatever their level.
FATAL_PATTERNS = [
    re.compile(p) for p in (
        r"Failed to load mod file",
        r"reads more than one module named",
        r"Mod loading has failed|Loading errors encountered|ModLoadingException",
        r"Crash report saved to|---- Minecraft Crash Report ----",
        r"Exception in thread \"main\"",
        r"Mixin apply failed|MixinApplyError|InvalidMixinException",
        r"Missing or unsupported mandatory dependencies",
    )
]
ERROR_LINE = re.compile(r"/(ERROR|FATAL)\]")
# ERROR lines a clean vanilla + NeoForge start prints anyway; anything else fails the test.
ERROR_ALLOW = [
    re.compile(p) for p in (
        r"Failed to (load|verify) .*(authentication|profile|Realms)",  # offline account
        r"Couldn't (connect|get) .*(realms|telemetry)",
        r"Failed to fetch user properties",
        r"YggdrasilServicesKeyInfo",
    )
]


def log(msg: str) -> None:
    print(f"[launch-test] {msg}", flush=True)


def fetch(url: str, dest: Path, sha1: str | None = None) -> Path:
    if dest.is_file() and (sha1 is None or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_suffix(dest.suffix + ".part")
    with urllib.request.urlopen(url, timeout=120) as r, open(tmp, "wb") as f:
        shutil.copyfileobj(r, f)
    if sha1 and hashlib.sha1(tmp.read_bytes()).hexdigest() != sha1:
        tmp.unlink()
        raise RuntimeError(f"checksum mismatch for {url}")
    tmp.replace(dest)
    return dest


def fetch_json(url: str, dest: Path, sha1: str | None = None) -> dict:
    return json.loads(fetch(url, dest, sha1).read_text(encoding="utf-8"))


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


# ---------------------------------------------------------------- library rules (version json)

OS_NAME = {"Windows": "windows", "Darwin": "osx"}.get(platform.system(), "linux")
OS_ARCH = "arm64" if platform.machine().lower() in ("arm64", "aarch64") else "x86_64"


def rules_allow(rules: list | None) -> bool:
    if not rules:
        return True
    allowed = False
    for rule in rules:
        if "features" in rule:  # demo mode, custom resolution, quick play: never wanted here
            continue
        os_rule = rule.get("os", {})
        match = True
        if "name" in os_rule and os_rule["name"] != OS_NAME:
            match = False
        if "arch" in os_rule and os_rule["arch"] != OS_ARCH:  # "x86" (32-bit) never matches
            match = False
        if match:
            allowed = rule["action"] == "allow"
    return allowed


def lib_key(name: str) -> str:
    parts = name.split(":")
    key = f"{parts[0]}:{parts[1]}"
    return key + (f":{parts[3]}" if len(parts) > 3 else "")


def lib_path(name: str) -> str:
    parts = name.split(":")
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = f"-{parts[3]}" if len(parts) > 3 else ""
    ext = "jar"
    if "@" in version:
        version, ext = version.split("@")
    return f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{classifier}.{ext}"


# ---------------------------------------------------------------- static jar checks

def game_libraries(work: Path, mc: str, neo_version_json: dict) -> set[str]:
    """group:artifact of every library the game puts on its module/class path (vanilla client + NeoForge)."""
    manifest = fetch_json(MOJANG_MANIFEST, work / "cache" / "version_manifest_v2.json")
    entry = next(v for v in manifest["versions"] if v["id"] == mc)
    vanilla = fetch_json(entry["url"], work / "cache" / f"{mc}.json", entry["sha1"])
    names = [lib["name"] for lib in vanilla["libraries"]] + [lib["name"] for lib in neo_version_json["libraries"]]
    return {":".join(n.split(":")[:2]) for n in names}


def check_jar(jar: Path, libraries: set[str]) -> list[str]:
    """Things in the jar the loader will choke on that the build's verifyModJar cannot know about."""
    problems = []
    with zipfile.ZipFile(jar) as z:
        try:
            meta = json.loads(z.read("META-INF/jarjar/metadata.json"))
        except KeyError:
            meta = {"jars": []}
        for nested in meta["jars"]:
            ident = f"{nested['identifier']['group']}:{nested['identifier']['artifact']}"
            if ident in libraries:
                problems.append(f"jar-in-jar {ident} is also a Minecraft/NeoForge library: a second module of the "
                                f"same name stops the game (issue #101). Shade and relocate it instead.")
            if nested["path"] not in z.namelist():
                problems.append(f"jar-in-jar metadata lists {nested['path']}, which is not in the jar")
    return problems


# ---------------------------------------------------------------- running a process and reading its log

class Run:
    def __init__(self, cmd: list[str], cwd: Path, log_file: Path, stdin: bool = False):
        log(f"starting in {cwd}")
        self.log_file = open(log_file, "w", encoding="utf-8", errors="replace")
        self.proc = subprocess.Popen(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                     stdin=subprocess.PIPE if stdin else subprocess.DEVNULL,
                                     text=True, encoding="utf-8", errors="replace", bufsize=1)
        self.lines: queue.Queue[str | None] = queue.Queue()
        self.errors: list[str] = []
        threading.Thread(target=self._pump, daemon=True).start()

    def _pump(self) -> None:
        for line in self.proc.stdout:
            self.log_file.write(line)
            self.lines.put(line.rstrip("\n"))
        self.lines.put(None)

    def send(self, command: str) -> None:
        self.proc.stdin.write(command + "\n")
        self.proc.stdin.flush()

    def wait_for(self, pattern: re.Pattern, deadline: float) -> str:
        """Reads lines until one matches; raises on a fatal line, process exit or the deadline."""
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise RuntimeError(f"timed out waiting for /{pattern.pattern}/")
            try:
                line = self.lines.get(timeout=min(remaining, 1.0))
            except queue.Empty:
                continue
            if line is None:
                raise RuntimeError(f"the game exited (code {self.proc.wait()}) before /{pattern.pattern}/")
            self.check_line(line)
            if pattern.search(line):
                return line

    def check_line(self, line: str) -> None:
        for p in FATAL_PATTERNS:
            if p.search(line):
                raise RuntimeError(f"fatal log line: {line}")
        if ERROR_LINE.search(line) and not any(a.search(line) for a in ERROR_ALLOW):
            self.errors.append(line)

    def drain(self, seconds: float) -> None:
        end = time.monotonic() + seconds
        while time.monotonic() < end:
            try:
                line = self.lines.get(timeout=0.2)
            except queue.Empty:
                continue
            if line is None:
                return
            self.check_line(line)

    def stop(self, graceful: str | None = None, wait: float = 60) -> None:
        if self.proc.poll() is None and graceful:
            try:
                self.send(graceful)
                self.proc.wait(timeout=wait)
            except (OSError, subprocess.TimeoutExpired):
                pass
        if self.proc.poll() is None:
            self.proc.kill()
            self.proc.wait()
        self.drain(0.5)
        self.log_file.close()


def install_mods(game_dir: Path, jar: Path, mods: list[Path]) -> None:
    mods_dir = game_dir / "mods"
    shutil.rmtree(mods_dir, ignore_errors=True)
    mods_dir.mkdir(parents=True)
    for m in [jar, *mods]:
        shutil.copy2(m, mods_dir / m.name)
    log("mods: " + ", ".join(p.name for p in sorted(mods_dir.iterdir())))


def installer(work: Path, neo: str) -> Path:
    return fetch(NEO_MAVEN.format(v=neo), work / "cache" / f"neoforge-{neo}-installer.jar")


def run_installer(inst: Path, flag: str, target: Path) -> None:
    marker = target / ".installed"
    if marker.is_file():
        return
    target.mkdir(parents=True, exist_ok=True)
    log(f"installing NeoForge ({flag}) into {target}")
    out = subprocess.run([java(), "-jar", str(inst), flag, str(target)], cwd=target,
                         capture_output=True, text=True, errors="replace")
    if out.returncode:
        raise RuntimeError(f"NeoForge installer failed:\n{out.stdout[-3000:]}\n{out.stderr[-3000:]}")
    marker.write_text("ok")


def java() -> str:
    home = os.environ.get("JAVA_HOME")
    exe = "java.exe" if os.name == "nt" else "java"
    if home and (Path(home) / "bin" / exe).is_file():
        return str(Path(home) / "bin" / exe)
    return "java"


# ---------------------------------------------------------------- server

def test_server(args, jar: Path, mods: list[Path]) -> list[str]:
    root = args.work / f"server-{args.neoforge}"
    run_installer(installer(args.work, args.neoforge), "--install-server", root)
    install_mods(root, jar, mods)
    for d in ("world", "logs", "crash-reports", "config"):
        shutil.rmtree(root / d, ignore_errors=True)
    (root / "eula.txt").write_text("eula=true\n")
    port = free_port()
    (root / "server.properties").write_text("\n".join([
        "online-mode=false", f"server-port={port}", "server-ip=127.0.0.1", "level-type=minecraft\\:flat",
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone",'
        '"height":3}],"biome":"minecraft:plains"}',
        "generate-structures=false", "spawn-protection=0", "max-tick-time=-1", "view-distance=4",
        "simulation-distance=4", "enable-rcon=false", "sync-chunk-writes=false", ""]))
    # Simple Voice Chat opens a UDP port; -1 means "the server's own port", which is free.
    (root / "config" / "voicechat").mkdir(parents=True)
    (root / "config" / "voicechat" / "voicechat-server.properties").write_text("port=-1\n")

    argfile = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    cmd = [java(), "-Xmx2G", f"@libraries/net/neoforged/neoforge/{args.neoforge}/{argfile}", "nogui"]
    run = Run(cmd, root, args.work / "server.log", stdin=True)
    try:
        deadline = time.monotonic() + args.timeout
        run.wait_for(re.compile(r"Done \(.*\)! For help"), deadline)
        log("server started; placing a RadioCraft block from the console")
        run.send(f"setblock ~ ~ ~ {args.probe_block}")
        reply = run.wait_for(re.compile(r"Changed the block|Could not set the block|Unknown block|Incorrect argument"),
                             time.monotonic() + 30)
        if "Changed the block" not in reply:
            raise RuntimeError(f"could not place {args.probe_block}: {reply}")
        run.drain(2)
    finally:
        run.stop("stop")
    return run.errors


# ---------------------------------------------------------------- client

def test_client(args, jar: Path, mods: list[Path], neo_json: dict) -> list[str]:
    root = args.work / f"client-{args.neoforge}"
    root.mkdir(parents=True, exist_ok=True)
    (root / "launcher_profiles.json").write_text('{"profiles": {}}')  # the installer refuses a folder without one
    run_installer(installer(args.work, args.neoforge), "--install-client", root)
    libs = root / "libraries"

    vanilla = json.loads((args.work / "cache" / f"{args.mc}.json").read_text(encoding="utf-8"))
    fetch(vanilla["downloads"]["client"]["url"], root / "versions" / args.mc / f"{args.mc}.jar",
                       vanilla["downloads"]["client"]["sha1"])
    index = vanilla["assetIndex"]
    objects = fetch_json(index["url"], root / "assets" / "indexes" / f"{index['id']}.json", index["sha1"])["objects"]
    # Icons, fonts and the built-in packs (about 15 MB); not the sounds and translations, which the test does not need.
    for name, obj in objects.items():
        if not name.startswith(("minecraft/sounds/", "minecraft/lang/")):
            h = obj["hash"]
            fetch(f"https://resources.download.minecraft.net/{h[:2]}/{h}", root / "assets" / "objects" / h[:2] / h, h)

    # Class path: NeoForge's libraries first, then vanilla's not overridden by them. Not the vanilla jar: NeoForge loads
    # its own patched client, and a second copy is a second module exporting the same packages.
    classpath, seen = [], set()
    for lib in neo_json["libraries"] + vanilla["libraries"]:
        if not rules_allow(lib.get("rules")) or lib_key(lib["name"]) in seen:
            continue
        seen.add(lib_key(lib["name"]))
        art = lib.get("downloads", {}).get("artifact")
        path = libs / (art["path"] if art and art.get("path") else lib_path(lib["name"]))
        if art and art.get("url"):
            fetch(art["url"], path, art.get("sha1"))
        if not path.is_file():
            raise RuntimeError(f"library missing after install: {path}")
        classpath.append(str(path))

    install_mods(root, jar, mods)
    for d in ("logs", "crash-reports", "config", "saves"):
        shutil.rmtree(root / d, ignore_errors=True)
    (root / "options.txt").write_text("onboardAccessibility:false\nsoundCategory_master:0.0\npauseOnLostFocus:false\n")
    natives = root / "natives"
    natives.mkdir(exist_ok=True)

    version_name = neo_json["id"]
    subst = {
        "natives_directory": str(natives), "launcher_name": "radiocraft-launch-test", "launcher_version": "1",
        "classpath": os.pathsep.join(classpath), "classpath_separator": os.pathsep, "library_directory": str(libs),
        "version_name": version_name, "game_directory": str(root), "assets_root": str(root / "assets"),
        "assets_index_name": index["id"], "auth_player_name": "LaunchTest",
        "auth_uuid": "00000000000000000000000000000001", "auth_access_token": "0", "clientid": "0",
        "auth_xuid": "0", "user_type": "legacy", "version_type": "release",
    }

    def expand(arg_list: list) -> list[str]:
        out = []
        for a in arg_list:
            if isinstance(a, dict):
                if not rules_allow(a.get("rules")):
                    continue
                values = a["value"] if isinstance(a["value"], list) else [a["value"]]
            else:
                values = [a]
            out += [re.sub(r"\$\{(\w+)}", lambda m: subst.get(m.group(1), m.group(0)), v) for v in values]
        return out

    jvm = expand(vanilla["arguments"]["jvm"]) + expand(neo_json["arguments"].get("jvm", []))
    game = expand(neo_json["arguments"].get("game", [])) + expand(vanilla["arguments"]["game"])
    cmd = [java(), "-Xmx3G", *jvm, neo_json["mainClass"], *game, "--width", "854", "--height", "480"]

    run = Run(cmd, root, args.work / "client.log")
    try:
        deadline = time.monotonic() + args.timeout
        # The block atlas is stitched in the resource reload that follows mod loading: the title screen is next.
        run.wait_for(re.compile(r"Created: \d+x\d+x\d+ minecraft:textures/atlas/blocks\.png-atlas"), deadline)
        log("client finished loading; watching it for a few seconds")
        run.drain(8)
        if run.proc.poll() is not None:
            raise RuntimeError(f"the client exited (code {run.proc.returncode}) after loading")
    finally:
        run.stop()
    return run.errors


# ---------------------------------------------------------------- main

def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--jar", required=True, type=Path, help="the shipped mod jar")
    ap.add_argument("--mod", action="append", default=[], type=Path, help="a required mod jar to install with it")
    ap.add_argument("--neoforge", required=True)
    ap.add_argument("--mc", required=True)
    ap.add_argument("--side", choices=("server", "client", "both"), default="both")
    ap.add_argument("--work", type=Path, default=Path("build/prod-launch"))
    ap.add_argument("--timeout", type=float, default=300, help="seconds each side may take to start")
    ap.add_argument("--probe-block", default="radiocraft:dummy_load", help="block the server places from the console")
    args = ap.parse_args()
    args.work = args.work.resolve()
    jar = args.jar.resolve()
    mods = [m.resolve() for m in args.mod]
    for p in [jar, *mods]:
        if not p.is_file():
            sys.exit(f"[launch-test] no such jar: {p}")

    log(f"{jar.name} on NeoForge {args.neoforge} / Minecraft {args.mc}, side {args.side}")
    with zipfile.ZipFile(installer(args.work, args.neoforge)) as z:
        neo_json = json.loads(z.read("version.json"))
    failures = check_jar(jar, game_libraries(args.work, args.mc, neo_json))

    sides = ["server", "client"] if args.side == "both" else [args.side]
    for side in sides:
        try:
            errors = test_server(args, jar, mods) if side == "server" else test_client(args, jar, mods, neo_json)
            failures += [f"{side}: unexpected ERROR line: {e}" for e in errors]
            if not errors:
                log(f"{side}: OK")
        except Exception as e:  # noqa: BLE001 - every failure is reported the same way
            failures.append(f"{side}: {e}")
        log(f"{side} log: {args.work / (side + '.log')}")

    if failures:
        print("\n[launch-test] FAILED:\n  " + "\n  ".join(failures), file=sys.stderr)
        return 1
    log("PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
