#!/usr/bin/env bash
# Controls a headless RadioCraft dev server that can be driven over RCON (tools/rcon.py).
#
#   tools/devserver.sh start    build + launch in the background, wait until RCON answers
#   tools/devserver.sh stop     graceful "stop" over RCON, wait for exit
#   tools/devserver.sh restart  stop, then start (picks up code changes)
#   tools/devserver.sh status   "list" over RCON
#   tools/devserver.sh log [N]  last N (default 40) non-DEBUG lines of the server log
#   tools/devserver.sh wait     block until RCON answers (for a server started some other way)
#
# The world is a superflat creative world in runs/server/devworld (runs/ is gitignored). Delete that folder for a
# fresh world. A dev client (./gradlew runClient) can join at localhost because online-mode is off.
# Build inside x/z -48..47: those chunks are force loaded so they keep ticking with nobody online.
# DEVSERVER_WORLD, DEVSERVER_LEVEL_TYPE and DEVSERVER_STRUCTURES pick another world, e.g. a normal one with structures
# to find the radio sites in: DEVSERVER_WORLD=sitesworld DEVSERVER_LEVEL_TYPE=minecraft:normal DEVSERVER_STRUCTURES=true
set -euo pipefail
cd "$(dirname "$0")/.."

SERVER_DIR=runs/server
LOG="$SERVER_DIR/devserver.log"
PIDFILE="$SERVER_DIR/devserver.pid"
RCON=(python tools/rcon.py)
export RCON_PORT="${RCON_PORT:-25575}" RCON_PASSWORD="${RCON_PASSWORD:-radiocraft-dev}"

set_prop() { # set_prop key value  -- add or replace a line in server.properties
  local file="$SERVER_DIR/server.properties"
  touch "$file"
  if grep -q "^$1=" "$file"; then sed -i "s#^$1=.*#$1=$2#" "$file"; else echo "$1=$2" >> "$file"; fi
}

configure() {
  mkdir -p "$SERVER_DIR"
  echo "eula=true" > "$SERVER_DIR/eula.txt"
  set_prop enable-rcon true
  set_prop rcon.port "$RCON_PORT"
  set_prop rcon.password "$RCON_PASSWORD"
  set_prop broadcast-rcon-to-ops false
  set_prop online-mode false
  set_prop level-name "${DEVSERVER_WORLD:-devworld}"
  set_prop level-type "${DEVSERVER_LEVEL_TYPE:-minecraft\:flat}"
  set_prop gamemode creative
  set_prop difficulty peaceful
  set_prop spawn-protection 0
  set_prop generate-structures "${DEVSERVER_STRUCTURES:-false}"
  set_prop spawn-monsters false
  set_prop motd 'RadioCraft dev server'
}

rcon_up() { "${RCON[@]}" list >/dev/null 2>&1; }

wait_ready() {
  local timeout=${1:-300}
  for ((i = 0; i < timeout; i += 2)); do
    rcon_up && { echo "Dev server ready (RCON :$RCON_PORT)."; return 0; }
    if [[ -f "$PIDFILE" ]] && ! kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
      echo "Dev server exited during startup. Last log lines:"; log 30; return 1
    fi
    if grep -qE "BUILD FAILED|Crash report|Failed to start the minecraft server" "$LOG" 2>/dev/null; then
      echo "Dev server failed to start. Last log lines:"; log 30; return 1
    fi
    sleep 2
  done
  echo "Timed out after ${timeout}s waiting for RCON."; return 1
}

log() {
  grep -vE "/DEBUG\]|^\s+at |^\s*$" "$LOG" 2>/dev/null | tail -n "${1:-40}"
}

start() {
  if rcon_up; then echo "Dev server already running."; return 0; fi
  configure
  : > "$LOG"
  # DEVSERVER_GRADLE_ARGS adds Gradle flags, e.g. DEVSERVER_GRADLE_ARGS=-PwithSable (or -PwithCreate).
  # shellcheck disable=SC2086
  nohup ./gradlew runServer --console=plain ${DEVSERVER_GRADLE_ARGS:-} > "$LOG" 2>&1 &
  echo $! > "$PIDFILE"
  echo "Starting dev server (pid $(cat "$PIDFILE")), log: $LOG"
  wait_ready 600 || return 1
  # A dedicated server stops ticking block entities 15 s after the last player leaves unless a chunk is force
  # loaded. Keep the 5x5 chunks around the origin (x/z -48..47) ticking so things built there over RCON work.
  "${RCON[@]}" "forceload add -2 -2 2 2" >/dev/null 2>&1 || echo "warning: could not force-load the build area"
}

stop() {
  if ! rcon_up; then echo "Dev server not running."; rm -f "$PIDFILE"; return 0; fi
  "${RCON[@]}" stop >/dev/null 2>&1 || true
  for _ in $(seq 1 60); do rcon_up || break; sleep 1; done
  if [[ -f "$PIDFILE" ]]; then
    for _ in $(seq 1 30); do kill -0 "$(cat "$PIDFILE")" 2>/dev/null || break; sleep 1; done
    rm -f "$PIDFILE"
  fi
  echo "Dev server stopped."
}

case "${1:-}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) "${RCON[@]}" list || exit 1 ;;
  log) log "${2:-40}" ;;
  wait) configure >/dev/null; wait_ready "${2:-600}" ;;
  *) sed -n '2,12p' "$0"; exit 2 ;;
esac
