#!/usr/bin/env python3
"""배치(layout) JSON 30여 종과 levels.json(1~200단계 -> 배치 id)을 생성한다.

좌표는 반 칸 단위: 패 1장은 가로 2 x 세로 2칸, 격자 칸 (i, j)의 패는 x=2i, y=2j.
같은 층에서 패끼리 겹치지 않고, 총 개수는 항상 짝수여야 한다.

사용: python tools/gen_layouts.py   (프로젝트 루트에서)
산출: app/src/main/assets/layouts/*.json, app/src/main/assets/levels.json
"""
import json
import os
import shutil
from collections import defaultdict

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")


# ---------------------------------------------------------------- 도형 (격자 칸 단위 (i, j, z))
def pyramid(w, h, layers):
    cells = []
    for k in range(layers):
        if w - 2 * k <= 0 or h - 2 * k <= 0:
            return None
        cells += [(i, j, k) for j in range(k, h - k) for i in range(k, w - k)]
    return cells


def diamond(r, layers):
    cells = []
    for k in range(layers):
        rk = r - k
        if rk < 0:
            return None
        for j in range(-rk, rk + 1):
            span = rk - abs(j)
            cells += [(i, j, k) for i in range(-span, span + 1)]
    return cells


def cross(arm, thick, layers):
    cells = []
    for k in range(layers):
        ak = arm - k
        if ak < thick:
            return None
        for j in range(-ak, ak + 1):
            for i in range(-ak, ak + 1):
                if abs(i) <= thick - k or abs(j) <= thick - k:
                    cells.append((i, j, k))
    return cells


def frame(w, h, layers):
    cells = []
    for k in range(layers):
        for j in range(k, h - k):
            for i in range(k, w - k):
                if i in (k, w - 1 - k) or j in (k, h - 1 - k):
                    cells.append((i, j, k))
    return cells


def oval(a, b, layers):
    cells = []
    for k in range(layers):
        ak, bk = a - k, b - k
        if ak <= 0 or bk <= 0:
            return None
        for j in range(-b, b + 1):
            for i in range(-a, a + 1):
                if (i / ak) ** 2 + (j / bk) ** 2 <= 1.0001:
                    cells.append((i, j, k))
    return cells


def triangle(n, layers):
    """행마다 반 칸씩 밀려 있는 삼각형. 셀 (i, j) -> x = 2i + j."""
    pos = []
    for k in range(layers):
        for j in range(k, n - k):
            for i in range(k, n - j - k):
                pos.append((2 * i + j, 2 * j, k))
    return pos


def turtle():
    pos = []
    rows = {0: (2, 24), 1: (6, 20), 2: (4, 22), 3: (2, 24), 4: (2, 24), 5: (4, 22), 6: (6, 20), 7: (2, 24)}
    for j, (x0, x1) in rows.items():
        pos += [(x, 2 * j, 0) for x in range(x0, x1 + 1, 2)]
    pos += [(0, 7, 0), (26, 7, 0), (28, 7, 0)]
    pos += [(x, y, 1) for y in range(2, 13, 2) for x in range(8, 19, 2)]
    pos += [(x, y, 2) for y in range(4, 11, 2) for x in range(10, 17, 2)]
    pos += [(x, y, 3) for y in (6, 8) for x in (12, 14)]
    pos += [(13, 7, 4)]
    return pos


def to_half_units(cells):
    """격자 칸 -> 반 칸 좌표로 바꾸고 최소 좌표가 0이 되게 평행이동."""
    pos = [(2 * i, 2 * j, k) for i, j, k in cells]
    return pos


def normalize(pos):
    mx = min(p[0] for p in pos)
    my = min(p[1] for p in pos)
    out = sorted({(x - mx, y - my, z) for x, y, z in pos}, key=lambda p: (p[2], p[1], p[0]))
    return out


def make_even(pos):
    """홀수면 맨 위층의 마지막 패 하나를 뺀다."""
    pos = list(pos)
    if len(pos) % 2:
        top = max(p[2] for p in pos)
        last = [p for p in pos if p[2] == top][-1]
        pos.remove(last)
    return pos


def layer_count(pos):
    return max(p[2] for p in pos) + 1


def difficulty(n, layers):
    if n <= 32:
        return 1
    if n <= 60:
        return 2
    if n <= 99:
        return 3
    if n <= 135:
        return 4
    return 5


FAMILY_KO = {"pyr": "피라미드", "dia": "마름모", "crs": "십자", "frm": "테두리", "ovl": "타원", "tri": "삼각형", "turtle": "거북이"}
FAMILY_EN = {"pyr": "Pyramid", "dia": "Diamond", "crs": "Cross", "frm": "Frame", "ovl": "Oval", "tri": "Triangle", "turtle": "Turtle"}


def candidates():
    out = []

    def add(fam, params, pos, half):
        if not pos:
            return
        pos = make_even(normalize(to_half_units(pos) if not half else pos))
        out.append({"family": fam, "params": params, "pos": pos})

    for w in range(4, 15):
        for h in range(3, 12):
            for L in range(1, 6):
                c = pyramid(w, h, L)
                if c:
                    add("pyr", f"{w}x{h}x{L}", c, False)
    for r in range(2, 8):
        for L in range(1, 6):
            c = diamond(r, L)
            if c:
                add("dia", f"{r}x{L}", c, False)
    for arm in range(3, 10):
        for t in range(1, 4):
            for L in range(1, 5):
                c = cross(arm, t, L)
                if c:
                    add("crs", f"{arm}x{t}x{L}", c, False)
    for w in range(5, 14):
        for h in range(4, 11):
            for L in range(1, 5):
                add("frm", f"{w}x{h}x{L}", frame(w, h, L), False)
    for a in range(3, 11):
        for b in range(2, 9):
            for L in range(1, 5):
                c = oval(a, b, L)
                if c:
                    add("ovl", f"{a}x{b}x{L}", c, False)
    for n in range(5, 19):
        for L in range(1, 6):
            add("tri", f"{n}x{L}", triangle(n, L), True)
    out.append({"family": "turtle", "params": "classic", "pos": make_even(normalize(turtle()))})
    for c in out:
        c["n"] = len(c["pos"])
        c["layers"] = layer_count(c["pos"])
        c["id"] = f'{c["family"]}_{c["params"]}'
    # 중복 도형 제거 (같은 좌표 집합)
    seen, uniq = set(), []
    for c in out:
        key = tuple(c["pos"])
        if key not in seen:
            seen.add(key)
            uniq.append(c)
    return uniq


# (시작 단계, 끝 단계, 최소 패, 최대 패, 최소 층, 최대 층, 필요한 배치 수)
TIERS = [
    (1, 10, 24, 40, 1, 2, 10),
    (11, 50, 41, 80, 2, 3, 10),
    (51, 120, 81, 119, 3, 4, 10),
    (121, 200, 120, 144, 4, 5, 10),
]


def pick(pool, used, spec):
    _, _, lo, hi, lmin, lmax, need = spec
    by_family = defaultdict(list)
    for c in pool:
        if c["id"] in used or not (lo <= c["n"] <= hi and lmin <= c["layers"] <= lmax):
            continue
        by_family[c["family"]].append(c)
    # 크기 순으로 정렬해 같은 계열 안에서 고르게 퍼지도록 뽑고, 계열을 돌아가며 선택한다.
    for fam in by_family:
        lst = sorted(by_family[fam], key=lambda c: (c["n"], c["id"]))
        step = max(1, len(lst) // 3)
        by_family[fam] = lst[::step] + [c for i, c in enumerate(lst) if i % step]
    chosen = []
    fams = sorted(by_family, key=lambda f: (f == "turtle", f))
    idx = 0
    while len(chosen) < need and any(by_family.values()):
        fam = fams[idx % len(fams)]
        idx += 1
        if by_family[fam]:
            c = by_family[fam].pop(0)
            if c["id"] not in {x["id"] for x in chosen}:
                chosen.append(c)
    return chosen


def fmt_layout(c):
    lines = [
        "{",
        f'  "id": "{c["id"]}",',
        f'  "name": "{FAMILY_KO[c["family"]]} {c["params"]}",',
        f'  "difficulty": {difficulty(c["n"], c["layers"])},',
        '  "positions": [',
    ]
    body = [f'    {{ "x": {x}, "y": {y}, "z": {z} }}' for x, y, z in c["pos"]]
    lines.append(",\n".join(body))
    lines += ["  ]", "}", ""]
    return "\n".join(lines)


def main():
    pool = candidates()
    used, chosen_by_tier = set(), []
    for spec in TIERS:
        chosen = pick(pool, used, spec)
        if len(chosen) < spec[6]:
            print(f"경고: 단계 {spec[0]}~{spec[1]} 배치가 {len(chosen)}개뿐 (필요 {spec[6]})")
        for c in chosen:
            used.add(c["id"])
        chosen_by_tier.append(chosen)

    layouts_dir = os.path.join(ASSETS, "layouts")
    shutil.rmtree(layouts_dir, ignore_errors=True)
    os.makedirs(layouts_dir)
    stages = {}
    for spec, chosen in zip(TIERS, chosen_by_tier):
        for c in chosen:
            with open(os.path.join(layouts_dir, c["id"] + ".json"), "w", encoding="utf-8", newline="\n") as f:
                f.write(fmt_layout(c))
        for stage in range(spec[0], spec[1] + 1):
            stages[str(stage)] = chosen[(stage - spec[0]) % len(chosen)]["id"]
    with open(os.path.join(ASSETS, "levels.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"stages": stages}, f, ensure_ascii=False, indent=1)
        f.write("\n")

    total = 0
    for spec, chosen in zip(TIERS, chosen_by_tier):
        print(f"단계 {spec[0]}~{spec[1]}:")
        for c in chosen:
            total += 1
            print(f'  {c["id"]:<18} 패 {c["n"]:>3}  층 {c["layers"]}  난이도 {difficulty(c["n"], c["layers"])}')
    print(f"배치 {total}종 생성")


if __name__ == "__main__":
    main()
