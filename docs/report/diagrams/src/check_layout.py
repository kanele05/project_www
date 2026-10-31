# -*- coding: utf-8 -*-
"""Kiểm tra hình học các sơ đồ đã vẽ (đọc *.layout.json do ERD.py, DatabaseDiagram.py, UseCase_TongQuat.py ghi ra).

Quy tắc (mọi sai phạm của 1-4 phải bằng 0):
  1. Không đoạn nối nào xuyên/chạm phần trong của hình KHÔNG phải đầu mút của nó (oval, hộp, tác nhân, tiêu đề khung),
     chừa lề `margin`; đầu mút chỉ được chạm biên ở điểm đầu/cuối của đường, không đi xuyên qua chính hình đó.
  2. Hai đường không chồng nhau thành đoạn chung (song song, cùng đường thẳng, giao nhau theo đoạn).
  3. Nhãn (tên khoá ngoại, «include», «extend») không đè hình, đường nối, ký hiệu đầu mút hay nhãn khác, và nằm trong khung ảnh.
  4. Hai hình không chồng nhau; hình có `inside` phải nằm trọn trong khung; hình có `outside` phải nằm ngoài khung.
  5. Số lần cắt giữa các đường (chỉ báo cáo; ERD và Database Diagram phải bằng 0).
  6. (thêm) chữ trong oval nằm trọn trong oval; đầu mút nằm đúng trên biên hình của nó.

Dùng:  python3 check_layout.py [tệp.layout.json ...]   (mặc định: mọi *.layout.json cạnh tệp này)
Thoát với mã 1 nếu có vi phạm ở quy tắc 1-4 hoặc 6, hoặc số lần cắt vượt `expected.crossings`."""
import glob, json, math, os, sys

EPS = 1e-6


# ---- hình học cơ bản --------------------------------------------------------------------------------
def shape_poly(s, m=0.0, n=72):
    k = s['kind']
    if k == 'ellipse':
        rx, ry = s['rx'] + m, s['ry'] + m
        return [(s['cx'] + rx * math.cos(2 * math.pi * i / n), s['cy'] + ry * math.sin(2 * math.pi * i / n)) for i in range(n)]
    x0, y0, x1, y1 = s['bbox']
    return [(x0 - m, y0 - m), (x1 + m, y0 - m), (x1 + m, y1 + m), (x0 - m, y1 + m)]


def sat_overlap(P, Q):
    """hai đa giác lồi có phần trong chung không (chạm biên không tính)"""
    for poly in (P, Q):
        for i in range(len(poly)):
            a, b = poly[i], poly[(i + 1) % len(poly)]
            ax, ay = -(b[1] - a[1]), b[0] - a[0]
            p = [ax * x + ay * y for x, y in P]
            q = [ax * x + ay * y for x, y in Q]
            if max(p) <= min(q) + 1e-9 or max(q) <= min(p) + 1e-9:
                return False
    return True


def seg_rect_hit(p, q, r):
    """đoạn pq cắt phần trong của hình chữ nhật r=(x0,y0,x1,y1)? (Liang-Barsky, phần trong mở)"""
    x0, y0, x1, y1 = r
    dx, dy = q[0] - p[0], q[1] - p[1]
    t0, t1 = 0.0, 1.0
    for pp, qq in ((-dx, p[0] - x0), (dx, x1 - p[0]), (-dy, p[1] - y0), (dy, y1 - p[1])):
        if abs(pp) < EPS:
            if qq <= EPS:
                return False
        else:
            t = qq / pp
            if pp < 0:
                if t > t1:
                    return False
                t0 = max(t0, t)
            else:
                if t < t0:
                    return False
                t1 = min(t1, t)
    return t1 - t0 > EPS or (t1 >= t0 and False)


def seg_ellipse_hit(p, q, cx, cy, rx, ry):
    ax, ay = (p[0] - cx) / rx, (p[1] - cy) / ry
    bx, by = (q[0] - cx) / rx, (q[1] - cy) / ry
    dx, dy = bx - ax, by - ay
    L2 = dx * dx + dy * dy
    t = 0 if L2 < EPS else max(0, min(1, -(ax * dx + ay * dy) / L2))
    px, py = ax + t * dx, ay + t * dy
    return px * px + py * py < 1 - 1e-9


def seg_shape_hit(p, q, s, m=0.0):
    if s['kind'] == 'ellipse':
        return seg_ellipse_hit(p, q, s['cx'], s['cy'], s['rx'] + m, s['ry'] + m)
    x0, y0, x1, y1 = s['bbox']
    return seg_rect_hit(p, q, (x0 - m, y0 - m, x1 + m, y1 + m))


def trim(p, q, d):
    """điểm cách p một đoạn d về phía q"""
    L = math.hypot(q[0] - p[0], q[1] - p[1])
    if L < EPS:
        return p
    t = min(1.0, d / L)
    return (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)


def segs(pts):
    return [(pts[i], pts[i + 1]) for i in range(len(pts) - 1)]


def on_boundary(pt, s, tol=2.0):
    x, y = pt
    if s['kind'] == 'ellipse':
        v = ((x - s['cx']) / s['rx']) ** 2 + ((y - s['cy']) / s['ry']) ** 2
        return abs(math.sqrt(v) - 1) * min(s['rx'], s['ry']) <= tol
    x0, y0, x1, y1 = s['bbox']
    inside_x = x0 - tol <= x <= x1 + tol
    inside_y = y0 - tol <= y <= y1 + tol
    near = min(abs(x - x0), abs(x - x1), abs(y - y0), abs(y - y1)) <= tol
    return inside_x and inside_y and near


def seg_seg(a1, a2, b1, b2):
    """giao của hai đoạn: trả về ('cross'|'touch'|'overlap'|None)"""
    def orient(p, q, r):
        return (q[0] - p[0]) * (r[1] - p[1]) - (q[1] - p[1]) * (r[0] - p[0])
    d1, d2 = orient(b1, b2, a1), orient(b1, b2, a2)
    d3, d4 = orient(a1, a2, b1), orient(a1, a2, b2)
    la = math.hypot(a2[0] - a1[0], a2[1] - a1[1]) or 1
    lb = math.hypot(b2[0] - b1[0], b2[1] - b1[1]) or 1
    n1, n2, n3, n4 = d1 / lb, d2 / lb, d3 / la, d4 / la   # khoảng cách có dấu (px)
    if abs(n1) < 0.8 and abs(n2) < 0.8 and abs(n3) < 0.8 and abs(n4) < 0.8:
        # cùng đường thẳng: tính chiều dài chung
        ux, uy = (a2[0] - a1[0]) / la, (a2[1] - a1[1]) / la
        ta = sorted([0, la])
        tb = sorted([(b1[0] - a1[0]) * ux + (b1[1] - a1[1]) * uy, (b2[0] - a1[0]) * ux + (b2[1] - a1[1]) * uy])
        ov = min(ta[1], tb[1]) - max(ta[0], tb[0])
        return ('overlap', ov) if ov > 1.0 else ('touch', 0) if ov >= -0.8 else None
    if (n1 > 0.8 and n2 > 0.8) or (n1 < -0.8 and n2 < -0.8) or (n3 > 0.8 and n4 > 0.8) or (n3 < -0.8 and n4 < -0.8):
        return None
    # có giao: phân biệt cắt thật / chạm đầu mút
    return ('cross', 0)


def parallel_close_overlap(a1, a2, b1, b2, dist=1.6):
    """hai đoạn gần song song (góc < ~0.2 độ) và sát nhau trong vòng dist px và trùng chiếu > 1px"""
    la = math.hypot(a2[0] - a1[0], a2[1] - a1[1])
    lb = math.hypot(b2[0] - b1[0], b2[1] - b1[1])
    if la < EPS or lb < EPS:
        return 0
    ux, uy = (a2[0] - a1[0]) / la, (a2[1] - a1[1]) / la
    vx, vy = (b2[0] - b1[0]) / lb, (b2[1] - b1[1]) / lb
    if abs(ux * vy - uy * vx) > 0.004:
        return 0
    nx, ny = -uy, ux
    d = max(abs((b1[0] - a1[0]) * nx + (b1[1] - a1[1]) * ny), abs((b2[0] - a1[0]) * nx + (b2[1] - a1[1]) * ny))
    if d > dist:
        return 0
    tb = sorted([(b1[0] - a1[0]) * ux + (b1[1] - a1[1]) * uy, (b2[0] - a1[0]) * ux + (b2[1] - a1[1]) * uy])
    return max(0, min(la, tb[1]) - max(0, tb[0]))


def glyph_rect(g):
    if g['t'] == 'circle':
        return (g['c'][0] - g['r'], g['c'][1] - g['r'], g['c'][0] + g['r'], g['c'][1] + g['r'])
    (x1, y1), (x2, y2) = g['p']
    return (min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2))


def rect_overlap(a, b, m=0.0):
    return not (a[2] + m <= b[0] or b[2] + m <= a[0] or a[3] + m <= b[1] or b[3] + m <= a[1])


# ---- kiểm tra ----------------------------------------------------------------------------------------
def check(L, verbose=True):
    S = {s['id']: s for s in L['shapes']}
    GRP = {}
    for s in L['shapes']:
        GRP.setdefault(s.get('group', s['id']), []).append(s)
    gof = {s['id']: s.get('group', s['id']) for s in L['shapes']}
    M = L.get('margin', 6)
    W, H = L['canvas']
    out = {'r1': [], 'r2': [], 'r3': [], 'r4': [], 'r6': [], 'cross': []}
    E = L['edges']

    # 1. đường nối với hình
    for e in E:
        pts = [tuple(p) for p in e['points']]
        own = {e['from'], e['to']}
        sg = segs(pts)
        for sid, s in S.items():
            if gof[sid] in own:
                continue
            for a, b in sg:
                if seg_shape_hit(a, b, s, M):
                    out['r1'].append(f"{e['id']} xuyên/chạm {sid}")
                    break
        # đầu mút: nằm trên biên hình của mình và không đi xuyên chính hình đó (cắt bỏ 2px ở đầu mút)
        for k, gid in ((0, e['from']), (len(pts) - 1, e['to'])):
            members = GRP.get(gid)
            if not members:
                continue
            if not any(on_boundary(pts[k], s) for s in members):
                out['r6'].append(f"{e['id']}: đầu mút {k} không nằm trên biên {gid}")
            for s in members:
                for i, (a, b) in enumerate(sg):
                    aa, bb = a, b
                    if k == 0 and i == 0:
                        aa = trim(a, b, 2.0)
                    if k == len(pts) - 1 and i == len(sg) - 1:
                        bb = trim(b, a, 2.0)
                    if seg_shape_hit(aa, bb, s, -0.0):
                        out['r1'].append(f"{e['id']} đi vào trong {s['id']} (đầu mút)")
                        break
        # ký hiệu đầu mút (glyph) không chạm hình lạ
        for g in e.get('glyphs', []):
            r = glyph_rect(g)
            for sid, s in S.items():
                if gof[sid] in own:
                    continue
                if rect_overlap(r, shape_bbox(s), 1.0):
                    out['r1'].append(f"ký hiệu của {e['id']} chạm {sid}")
                    break

    # 2. chồng đoạn + 5. cắt nhau
    ncross = 0
    for i in range(len(E)):
        for j in range(i + 1, len(E)):
            A = [tuple(p) for p in E[i]['points']]
            B = [tuple(p) for p in E[j]['points']]
            cnt = 0
            for a1, a2 in segs(A):
                for b1, b2 in segs(B):
                    ov = parallel_close_overlap(a1, a2, b1, b2)
                    if ov > 1.0:
                        out['r2'].append(f"{E[i]['id']} và {E[j]['id']} chung đoạn dài {ov:.1f}px")
                        continue
                    r = seg_seg(a1, a2, b1, b2)
                    if r and r[0] in ('cross', 'touch'):
                        # chạm tại đúng đầu mút chung (cùng một điểm) không tính là cắt
                        shared = any(math.hypot(p[0] - q[0], p[1] - q[1]) < 1.0 for p in (a1, a2) for q in (b1, b2)
                                     if (p in (A[0], A[-1])) and (q in (B[0], B[-1])))
                        if not shared:
                            cnt += 1
            if cnt:
                out['cross'].append(f"{E[i]['id']} x {E[j]['id']}: {cnt}")
                ncross += cnt
    # glyph của đường này không chạm đường khác
    for e in E:
        for g in e.get('glyphs', []):
            r = glyph_rect(g)
            for f in E:
                if f is e:
                    continue
                pts = [tuple(p) for p in f['points']]
                if any(seg_rect_hit(a, b, (r[0] - 1, r[1] - 1, r[2] + 1, r[3] + 1)) for a, b in segs(pts)):
                    out['r3'].append(f"ký hiệu của {e['id']} chạm đường {f['id']}")
                    break

    # 3. nhãn
    labs = L.get('labels', [])
    for lb in L.get('failed_labels', []):
        out['r3'].append(f'nhãn {lb} không đặt được')
    for i, lb in enumerate(labs):
        r = lb['bbox']
        if r[0] < 0 or r[1] < 0 or r[2] > W or r[3] > H:
            out['r3'].append(f"nhãn {lb['id']} ra ngoài khung ảnh")
        for sid, s in S.items():
            if shape_hit_rect(s, r, 2.0, skip_ok=lb.get('inside_ok')):
                out['r3'].append(f"nhãn {lb['id']} đè hình {sid}")
        for e in E:
            pts = [tuple(p) for p in e['points']]
            if any(seg_rect_hit(a, b, (r[0] - 1.5, r[1] - 1.5, r[2] + 1.5, r[3] + 1.5)) for a, b in segs(pts)):
                out['r3'].append(f"nhãn {lb['id']} đè đường {e['id']}")
            for g in e.get('glyphs', []):
                if rect_overlap(r, glyph_rect(g), 1.0):
                    out['r3'].append(f"nhãn {lb['id']} đè ký hiệu của {e['id']}")
                    break
        for k in range(i + 1, len(labs)):
            if rect_overlap(r, labs[k]['bbox'], 1.0):
                out['r3'].append(f"nhãn {lb['id']} đè nhãn {labs[k]['id']}")

    # 4. hình chồng hình, trong/ngoài khung
    ids = list(S)
    cont = {c['id']: c for c in L.get('containers', [])}
    for i in range(len(ids)):
        for j in range(i + 1, len(ids)):
            a, b = S[ids[i]], S[ids[j]]
            if gof[ids[i]] == gof[ids[j]]:
                continue
            if sat_overlap(shape_poly(a, 0), shape_poly(b, 0)) or sat_overlap(shape_poly(a, 2), shape_poly(b, 2)) and False:
                out['r4'].append(f"{ids[i]} chồng {ids[j]}")
            elif sat_overlap(shape_poly(a, L.get('shape_gap', 1.5)), shape_poly(b, L.get('shape_gap', 1.5))):
                out['r4'].append(f"{ids[i]} sát/chồng {ids[j]}")
    for sid, s in S.items():
        if s.get('inside') in cont:
            c = cont[s['inside']]['bbox']
            bb = shape_bbox(s)
            if not (bb[0] >= c[0] + 2 and bb[1] >= c[1] + 2 and bb[2] <= c[2] - 2 and bb[3] <= c[3] - 2):
                out['r4'].append(f"{sid} không nằm trọn trong khung {s['inside']}")
        if s.get('outside') in cont:
            c = cont[s['outside']]['bbox']
            if rect_overlap(shape_bbox(s), c, 0):
                out['r4'].append(f"{sid} đè lên khung {s['outside']}")
        bb = shape_bbox(s)
        if bb[0] < 0 or bb[1] < 0 or bb[2] > W or bb[3] > H:
            out['r4'].append(f"{sid} ra ngoài khung ảnh")
    # 6. chữ trong oval
    for sid, s in S.items():
        for tb in s.get('text_boxes', []):
            if s['kind'] == 'ellipse':
                for cx, cy in ((tb[0], tb[1]), (tb[2], tb[1]), (tb[0], tb[3]), (tb[2], tb[3])):
                    v = ((cx - s['cx']) / s['rx']) ** 2 + ((cy - s['cy']) / s['ry']) ** 2
                    if v > 0.97:
                        out['r6'].append(f"chữ trong {sid} sát/vượt biên oval")
                        break
    return out, ncross


def shape_bbox(s):
    if s['kind'] == 'ellipse':
        return (s['cx'] - s['rx'], s['cy'] - s['ry'], s['cx'] + s['rx'], s['cy'] + s['ry'])
    return tuple(s['bbox'])


def shape_hit_rect(s, r, m, skip_ok=None):
    if skip_ok and s['id'] in skip_ok:
        return False
    return sat_overlap(shape_poly(s, m), [(r[0], r[1]), (r[2], r[1]), (r[2], r[3]), (r[0], r[3])])


def main(paths):
    bad = 0
    for p in paths:
        L = json.load(open(p, encoding='utf8'))
        out, nc = check(L)
        n_shape, n_edge, n_lab = len(L['shapes']), len(L['edges']), len(L.get('labels', []))
        print(f"== {L['name']}: {n_shape} hình, {n_edge} đường, {n_lab} nhãn, khung {L['canvas'][0]}x{L['canvas'][1]}")
        names = {'r1': '1. đường xuyên/chạm hình lạ', 'r2': '2. đường chồng đoạn', 'r3': '3. nhãn/ký hiệu đè',
                 'r4': '4. hình chồng hình / ngoài khung', 'r6': '6. đầu mút/chữ trong oval'}
        for k in ('r1', 'r2', 'r3', 'r4', 'r6'):
            v = out[k]
            print(f"   quy tắc {names[k]}: {len(v)} vi phạm")
            for x in v[:15]:
                print('       -', x)
            bad += len(v)
        exp = L.get('expected', {}).get('crossings')
        print(f"   quy tắc 5. số lần cắt giữa các đường: {nc}" + (f" (kỳ vọng {exp})" if exp is not None else ''))
        for x in out['cross'][:40]:
            print('       -', x)
        if exp is not None and nc > exp:
            bad += 1
    print('KẾT QUẢ:', 'ĐẠT' if bad == 0 else f'KHÔNG ĐẠT ({bad})')
    return 1 if bad else 0


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    paths = sys.argv[1:] or sorted(glob.glob(os.path.join(here, '*.layout.json')))
    sys.exit(main(paths))
