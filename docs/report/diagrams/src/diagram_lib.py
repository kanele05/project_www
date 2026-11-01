# -*- coding: utf-8 -*-
"""Thư viện vẽ sơ đồ có toạ độ tường minh: đo chữ, định tuyến đường nối vuông góc (A*), đặt nhãn không đè,
ghi bố cục ra JSON để check_layout.py kiểm tra bằng hình học."""
import heapq, json, math, os, random
from PIL import ImageFont

FONT_REG = '/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf'
FONT_BOLD = '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf'
FAMILY = 'Liberation Sans, Arial, sans-serif'
_fc = {}


def tw(text, size, bold=False):
    key = (size, bold)
    if key not in _fc:
        _fc[key] = ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)
    return _fc[key].getlength(text)


def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def simplify(pts):
    """bỏ điểm thẳng hàng và điểm trùng"""
    out = []
    for p in pts:
        if out and out[-1] == p:
            continue
        out.append(p)
    res = [out[0]]
    for i in range(1, len(out) - 1):
        a, b, c = res[-1], out[i], out[i + 1]
        if (a[0] == b[0] == c[0]) or (a[1] == b[1] == c[1]):
            continue
        res.append(b)
    res.append(out[-1])
    return res


class Router:
    """Định tuyến vuông góc trên lưới g px. Hộp được nới thêm 1 ô. Hai đường không bao giờ chung đoạn;
    cắt nhau bị phạt nặng (để A* chỉ cắt khi bắt buộc)."""
    DIRS = [(1, 0), (-1, 0), (0, 1), (0, -1)]

    def __init__(self, W, H, g, boxes, stub=3, bend=7, cross=400, margin=1):
        self.W, self.H, self.g = W, H, g
        self.nx, self.ny = W // g + 1, H // g + 1
        self.boxes = boxes                      # id -> (x, y, w, h), toạ độ là bội của g
        self.stub, self.bend, self.cross = stub, bend, cross
        self.blocked = set()
        for (x, y, w, h) in boxes.values():
            for i in range(x // g - margin + 1, (x + w) // g + margin):
                for j in range(y // g - margin + 1, (y + h) // g + margin):
                    self.blocked.add((i, j))
        for i in range(self.nx):
            for j in range(self.ny):
                if i < 1 or j < 1 or i > self.nx - 2 or j > self.ny - 2:
                    self.blocked.add((i, j))
        self.reset()

    def reset(self):
        self.resv = {}          # node -> edge id (vùng giữ chỗ quanh gốc đường nối)
        self.nodes = {}         # node -> {edge: 'h'|'v'|'c'}
        self.hseg = {}          # ((i,j),(i+1,j)) -> edge
        self.vseg = {}
        self.slots_used = {}    # id hộp -> list (side, k)
        self.routes = {}

    # --- cổng (port) ---------------------------------------------------------------------
    def slots(self, bid, only_sides=None, row_range=None):
        g = self.g
        x, y, w, h = self.boxes[bid]
        out = []
        x0, y0, x1, y1 = x // g, y // g, (x + w) // g, (y + h) // g
        for side in ('L', 'R', 'T', 'B'):
            if only_sides and side not in only_sides:
                continue
            if side in 'LR':
                rng = range(y0 + 1, y1)
                if row_range:
                    rng = [j for j in rng if row_range[0] <= j * g <= row_range[1]]
                for j in rng:
                    out.append((side, (x0 if side == 'L' else x1, j)))
            else:
                for i in range(x0 + 1, x1):
                    out.append((side, (i, y0 if side == 'T' else y1)))
        return out

    @staticmethod
    def outward(side):
        return {'L': (-1, 0), 'R': (1, 0), 'T': (0, -1), 'B': (0, 1)}[side]

    def stub_nodes(self, side, p):
        d = self.outward(side)
        lat = (0, 1) if d[0] else (1, 0)
        return [(p[0] + d[0] * k, p[1] + d[1] * k) for k in range(self.stub + 1)], lat

    def stub_ok(self, eid, side, p):
        d = self.outward(side)
        lat = (0, 1) if d[0] else (1, 0)
        for k in range(0, self.stub + 1):
            q = (p[0] + d[0] * k, p[1] + d[1] * k)
            if k >= 1 and q in self.blocked:
                return False
            if not (0 <= q[0] < self.nx and 0 <= q[1] < self.ny):
                return False
            for s in ((0,) if k == self.stub else (-1, 0, 1)):
                r = (q[0] + lat[0] * s, q[1] + lat[1] * s)
                if self.resv.get(r, eid) != eid:
                    return False
                if self.nodes.get(r) and any(e != eid for e in self.nodes[r]) and (k >= 1):
                    return False
            if self.nodes.get(q) and any(e != eid for e in self.nodes[q]):
                return False
        return True

    # --- A* ---------------------------------------------------------------------------------
    def route(self, eid, a, b, a_sides=None, b_sides=None, a_rows=None, b_rows=None):
        g = self.g
        starts, ends = {}, {}
        for side, p in self.slots(a, a_sides, a_rows):
            if self.stub_ok(eid, side, p):
                nodes, _ = self.stub_nodes(side, p)
                starts[nodes[-1]] = (side, p)
        for side, p in self.slots(b, b_sides, b_rows):
            if self.stub_ok(eid, side, p):
                nodes, _ = self.stub_nodes(side, p)
                ends[nodes[-1]] = (side, p)
        if not starts or not ends:
            return None
        ex = [n[0] for n in ends]
        ey = [n[1] for n in ends]
        bx0, bx1, by0, by1 = min(ex), max(ex), min(ey), max(ey)

        def hfun(n):
            dx = max(bx0 - n[0], 0, n[0] - bx1)
            dy = max(by0 - n[1], 0, n[1] - by1)
            return dx + dy

        pq = []
        best = {}
        parent = {}
        for n, (side, p) in starts.items():
            di = self.DIRS.index(self.outward(side))
            st = (n, di)
            best[st] = 0
            parent[st] = None
            heapq.heappush(pq, (hfun(n), 0, n, di))
        goal = None
        while pq:
            f, c, n, di = heapq.heappop(pq)
            if best.get((n, di), 1e18) < c:
                continue
            if n in ends:
                goal = (n, di)
                break
            for nd, (dx, dy) in enumerate(self.DIRS):
                if nd == (di ^ 1):
                    continue
                m = (n[0] + dx, n[1] + dy)
                if not (0 <= m[0] < self.nx and 0 <= m[1] < self.ny):
                    continue
                if m in self.blocked:
                    continue
                if self.resv.get(m, eid) != eid:
                    continue
                cost = 1 + (self.bend if nd != di else 0)
                # đoạn trùng
                key = (n, m) if dx + dy > 0 else (m, n)
                seg = self.hseg if dy == 0 else self.vseg
                if key in seg and seg[key] != eid:
                    continue
                inf = self.nodes.get(m)
                if inf:
                    other = {e: k for e, k in inf.items() if e != eid}
                    if other:
                        # chỉ được cắt vuông góc ở nút mà đường kia đi thẳng
                        kinds = set(other.values())
                        want = 'v' if dy == 0 else 'h'
                        if kinds != {want} or (nd != di):
                            continue
                        # nút kế tiếp cùng hướng phải còn đi thẳng được (tránh bẻ ngay trên đường kia)
                        cost += self.cross
                nc = c + cost
                st = (m, nd)
                if nc < best.get(st, 1e18):
                    best[st] = nc
                    parent[st] = (n, di)
                    heapq.heappush(pq, (nc + hfun(m), nc, m, nd))
        if goal is None:
            return None
        path = []
        cur = goal
        while cur is not None:
            path.append(cur[0])
            cur = parent[cur]
        path.reverse()
        return starts, ends, path

    def commit(self, eid, a, b, sa, pa, sb, pb, path):
        """ghi nhận đường đi: sa/pa = cổng đầu (cạnh, nút), sb/pb = cổng cuối"""
        na, lat_a = self.stub_nodes(sa, pa)
        nb, lat_b = self.stub_nodes(sb, pb)
        pts = list(na) + path[1:] + list(reversed(nb))[1:]
        # dọn trùng nút liên tiếp
        clean = [pts[0]]
        for q in pts[1:]:
            if q != clean[-1]:
                clean.append(q)
        # đánh dấu
        for s, p, lat in ((sa, pa, lat_a), (sb, pb, lat_b)):
            d = self.outward(s)
            for k in range(0, self.stub + 1):
                q = (p[0] + d[0] * k, p[1] + d[1] * k)
                for sft in ((0,) if k == self.stub else (-1, 0, 1)):
                    self.resv[(q[0] + lat[0] * sft, q[1] + lat[1] * sft)] = eid
        for i in range(len(clean) - 1):
            u, v = clean[i], clean[i + 1]
            step = (v[0] > u[0]) - (v[0] < u[0]), (v[1] > u[1]) - (v[1] < u[1])
            n = u
            while n != v:
                m = (n[0] + step[0], n[1] + step[1])
                key = (n, m) if step[0] + step[1] > 0 else (m, n)
                (self.hseg if step[1] == 0 else self.vseg)[key] = eid
                n = m
        for i, q in enumerate(clean):
            kind = 'c'
            if 0 < i < len(clean) - 1:
                u, w = clean[i - 1], clean[i + 1]
                if u[0] == q[0] == w[0]:
                    kind = 'v'
                elif u[1] == q[1] == w[1]:
                    kind = 'h'
            self.nodes.setdefault(q, {})[eid] = kind
        # nút giữa các đỉnh (đi thẳng)
        for i in range(len(clean) - 1):
            u, v = clean[i], clean[i + 1]
            step = (v[0] > u[0]) - (v[0] < u[0]), (v[1] > u[1]) - (v[1] < u[1])
            n = (u[0] + step[0], u[1] + step[1])
            while n != v:
                self.nodes.setdefault(n, {})[eid] = 'h' if step[1] == 0 else 'v'
                n = (n[0] + step[0], n[1] + step[1])
        self.routes[eid] = clean

    def px(self, nodes):
        g = self.g
        return [[n[0] * g, n[1] * g] for n in nodes]


def route_all(R, edges, tries=60, seed=1, log=print, hints=None):
    """edges: list of dict(id, a, b) (a = hộp con, b = hộp cha). Thử nhiều thứ tự, giữ phương án ít cắt nhất.
    hints: id -> dict(a_sides=..., b_sides=...)"""
    hints = hints or {}
    rnd = random.Random(seed)
    best = None
    order0 = list(edges)
    for t in range(tries):
        R.reset()
        order = list(order0)
        if t:
            rnd.shuffle(order)
        ok = True
        info = {}
        for e in order:
            h = hints.get(e['id'], {})
            r = R.route(e['id'], e['a'], e['b'], h.get('a_sides'), h.get('b_sides'), h.get('a_rows'), h.get('b_rows'))
            if r is None:
                ok = False
                break
            starts, ends, path = r
            sa, pa = starts[path[0]]
            sb, pb = ends[path[-1]]
            R.commit(e['id'], e['a'], e['b'], sa, pa, sb, pb, path)
            info[e['id']] = (sa, pa, sb, pb)
        if not ok:
            continue
        score = count_cross({k: v for k, v in R.routes.items()}) * 10000 + sum(len(v) * 3 + sum(
            abs(v[i][0] - v[i + 1][0]) + abs(v[i][1] - v[i + 1][1]) for i in range(len(v) - 1)) for v in R.routes.values())
        if best is None or score < best[0]:
            best = (score, dict(R.routes), dict(info))
            log('  lần', t, 'điểm', score, 'cắt', count_cross(R.routes))
            if count_cross(R.routes) == 0 and t >= 12:
                break
    if best is None:
        return None
    R.routes = best[1]
    return best[1], best[2]


def seg_cross(a1, a2, b1, b2):
    """giao nhau của hai đoạn vuông góc (kể cả chạm đầu mút) -> bool"""
    def box(p, q):
        return min(p[0], q[0]), min(p[1], q[1]), max(p[0], q[0]), max(p[1], q[1])
    A, B = box(a1, a2), box(b1, b2)
    return not (A[2] < B[0] or B[2] < A[0] or A[3] < B[1] or B[3] < A[1])


def count_cross(routes):
    ids = list(routes)
    n = 0
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            A, B = routes[ids[i]], routes[ids[j]]
            hit = 0
            for p in range(len(A) - 1):
                for q in range(len(B) - 1):
                    if seg_cross(A[p], A[p + 1], B[q], B[q + 1]):
                        hit += 1
            n += hit
    return n


# --- nhãn ---------------------------------------------------------------------------------------

def rects_overlap(a, b, m=0):
    return not (a[2] + m <= b[0] or b[2] + m <= a[0] or a[3] + m <= b[1] or b[3] + m <= a[1])


def seg_rect(p, q, r, m=0):
    """đoạn pq (trục) có cắt hình chữ nhật r=(x0,y0,x1,y1) nới m không"""
    x0, y0, x1, y1 = r[0] - m, r[1] - m, r[2] + m, r[3] + m
    sx0, sx1 = min(p[0], q[0]), max(p[0], q[0])
    sy0, sy1 = min(p[1], q[1]), max(p[1], q[1])
    return not (sx1 < x0 or sx0 > x1 or sy1 < y0 or sy0 > y1)


def place_labels(labels, polylines, boxes_px, glyph_rects, W, H, size, extra_rects=(), gap=6, pad=2):
    """labels: list of dict(id, text, edge). Trả về dict id -> dict(x,y,w,h,rot). Tham lam theo độ khó."""
    placed = {}
    occupied = list(extra_rects)

    def variants(text):
        v = [[text]]
        if tw(text, size) > 130 and text.count('_') >= 2:
            parts = text.split('_')
            best = None
            for k in range(1, len(parts)):
                l1, l2 = '_'.join(parts[:k]) + '_', '_'.join(parts[k:])
                d = max(tw(l1, size), tw(l2, size))
                if best is None or d < best[0]:
                    best = (d, [l1, l2])
            v.append(best[1])
        return v

    def cands(lb):
        out = []
        pts = polylines[lb['edge']]
        for lines in variants(lb['text']):
            w = max(tw(l, size) for l in lines) + 2 * pad
            h = len(lines) * (size + 2) + 2 * pad - 1
            vpen = 0 if len(lines) == 1 else 12
            for i in range(len(pts) - 1):
                p, q = pts[i], pts[i + 1]
                L = abs(p[0] - q[0]) + abs(p[1] - q[1])
                if p[1] == q[1]:   # ngang
                    x0, x1 = min(p[0], q[0]), max(p[0], q[0])
                    for f in (0.5, 0.35, 0.65, 0.25, 0.75, 0.15, 0.85):
                        cx = x0 + (x1 - x0) * f
                        if L >= w + 56:
                            cx = min(max(cx, x0 + w / 2 + 28), x1 - w / 2 - 28)
                        elif L < 40:
                            continue
                        for side, pen in ((-1, 0), (1, 5)):
                            y = p[1] + side * (gap + h / 2 + 1)
                            out.append((abs(f - .5) * 40 + pen + vpen + (0 if L >= w + 56 else 25), (cx - w / 2, y - h / 2, cx + w / 2, y + h / 2), 0, lines))
                else:              # dọc
                    y0, y1 = min(p[1], q[1]), max(p[1], q[1])
                    for f in (0.5, 0.35, 0.65, 0.25, 0.75, 0.15, 0.85):
                        cy = y0 + (y1 - y0) * f
                        if L >= w + 56:
                            cy = min(max(cy, y0 + w / 2 + 28), y1 - w / 2 - 28)
                            for side, pen in ((-1, 0), (1, 5)):
                                x = p[0] + side * (gap + h / 2 + 1)
                                out.append((60 + abs(f - .5) * 40 + pen + vpen, (x - h / 2, cy - w / 2, x + h / 2, cy + w / 2), 90, lines))
                        if L >= 24:
                            for side, pen in ((1, 0), (-1, 3)):
                                xx = p[0] + side * (gap + 1 + w / 2)
                                out.append((30 + abs(f - .5) * 40 + pen + vpen, (xx - w / 2, cy - h / 2, xx + w / 2, cy + h / 2), 0, lines))
        return sorted(out, key=lambda c: c[0]), None, None

    allc = {lb['id']: cands(lb) for lb in labels}
    order = sorted(labels, key=lambda lb: len(allc[lb['id']][0]))

    def free(r, owner):
        if r[0] < 4 or r[1] < 4 or r[2] > W - 4 or r[3] > H - 4:
            return False
        for b in boxes_px:
            if rects_overlap(r, b, 4):
                return False
        for o in occupied:
            if rects_overlap(r, o, 3):
                return False
        for g in glyph_rects:
            if rects_overlap(r, g, 1):
                return False
        for eid, pts in polylines.items():
            for i in range(len(pts) - 1):
                if seg_rect(pts[i], pts[i + 1], r, 2):
                    return False
        return True

    for lb in order:
        cs, _, _ = allc[lb['id']]
        for pen, r, rot, lines in cs:
            if free(r, lb['edge']):
                placed[lb['id']] = {'bbox': r, 'rot': rot, 'lines': lines, 'text': lb['text'], 'edge': lb['edge']}
                occupied.append(r)
                break
        else:
            placed[lb['id']] = None
    return placed


def dump(path, data):
    with open(path, 'w', encoding='utf8') as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
