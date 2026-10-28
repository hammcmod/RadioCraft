"""Splits the maintainer's WEFAX receiver model into the two the blockstate uses.

`models/block/wefax_receiver.json` (Blockbench, 2026-10-27) carries two sheets in the tray: the element named
"No message Ready" (the slot's empty lip) and "Message Ready" (a printed sheet sticking out of the front). Only one
shows at a time, by the block's READY state, so this writes:

    models/block/wefax_receiver_idle.json   the model without "Message Ready"
    models/block/wefax_receiver_ready.json  the model without "No message Ready"

Rerun it after any re-export of wefax_receiver.json; the datagen blockstate provider names these two files.
"""
import json
import os

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "radiocraft", "models", "block") + os.sep
SOURCE = BASE + "wefax_receiver.json"
HIDE = {"idle": "Message Ready", "ready": "No message Ready"}


def without(model, name):
    out = json.loads(json.dumps(model))
    out["elements"] = [e for e in out["elements"] if e.get("name", "").strip().lower() != name.lower()]
    out["credit"] = model.get("credit", "") + " (tools/wefax_models.py: without the '" + name + "' element)"
    return out


model = json.load(open(SOURCE, encoding="utf-8"))
names = [e.get("name") for e in model["elements"]]
for suffix, name in HIDE.items():
    assert any((n or "").strip().lower() == name.lower() for n in names), f"no element named {name!r} in {SOURCE}: {names}"
    with open(f"{BASE}wefax_receiver_{suffix}.json", "w", encoding="utf-8", newline="\n") as f:
        json.dump(without(model, name), f, indent="\t")
print("wrote wefax_receiver_idle.json and wefax_receiver_ready.json")
