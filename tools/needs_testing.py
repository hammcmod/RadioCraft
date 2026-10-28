#!/usr/bin/env python3
"""A local web page for doc/NeedsTesting.md: tick items off, mark them as needing work, leave comments, attach screenshots.

    python tools/needs_testing.py            # serves http://127.0.0.1:8765 and opens it
    python tools/needs_testing.py --port 9000 --no-browser

Everything is written straight back into the markdown, so the document stays the single source of truth (and readable
on GitHub, or by whoever fixes the items):

    - [ ] not tested yet          - [x] passed          - [!] needs work
      - Note (2026-10-27): a comment, one per line
      - ![caption](testing_shots/2026-10-27-123456.png)   a screenshot, saved under doc/testing_shots/

Only the standard library is used. One user at a time: the page reloads the document after every change. It only
listens on 127.0.0.1, and writes need a token minted for this run (so a page on some other site cannot post to it) and
a matching Host header.
"""
import argparse
import base64
import json
import re
import secrets
import sys
import threading
import webbrowser
from datetime import date, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent
DOC = ROOT / "doc" / "NeedsTesting.md"
SHOTS = ROOT / "doc" / "testing_shots"

ITEM = re.compile(r"^- \[([ x!])\] (.*)$")
SUB = re.compile(r"^  - (.*)$")
NOTE = re.compile(r"^Note \((\d{4}-\d{2}-\d{2})\): (.*)$")
SHOT = re.compile(r"^!\[(.*?)\]\((.*?)\)$")
STATES = {" ": "open", "x": "pass", "!": "work"}
MARKS = {v: k for k, v in STATES.items()}


def read_lines():
    return DOC.read_text(encoding="utf-8").split("\n")


def write_lines(lines):
    DOC.write_text("\n".join(lines), encoding="utf-8", newline="\n")


def parse(lines):
    """Sections (## headings) with their items; each item knows the line it starts on and its sub-bullets."""
    sections = []
    section = None
    item = None
    for i, line in enumerate(lines):
        if line.startswith("## "):
            section = {"title": line[3:].strip(), "line": i, "intro": [], "items": []}
            sections.append(section)
            item = None
            continue
        if section is None:
            continue
        m = ITEM.match(line)
        if m:
            item = {"line": i, "state": STATES[m.group(1)], "text": m.group(2), "comments": [], "shots": [], "other": []}
            section["items"].append(item)
            continue
        if item is not None:
            s = SUB.match(line)
            if s:
                body = s.group(1)
                n = NOTE.match(body)
                sh = SHOT.match(body)
                if n:
                    item["comments"].append({"line": i, "date": n.group(1), "text": n.group(2)})
                elif sh:
                    item["shots"].append({"line": i, "caption": sh.group(1), "path": sh.group(2)})
                else:
                    item["other"].append({"line": i, "text": body})
                continue
            if line.startswith("  ") and line.strip():
                item["text"] += " " + line.strip()  # A wrapped item.
                continue
            if line.strip() == "":
                continue
            item = None  # A paragraph after the list: section prose.
        if item is None and line.strip():
            section["intro"].append(line)
    return sections


def item_end(lines, start):
    """The index just past an item's block: its wrapped lines and sub-bullets (blank lines inside are kept with it)."""
    i = start + 1
    last = start
    while i < len(lines):
        line = lines[i]
        if line.strip() == "":
            i += 1
            continue
        if line.startswith("  "):
            last = i
            i += 1
            continue
        break
    return last + 1


def set_state(lines, line, state):
    m = ITEM.match(lines[line])
    if not m or state not in MARKS:
        raise ValueError("not an item line")
    lines[line] = f"- [{MARKS[state]}] {m.group(2)}"


def add_sub(lines, line, body):
    if not ITEM.match(lines[line]):
        raise ValueError("not an item line")
    lines.insert(item_end(lines, line), f"  - {body}")


def remove_sub(lines, line):
    if not SUB.match(lines[line]):
        raise ValueError("not a sub-bullet")
    del lines[line]


def save_shot(data_url, name_hint):
    SHOTS.mkdir(parents=True, exist_ok=True)
    header, _, payload = data_url.partition(",")
    ext = "png"
    if "image/jpeg" in header:
        ext = "jpg"
    elif "image/gif" in header:
        ext = "gif"
    elif "image/webp" in header:
        ext = "webp"
    stem = re.sub(r"[^a-z0-9]+", "-", (name_hint or "").lower()).strip("-")[:40]
    stamp = datetime.now().strftime("%Y-%m-%d-%H%M%S")
    name = f"{stamp}{'-' + stem if stem else ''}.{ext}"
    (SHOTS / name).write_bytes(base64.b64decode(payload))
    return f"testing_shots/{name}"


PAGE = r"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="nt-token" content="%%TOKEN%%">
<title>Needs Testing</title>
<style>
:root { --bg: #f6f5f1; --card: #fff; --ink: #1f1f1f; --mute: #6b6b6b; --line: #ddd; --accent: #c8781a; --pass: #2f8f4e; --work: #c23b3b; --open: #8a8a8a; }
@media (prefers-color-scheme: dark) { :root { --bg: #1b1b1b; --card: #262626; --ink: #ececec; --mute: #a0a0a0; --line: #3a3a3a; --accent: #e0a24f; --pass: #5ec27a; --work: #ef6b6b; --open: #9a9a9a; } }
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--ink); font: 15px/1.45 system-ui, Segoe UI, sans-serif; }
header { position: sticky; top: 0; background: var(--bg); border-bottom: 1px solid var(--line); padding: 10px 16px; display: flex; gap: 16px; align-items: center; flex-wrap: wrap; z-index: 2; }
header h1 { font-size: 18px; margin: 0 auto 0 0; }
header label { color: var(--mute); display: flex; gap: 6px; align-items: center; }
main { max-width: 1000px; margin: 0 auto; padding: 12px 16px 60px; }
section { margin: 18px 0; }
section > h2 { font-size: 17px; margin: 0 0 6px; display: flex; align-items: baseline; gap: 10px; cursor: pointer; user-select: none; }
section > h2 .count { color: var(--mute); font-size: 13px; font-weight: normal; }
section .intro { color: var(--mute); font-size: 14px; margin: 0 0 8px; white-space: pre-wrap; }
.item { background: var(--card); border: 1px solid var(--line); border-radius: 8px; padding: 10px 12px; margin: 8px 0; display: grid; grid-template-columns: auto 1fr; gap: 8px 12px; }
.item.active { outline: 2px solid var(--accent); }
.item.pass .text { color: var(--mute); }
.state { display: flex; flex-direction: column; gap: 4px; }
.state button { border: 1px solid var(--line); background: transparent; color: var(--mute); border-radius: 6px; width: 34px; height: 30px; cursor: pointer; font-size: 15px; }
.state button.on.pass { background: var(--pass); color: #fff; border-color: var(--pass); }
.state button.on.work { background: var(--work); color: #fff; border-color: var(--work); }
.state button.on.open { background: var(--open); color: #fff; border-color: var(--open); }
.text code { background: rgba(127,127,127,.18); padding: 1px 4px; border-radius: 4px; font-size: 13px; }
.subs { grid-column: 2; display: flex; flex-direction: column; gap: 6px; }
.note { display: flex; gap: 8px; align-items: baseline; font-size: 14px; }
.note .date { color: var(--mute); font-size: 12px; white-space: nowrap; }
.note .x, .shot .x { color: var(--mute); background: none; border: none; cursor: pointer; font-size: 13px; }
.shots { display: flex; gap: 8px; flex-wrap: wrap; }
.shot { position: relative; }
.shot img { height: 110px; border-radius: 6px; border: 1px solid var(--line); display: block; cursor: zoom-in; }
.shot .x { position: absolute; top: 2px; right: 2px; background: rgba(0,0,0,.55); color: #fff; border-radius: 4px; }
.shot .cap { font-size: 12px; color: var(--mute); max-width: 180px; }
.tools { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.tools input[type=text] { flex: 1; min-width: 200px; border: 1px solid var(--line); background: transparent; color: var(--ink); border-radius: 6px; padding: 5px 8px; font: inherit; }
.tools button { border: 1px solid var(--line); background: transparent; color: var(--ink); border-radius: 6px; padding: 5px 10px; cursor: pointer; font: inherit; }
.tools button:hover { border-color: var(--accent); }
.hint { color: var(--mute); font-size: 12px; }
#status { color: var(--mute); font-size: 13px; }
#zoom { position: fixed; inset: 0; background: rgba(0,0,0,.85); display: none; align-items: center; justify-content: center; z-index: 5; cursor: zoom-out; }
#zoom img { max-width: 96vw; max-height: 96vh; }
.hidden { display: none !important; }
</style>
</head>
<body>
<header>
  <h1>Needs Testing</h1>
  <label><input type="checkbox" id="openOnly"> only open and needs-work</label>
  <span id="status"></span>
</header>
<main id="main"></main>
<div id="zoom"><img alt=""></div>
<script>
let doc = [];
let active = null;           // Line of the item a pasted screenshot goes to.
const main = document.getElementById('main');
const status = document.getElementById('status');
const openOnly = document.getElementById('openOnly');
openOnly.checked = localStorage.getItem('nt.openOnly') === '1';
openOnly.onchange = () => { localStorage.setItem('nt.openOnly', openOnly.checked ? '1' : '0'); render(); };

function esc(s) { return s.replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
function md(s) {
  return esc(s).replace(/`([^`]+)`/g, '<code>$1</code>').replace(/\*\*([^*]+)\*\*/g, '<b>$1</b>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, (m, text, url) => /^(https?:|\/|#|\.\.?\/)/i.test(url) ? `<a href="${url}">${text}</a>` : `${text} (${url})`);
}
const TOKEN = document.querySelector('meta[name="nt-token"]').content;
async function api(path, body) {
  const r = await fetch(path, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-NT-Token': TOKEN }, body: JSON.stringify(body) });
  if (!r.ok) { status.textContent = 'Error: ' + await r.text(); throw new Error(); }
  return r.json();
}
async function load() {
  doc = await (await fetch('/api/doc')).json();
  render();
}
function counts(items) {
  const c = { open: 0, pass: 0, work: 0 };
  items.forEach(i => c[i.state]++);
  return c;
}
function render() {
  const collapsed = JSON.parse(localStorage.getItem('nt.collapsed') || '{}');
  main.innerHTML = '';
  let total = { open: 0, pass: 0, work: 0 };
  for (const s of doc) {
    const c = counts(s.items);
    for (const k in c) total[k] += c[k];
    const sec = document.createElement('section');
    const h = document.createElement('h2');
    h.innerHTML = `<span>${collapsed[s.title] ? '&#9656;' : '&#9662;'}</span> ${esc(s.title)} <span class="count">${c.pass}/${s.items.length} passed${c.work ? `, ${c.work} need work` : ''}</span>`;
    h.onclick = () => { collapsed[s.title] = !collapsed[s.title]; localStorage.setItem('nt.collapsed', JSON.stringify(collapsed)); render(); };
    sec.appendChild(h);
    if (!collapsed[s.title]) {
      if (s.intro.length) { const p = document.createElement('p'); p.className = 'intro'; p.innerHTML = md(s.intro.join('\n')); sec.appendChild(p); }
      for (const it of s.items) {
        if (openOnly.checked && it.state === 'pass') continue;
        sec.appendChild(renderItem(it));
      }
    }
    main.appendChild(sec);
  }
  status.textContent = `${total.pass} passed, ${total.work} need work, ${total.open} untested`;
}
function renderItem(it) {
  const el = document.createElement('div');
  el.className = 'item ' + it.state + (active === it.line ? ' active' : '');
  el.onclick = () => { if (active !== it.line) { active = it.line; render(); } };
  const st = document.createElement('div'); st.className = 'state';
  for (const [state, glyph, title] of [['pass', '✓', 'Passed'], ['work', '!', 'Needs work'], ['open', '·', 'Not tested yet']]) {
    const b = document.createElement('button'); b.textContent = glyph; b.title = title;
    b.className = state + (it.state === state ? ' on' : '');
    b.onclick = async e => { e.stopPropagation(); await api('/api/state', { line: it.line, state }); load(); };
    st.appendChild(b);
  }
  el.appendChild(st);
  const t = document.createElement('div'); t.className = 'text'; t.innerHTML = md(it.text); el.appendChild(t);
  const subs = document.createElement('div'); subs.className = 'subs';
  for (const o of it.other) { const d = document.createElement('div'); d.className = 'note'; d.innerHTML = md(o.text); subs.appendChild(d); }
  for (const n of it.comments) {
    const d = document.createElement('div'); d.className = 'note';
    d.innerHTML = `<span class="date">${esc(n.date)}</span><span>${md(n.text)}</span>`;
    const x = document.createElement('button'); x.className = 'x'; x.textContent = 'remove'; x.title = 'Remove this comment';
    x.onclick = async e => { e.stopPropagation(); if (confirm('Remove this comment?')) { await api('/api/remove', { line: n.line }); load(); } };
    d.appendChild(x); subs.appendChild(d);
  }
  if (it.shots.length) {
    const row = document.createElement('div'); row.className = 'shots';
    for (const sh of it.shots) {
      const d = document.createElement('div'); d.className = 'shot';
      const img = document.createElement('img'); img.src = '/doc/' + sh.path; img.alt = sh.caption; img.title = sh.caption;
      img.onclick = e => { e.stopPropagation(); zoom(img.src); };
      d.appendChild(img);
      if (sh.caption) { const cap = document.createElement('div'); cap.className = 'cap'; cap.textContent = sh.caption; d.appendChild(cap); }
      const x = document.createElement('button'); x.className = 'x'; x.textContent = '×'; x.title = 'Remove from the list (the file stays)';
      x.onclick = async e => { e.stopPropagation(); if (confirm('Remove this screenshot from the item?')) { await api('/api/remove', { line: sh.line }); load(); } };
      d.appendChild(x); row.appendChild(d);
    }
    subs.appendChild(row);
  }
  const tools = document.createElement('div'); tools.className = 'tools';
  const input = document.createElement('input'); input.type = 'text'; input.placeholder = 'Comment (Enter to add)';
  input.onfocus = () => { if (active !== it.line) { active = it.line; render(); } };
  input.onkeydown = async e => { if (e.key === 'Enter' && input.value.trim()) { await api('/api/comment', { line: it.line, text: input.value.trim() }); load(); } };
  tools.appendChild(input);
  const file = document.createElement('input'); file.type = 'file'; file.accept = 'image/*'; file.multiple = true; file.className = 'hidden';
  file.onchange = async () => { for (const f of file.files) await attach(it.line, f, f.name.replace(/\.[^.]+$/, '')); load(); };
  const pick = document.createElement('button'); pick.textContent = 'Screenshot…'; pick.title = 'Attach an image file (or click the item and paste one)';
  pick.onclick = e => { e.stopPropagation(); file.click(); };
  tools.appendChild(pick); tools.appendChild(file);
  if (active === it.line) { const h = document.createElement('span'); h.className = 'hint'; h.textContent = 'Ctrl+V pastes a screenshot here'; tools.appendChild(h); }
  subs.appendChild(tools);
  el.appendChild(subs);
  return el;
}
function attach(line, blob, hint) {
  return new Promise(resolve => {
    const r = new FileReader();
    r.onload = async () => {
      const caption = prompt('Caption for the screenshot (optional):', hint || '') ;
      if (caption === null) return resolve();
      await api('/api/shot', { line, data: r.result, caption, hint: caption || hint });
      resolve();
    };
    r.readAsDataURL(blob);
  });
}
document.addEventListener('paste', async e => {
  if (active === null) return;
  for (const item of e.clipboardData.items) {
    if (item.type.startsWith('image/')) { e.preventDefault(); await attach(active, item.getAsFile(), ''); load(); return; }
  }
});
const zoomEl = document.getElementById('zoom');
function zoom(src) { zoomEl.querySelector('img').src = src; zoomEl.style.display = 'flex'; }
zoomEl.onclick = () => { zoomEl.style.display = 'none'; };
load();
</script>
</body>
</html>
"""


TOKEN = secrets.token_urlsafe(24)
HOST = "127.0.0.1"


class Handler(BaseHTTPRequestHandler):

    def log_message(self, fmt, *args):  # Quiet.
        pass

    def local(self):
        """True if the request came from our own page: the right Host, and no Origin from elsewhere."""
        host = self.headers.get("Host", "")
        origin = self.headers.get("Origin")
        return host == HOST and (origin is None or origin == f"http://{HOST}")

    def send(self, code, body, ctype="application/json"):
        data = body if isinstance(body, bytes) else body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        path = urlparse(self.path).path
        if not self.local():
            return self.send(403, "forbidden", "text/plain")
        if path == "/":
            return self.send(200, PAGE.replace("%%TOKEN%%", TOKEN), "text/html; charset=utf-8")
        if path == "/api/doc":
            return self.send(200, json.dumps(parse(read_lines())))
        if path.startswith("/doc/"):
            target = (ROOT / "doc" / unquote(path[5:])).resolve()
            if target.is_file() and (ROOT / "doc").resolve() in target.parents:
                ctype = {"png": "image/png", "jpg": "image/jpeg", "gif": "image/gif", "webp": "image/webp"}.get(target.suffix[1:].lower(), "application/octet-stream")
                return self.send(200, target.read_bytes(), ctype)
        self.send(404, "not found", "text/plain")

    def do_POST(self):
        path = urlparse(self.path).path
        if not self.local() or self.headers.get("X-NT-Token") != TOKEN or not self.headers.get("Content-Type", "").startswith("application/json"):
            return self.send(403, "forbidden", "text/plain")
        length = int(self.headers.get("Content-Length", "0"))
        body = json.loads(self.rfile.read(length) or b"{}")
        try:
            with LOCK:
                lines = read_lines()
                line = int(body.get("line", -1))
                if path == "/api/state":
                    set_state(lines, line, body["state"])
                elif path == "/api/comment":
                    text = " ".join(str(body["text"]).split())
                    add_sub(lines, line, f"Note ({date.today().isoformat()}): {text}")
                elif path == "/api/shot":
                    rel = save_shot(body["data"], body.get("hint", ""))
                    caption = " ".join(str(body.get("caption", "")).split()).replace("]", ")")
                    add_sub(lines, line, f"![{caption}]({rel})")
                elif path == "/api/remove":
                    remove_sub(lines, line)
                else:
                    return self.send(404, "not found", "text/plain")
                write_lines(lines)
            self.send(200, json.dumps({"ok": True}))
        except (KeyError, ValueError, IndexError) as e:
            self.send(400, f"{e}", "text/plain")


LOCK = threading.Lock()


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--no-browser", action="store_true")
    args = ap.parse_args()
    if not DOC.is_file():
        sys.exit(f"{DOC} not found")
    global HOST
    HOST = f"127.0.0.1:{args.port}"
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    url = f"http://{HOST}/"
    print(f"Needs Testing: {url}  (editing {DOC}; Ctrl+C to stop)")
    if not args.no_browser:
        threading.Timer(0.5, webbrowser.open, [url]).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
