"""Validates every content file against docs/CONTENT.md.

python -I tools/check_content.py
"""
import re
import sys
from pathlib import Path

C = Path(__file__).resolve().parent.parent / "app/src/main/assets/content"
KINDS = set("monstera philo ficus tree grass rosette aloe paddle column ball fern pattern blade zz palm cane orchid spathe violet bush coin trail herb bulb carnivore".split())
ELEMENTS = {"desert", "mist", "rain", "meadow", "forest"}
CUTTING = {"water", "soil", "leaf", "division", "offset", "seed", "none"}
SPRITES = set("mite scale thrips mealy gnat aphid whitefly rot burn mildew dark springtail".split())
MOODS = set("calm sun gloom dry soggy night rest pest heat scorch new evolve hatch".split())
errors = []


def rows(name):
    for n, line in enumerate((C / name).read_text(encoding="utf-8").split("\n"), 1):
        if line.strip() and not line.startswith("#"):
            yield n, line.split("\t")


def err(f, n, msg):
    errors.append(f"{f}:{n}: {msg}")


ids = set()
for f in sorted(p.name for p in C.glob("species*.tsv")):
    for n, r in rows(f):
        if len(r) != 25:
            err(f, n, f"{len(r)} fields"); continue
        if not re.fullmatch(r"[a-z0-9_]+", r[0]) or r[0] in ids: err(f, n, f"bad or repeated id {r[0]}")
        ids.add(r[0])
        if r[1] not in KINDS: err(f, n, f"kind {r[1]}")
        if r[2] not in ELEMENTS: err(f, n, f"element {r[2]}")
        try:
            lo, hi, dry, at = float(r[6]), float(r[7]), float(r[8]), int(r[9])
            if not lo < hi: err(f, n, "dli_min >= dli_max")
            if not 2 <= dry <= 15: err(f, n, "dry")
            if at not in (10, 25, 40, 55): err(f, n, "water_at")
            if not (0 <= int(r[14]) <= 360 and 10 <= int(r[15]) <= 70 and 28 <= int(r[16]) <= 62): err(f, n, "colour")
            if not int(r[17]) < int(r[18]): err(f, n, "s2 >= s3")
            int(r[20])
        except ValueError as e:
            err(f, n, str(e))
        if r[10] not in "01" or r[11] not in "01" or r[12] not in "01": err(f, n, "flags")
        if r[13] != "-" and not re.fullmatch(r"#[0-9A-Fa-f]{6}", r[13]): err(f, n, "bloom")
        if r[19] not in CUTTING: err(f, n, "cutting")
        for k in (21, 22):
            if len(r[k].split(",")) != 3: err(f, n, "three forms")
        for k in (23, 24):
            if len(r[k]) > 140: err(f, n, "tip too long")

codes = {r[0] for _, r in rows("symptoms.tsv")}
pests = set()
for n, r in rows("pests.tsv"):
    if len(r) != 12: err("pests.tsv", n, f"{len(r)} fields"); continue
    pests.add(r[0])
    if r[1] not in SPRITES: err("pests.tsv", n, f"sprite {r[1]}")
    for s in r[7].split(","):
        if s.strip() not in codes: err("pests.tsv", n, f"symptom {s}")

moves = {}
for n, r in rows("moves.tsv"):
    if len(r) != 9: err("moves.tsv", n, f"{len(r)} fields"); continue
    if r[0] not in pests: err("moves.tsv", n, f"pest {r[0]}")
    moves.setdefault(r[0], []).append((int(r[1]), int(r[2])))
for p in pests:
    m = sorted(moves.get(p, []))
    if [x[0] for x in m] != list(range(1, len(m) + 1)) or len(m) < 3: err("moves.tsv", 0, f"{p}: moves {m}")
    if any(m[i][1] > m[i + 1][1] for i in range(len(m) - 1)): err("moves.tsv", 0, f"{p}: days go back")

pairs = {}
for n, r in rows("lines.tsv"):
    if len(r) != 4: err("lines.tsv", n, f"{len(r)} fields"); continue
    if r[0] not in MOODS or (r[1] != "*" and r[1] not in ELEMENTS): err("lines.tsv", n, "mood/element")
    pairs[(r[0], r[1])] = pairs.get((r[0], r[1]), 0) + 1
for m in MOODS - {"new", "evolve", "hatch"}:
    for e in ELEMENTS:
        if pairs.get((m, e), 0) < 6: err("lines.tsv", 0, f"{m}/{e}: {pairs.get((m, e), 0)} lines")

print("\n".join(errors) if errors else f"ok: {len(ids)} species, {len(pests)} pests, {sum(len(v) for v in moves.values())} moves, {sum(pairs.values())} lines")
sys.exit(1 if errors else 0)
