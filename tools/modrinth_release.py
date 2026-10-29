#!/usr/bin/env python3
"""Publish a built mod jar as a new version on Modrinth and, optionally, as a GitHub release.

Driven by tools/push-versions.ps1; see there for the usual way to run it. Standard library only.

    python tools/modrinth_release.py --project radiocraft --version 1.9.1 --jar build/libs/x.jar \
        --changelog doc/ReleaseNotes-1.9.md [--type alpha|beta|release] [--featured] [--github] [--dry-run]

Tokens (never put them in the repository): MODRINTH_TOKEN and GITHUB_TOKEN in the environment, or one per line in
~/.radiocraft/modrinth_token and ~/.radiocraft/github_token. The GitHub token needs "contents: write" on the repo
(a fine-grained token) or the "repo" scope (classic).

The changelog file may hold several releases, each under its own "# " heading; the section whose heading contains
the version number is used (the whole file if there is only one section).
"""
import argparse
import json
import mimetypes
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

USER_AGENT = "hammcmod/RadioCraft release tool (github.com/hammcmod/RadioCraft)"
MODRINTH = "https://api.modrinth.com/v2"
GITHUB = "https://api.github.com"


def read_token(env_name: str, file_name: str) -> str | None:
    value = os.environ.get(env_name)
    if value:
        return value.strip()
    path = Path.home() / ".radiocraft" / file_name
    if path.is_file():
        return path.read_text(encoding="utf-8").strip() or None
    return None


def changelog_section(text: str, version: str) -> str:
    """The "# " section whose heading names the version, or the whole text if it has one section or none match."""
    parts = re.split(r"(?m)^(?=# )", text)
    sections = [p for p in parts if p.strip()]
    if len(sections) <= 1:
        return text.strip()
    for section in sections:
        heading = section.splitlines()[0]
        if version in heading:
            return section.strip()
    return text.strip()


def multipart(fields: dict[str, str], files: list[tuple[str, Path]]) -> tuple[bytes, str]:
    boundary = "----RadioCraft" + uuid.uuid4().hex
    out = bytearray()
    for name, value in fields.items():
        out += f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n".encode()
        out += value.encode("utf-8") + b"\r\n"
    for name, path in files:
        ctype = mimetypes.guess_type(path.name)[0] or "application/java-archive"
        out += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"; filename=\"{path.name}\"\r\n"
                f"Content-Type: {ctype}\r\n\r\n").encode()
        out += path.read_bytes() + b"\r\n"
    out += f"--{boundary}--\r\n".encode()
    return bytes(out), f"multipart/form-data; boundary={boundary}"


def request(method: str, url: str, headers: dict[str, str], body: bytes | None = None) -> dict:
    req = urllib.request.Request(url, data=body, method=method, headers={"User-Agent": USER_AGENT, **headers})
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        sys.exit(f"{method} {url} failed: HTTP {e.code}\n{detail}")


def modrinth_version_exists(project_id: str, version_number: str, token: str) -> bool:
    versions = request("GET", f"{MODRINTH}/project/{project_id}/version", {"Authorization": token})
    return any(v["version_number"] == version_number for v in versions)


def push_modrinth(cfg: dict, args, changelog: str, jar: Path, token: str) -> str:
    project_id = cfg["project_id"]
    if modrinth_version_exists(project_id, args.version, token):
        sys.exit(f"Modrinth already has version {args.version} of {project_id}; delete it there first or pick another number.")
    data = {
        "name": args.name or f"{args.display_name} {args.version}",
        "version_number": args.version,
        "changelog": changelog,
        "dependencies": [{k: v for k, v in d.items() if not k.startswith("_")} for d in cfg.get("dependencies", [])],
        "game_versions": args.game_versions or cfg["game_versions"],
        "version_type": args.type,
        "loaders": cfg["loaders"],
        "featured": bool(args.featured),
        "project_id": project_id,
        "file_parts": ["file"],
        "primary_file": "file",
    }
    if args.dry_run:
        print("Modrinth payload:\n" + json.dumps(data, indent=2))
        return "(dry run)"
    body, ctype = multipart({"data": json.dumps(data)}, [("file", jar)])
    result = request("POST", f"{MODRINTH}/version", {"Authorization": token, "Content-Type": ctype}, body)
    return f"https://modrinth.com/mod/{project_id}/version/{result['id']}"


def push_github(repo: str, args, changelog: str, jar: Path, token: str) -> str:
    headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
               "X-GitHub-Api-Version": "2022-11-28"}
    tag = args.tag or args.version
    payload = {"tag_name": tag, "name": args.name or f"{args.display_name} {args.version}", "body": changelog,
               "prerelease": args.type != "release", "draft": False}
    if args.dry_run:
        print(f"GitHub release on {repo}:\n" + json.dumps(payload, indent=2))
        return "(dry run)"
    release = request("POST", f"{GITHUB}/repos/{repo}/releases", {**headers, "Content-Type": "application/json"},
                      json.dumps(payload).encode())
    upload = release["upload_url"].split("{")[0] + f"?name={jar.name}"
    request("POST", upload, {**headers, "Content-Type": "application/java-archive"}, jar.read_bytes())
    return release["html_url"]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--project", required=True, help="key in tools/modrinth_projects.json")
    ap.add_argument("--version", required=True, help="version number, e.g. 1.9.1 (also the git tag unless --tag)")
    ap.add_argument("--jar", required=True, type=Path)
    ap.add_argument("--changelog", required=True, type=Path, help="markdown file; the version's section is used")
    ap.add_argument("--type", default="alpha", choices=["alpha", "beta", "release"])
    ap.add_argument("--name", help="display name of the version (default '<Project> <version>')")
    ap.add_argument("--display-name", default=None, help="project name used in the default version name")
    ap.add_argument("--game-versions", nargs="*", help="override the game versions in the projects file")
    ap.add_argument("--featured", action="store_true")
    ap.add_argument("--tag", help="existing git tag for the GitHub release (default: the version)")
    ap.add_argument("--github", action="store_true", help="also create a GitHub release with the jar attached")
    ap.add_argument("--no-modrinth", action="store_true", help="skip Modrinth (GitHub only)")
    ap.add_argument("--dry-run", action="store_true", help="print what would be sent and send nothing")
    args = ap.parse_args()

    projects = json.loads((Path(__file__).parent / "modrinth_projects.json").read_text(encoding="utf-8"))
    if args.project not in projects:
        sys.exit(f"Unknown project {args.project!r}; known: {', '.join(k for k in projects if not k.startswith('_'))}")
    project = projects[args.project]
    args.display_name = args.display_name or {"radiocraft": "Radiocraft", "anotherweathermod": "Another Weather Mod"}.get(
        args.project, args.project)

    if not args.jar.is_file():
        sys.exit(f"No jar at {args.jar}")
    if not args.changelog.is_file():
        sys.exit(f"No changelog at {args.changelog}")
    changelog = changelog_section(args.changelog.read_text(encoding="utf-8"), args.version)
    print(f"{args.display_name} {args.version} ({args.type}): {args.jar.name}, {len(changelog)} chars of changelog")

    modrinth_cfg = project.get("modrinth")
    if modrinth_cfg and not args.no_modrinth:
        token = read_token("MODRINTH_TOKEN", "modrinth_token")
        if not token and not args.dry_run:
            sys.exit("No Modrinth token: set MODRINTH_TOKEN or write it to ~/.radiocraft/modrinth_token")
        print("Modrinth: " + push_modrinth(modrinth_cfg, args, changelog, args.jar, token or ""))
    elif not args.no_modrinth:
        print("Modrinth: this project has no Modrinth entry in modrinth_projects.json, skipped")

    if args.github:
        repo = project.get("github")
        if not repo:
            sys.exit("No 'github' repo in modrinth_projects.json for this project")
        token = read_token("GITHUB_TOKEN", "github_token")
        if not token and not args.dry_run:
            sys.exit("No GitHub token: set GITHUB_TOKEN or write it to ~/.radiocraft/github_token")
        print("GitHub: " + push_github(repo, args, changelog, args.jar, token or ""))


if __name__ == "__main__":
    main()
