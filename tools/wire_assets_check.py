"""Checks the generated wire blockstate against the rendering rule in WireBlock#buildShape for every state.

    python tools/wire_assets_check.py

For each of the 6400 (flags x hug x arrive x end) states it evaluates the multipart conditions of
blockstates/wire.json and compares the set of applied models with what the rule says should be drawn.
"""
import itertools
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
BS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "radiocraft", "blockstates", "wire.json")
SIDES = ["north", "east", "south", "west"]
ROT = {"north": None, "east": 90, "south": 180, "west": 270}


def matches(cond, state):
    if "OR" in cond:
        return any(matches(c, state) for c in cond["OR"])
    return all(str(state[k]).lower() in v.split("|") for k, v in cond.items())


def applied(cases, state):
    out = []
    for case in cases:
        if "when" not in case or matches(case["when"], state):
            m = case["apply"]
            out.append((m["model"].split("/")[-1], m.get("y")))
    return sorted(out, key=str)


def expected(state):
    hug, arrive, end = state["hug"], state["arrive"], state["end"]
    edge_cable = {e for e in (hug, arrive) if e != "none"}
    flags = {d for d in SIDES if state[d]}
    port_off_edge = (state["up"] and hug == "none") or (state["down"] and arrive == "none")
    port_off_edge = port_off_edge or any(d not in edge_cable for d in flags)
    any_port = bool(flags) or (state["up"] and hug == "none") or (state["down"] and arrive == "none")
    middle = port_off_edge or len(edge_cable) != 1 or not any_port
    out = []
    if middle:
        out.append(("wire_middle", None))
    for d in SIDES:
        if middle and (d in flags or d in edge_cable):
            out.append(("wire_horizontal", ROT[d]))
        elif d in flags and arrive == d and hug == "none":
            out.append(("wire_corner", ROT[d]))
    if hug != "none":
        out.append(("wire_edge_full" if arrive == hug else "wire_edge", ROT[hug]))
        if end == "climb_device":
            out.append(("wire_climb_overhang", ROT[hug]))
        if end == "up_device":
            out.append(("wire_up_overhang", ROT[hug]))
    elif state["up"]:
        out.append(("wire_vertical", None))
    return sorted(out, key=str)


def main():
    cases = json.load(open(BS))["multipart"]
    bad = 0
    for bits in itertools.product([False, True], repeat=6):
        for hug in ["none"] + SIDES:
            for arrive in ["none"] + SIDES:
                for end in ["none", "climb_wire", "climb_device", "up_device"]:
                    state = dict(zip(SIDES + ["up", "down"], bits), hug=hug, arrive=arrive, end=end)
                    got, want = applied(cases, state), expected(state)
                    if got != want:
                        bad += 1
                        if bad <= 10:
                            print("MISMATCH", {k: v for k, v in state.items() if v and v != "none"}, "\n   got ", got, "\n   want", want)
    print("states checked: 6400, mismatches:", bad)
    return bad


if __name__ == "__main__":
    raise SystemExit(1 if main() else 0)
