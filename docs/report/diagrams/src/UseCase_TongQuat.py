# -*- coding: utf-8 -*-
"""Hình 2.2 - Biểu đồ use case tổng quát (đặt trên trang ngang). Mọi hình và đường nối có toạ độ tường minh;
bố cục được ghi ra UseCase_TongQuat.layout.json để check_layout.py kiểm tra hình học (không đường nào xuyên oval,
không nhãn nào đè lên hình/đường, không hai hình chồng nhau, mọi use case nằm trong khung hệ thống).

Nội dung giữ nguyên bản cũ: 3 tác nhân (Khách vãng lai, Khách hàng, Quản trị viên) và hai quan hệ kế thừa;
40 use case; «include» về Đăng nhập; «extend» (Thêm/Sửa/Xóa cho giỏ hàng và 5 mục quản lý, một «Sửa» duy nhất cho
Quản lý đơn đặt tour, Đăng xuất, Áp dụng mã giảm giá, Huỷ đơn đặt tour, Đánh giá tour đã đi).
Chạy: python3 UseCase_TongQuat.py  ->  UseCase_TongQuat.svg, UseCase_TongQuat.layout.json, ../png/UseCase_TongQuat.png"""
import math, os
import cairosvg
from diagram_lib import tw, esc, FAMILY, dump
from check_layout import shape_poly, sat_overlap, seg_rect_hit, rect_overlap, seg_shape_hit

HERE = os.path.dirname(os.path.abspath(__file__))
W, H = 2520, 1650
FILL, STROKE = '#7FC8F5', '#222222'
FS_MAIN, FS_SMALL, FS_CRUD, FS_LBL, FS_ACT, FS_TITLE = 24, 22, 20, 20, 22, 28
BX0, BY0, BX1, BY1 = 262, 24, 2300, 1575          # khung hệ thống
shapes, edges, svg_shapes, svg_edges = [], [], [], []
E = {}                                            # id -> (cx, cy, rx, ry)


def fit_rx(lines, fs, ry, v=0.86):
    """bán kính ngang nhỏ nhất để chữ nằm trọn trong oval (tâm chữ trùng tâm oval)"""
    half_w = max(tw(l, fs) for l in lines) / 2
    half_h = (len(lines) * fs * 1.15) / 2
    k = v - (half_h / ry) ** 2
    return half_w / math.sqrt(k)


def ell(eid, cx, cy, lines, fs, rx=None, ry=30):
    if isinstance(lines, str):
        lines = [lines]
    if rx is None:
        rx = fit_rx(lines, fs, ry)
    rx, ry = int(round(rx)), int(ry)
    E[eid] = (cx, cy, rx, ry)
    tb = []
    n = len(lines)
    for k, l in enumerate(lines):
        yy = cy + (k - (n - 1) / 2) * fs * 1.15
        w = tw(l, fs)
        tb.append([cx - w / 2, yy - fs * 0.52, cx + w / 2, yy + fs * 0.52])
    shapes.append({'id': eid, 'kind': 'ellipse', 'cx': cx, 'cy': cy, 'rx': rx, 'ry': ry, 'inside': 'boundary', 'text_boxes': tb})
    t = ''.join(f'<text x="{cx}" y="{cy + (k - (n - 1) / 2) * fs * 1.15 + fs * 0.35:.1f}" font-size="{fs}" text-anchor="middle" '
                f'font-family="{FAMILY}">{esc(l)}</text>' for k, l in enumerate(lines))
    svg_shapes.append(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{FILL}" stroke="{STROKE}" stroke-width="1.6"/>' + t)


def ell_pt(eid, phi_deg):
    """điểm trên biên oval theo góc tham số (y hướng lên)"""
    cx, cy, rx, ry = E[eid]
    p = math.radians(phi_deg)
    return (cx + rx * math.cos(p), cy - ry * math.sin(p))


def ell_toward(eid, tx, ty):
    cx, cy, rx, ry = E[eid]
    dx, dy = tx - cx, ty - cy
    k = 1 / math.sqrt((dx / rx) ** 2 + (dy / ry) ** 2)
    return (cx + dx * k, cy + dy * k)


ACT = {}


def actor(aid, x, y, name):
    """người que cỡ lớn: đầu, thân, tay, chân; nhãn tên bên dưới. Hai hình (thân + nhãn) cùng nhóm."""
    fig = [x - 33, y - 64, x + 33, y + 60]
    lw = tw(name, FS_ACT)
    lbl = [x - lw / 2 - 4, y + 72, x + lw / 2 + 4, y + 100]
    shapes.append({'id': aid + '_than', 'group': aid, 'kind': 'rect', 'bbox': fig, 'outside': 'boundary'})
    shapes.append({'id': aid + '_ten', 'group': aid, 'kind': 'rect', 'bbox': lbl, 'outside': 'boundary'})
    ACT[aid] = {'x': x, 'y': y, 'fig': fig, 'lbl': lbl}
    svg_shapes.append(
        f'<circle cx="{x}" cy="{y-47}" r="16" fill="{FILL}" stroke="{STROKE}" stroke-width="1.8"/>'
        f'<path d="M{x} {y-31} L{x} {y+18} M{x-33} {y-14} L{x+33} {y-14} M{x} {y+18} L{x-24} {y+58} M{x} {y+18} L{x+24} {y+58}" '
        f'stroke="{STROKE}" stroke-width="1.8" fill="none"/>'
        f'<text x="{x}" y="{y+92}" font-size="{FS_ACT}" text-anchor="middle" font-family="{FAMILY}">{esc(name)}</text>')


def hand(aid, side):
    a = ACT[aid]
    return (a['x'] + (33 if side == 'R' else -33), a['y'] - 14)


def unit(p, q):
    L = math.hypot(q[0] - p[0], q[1] - p[1])
    return ((q[0] - p[0]) / L, (q[1] - p[1]) / L)


def open_arrow(tip, u):
    """mũi tên mở (V) tại tip, hướng u"""
    out = []
    for ang in (26, -26):
        a = math.radians(ang)
        vx = -(u[0] * math.cos(a) - u[1] * math.sin(a))
        vy = -(u[0] * math.sin(a) + u[1] * math.cos(a))
        out.append([[tip[0], tip[1]], [tip[0] + vx * 18, tip[1] + vy * 18]])
    return out


def tri_arrow(tip, u):
    n = (-u[1], u[0])
    b = (tip[0] - u[0] * 26, tip[1] - u[1] * 26)
    p1, p2 = (b[0] + n[0] * 13, b[1] + n[1] * 13), (b[0] - n[0] * 13, b[1] - n[1] * 13)
    return [[list(tip), list(p1)], [list(p1), list(p2)], [list(p2), list(tip)]], (tip, p1, p2)


def add_edge(eid, a, b, pts, kind, label=None):
    pts = [[round(x, 1), round(y, 1)] for x, y in pts]
    u = unit(pts[-2], pts[-1])
    glyphs, svg = [], []
    poly = ' '.join(f'{x},{y}' for x, y in pts)
    if kind == 'assoc':
        svg.append(f'<polyline points="{poly}" fill="none" stroke="{STROKE}" stroke-width="1.6"/>')
    elif kind == 'gen':
        segs, tri = tri_arrow(tuple(pts[-1]), u)
        svg.append(f'<polyline points="{poly}" fill="none" stroke="{STROKE}" stroke-width="1.6"/>')
        svg.append('<polygon points="%s" fill="white" stroke="%s" stroke-width="1.6"/>' % (' '.join(f'{x:.1f},{y:.1f}' for x, y in tri), STROKE))
        glyphs = [{'t': 'line', 'p': s} for s in segs]
    else:
        svg.append(f'<polyline points="{poly}" fill="none" stroke="{STROKE}" stroke-width="1.6" stroke-dasharray="9,6"/>')
        for s in open_arrow(tuple(pts[-1]), u):
            glyphs.append({'t': 'line', 'p': s})
            svg.append(f'<line x1="{s[0][0]:.1f}" y1="{s[0][1]:.1f}" x2="{s[1][0]:.1f}" y2="{s[1][1]:.1f}" stroke="{STROKE}" stroke-width="1.6"/>')
    edges.append({'id': eid, 'from': a, 'to': b, 'points': pts, 'glyphs': glyphs, 'label': label})
    svg_edges.append(''.join(svg))


def link(eid, a, b, kind, label=None, p_a=None, p_b=None):
    """nối hai oval bằng đoạn thẳng giữa hai điểm trên biên (mặc định: hướng vào tâm nhau)"""
    ca, cb = E[a], E[b]
    pb = p_b or ell_toward(b, ca[0], ca[1])
    pa = p_a or ell_toward(a, pb[0], pb[1])
    add_edge(eid, a, b, [pa, pb], kind, label)


# ====================================================================================================
# Tác nhân
actor('KVL', 130, 700, 'Khách vãng lai')
actor('KH', 130, 1165, 'Khách hàng')
actor('QT', 2405, 860, 'Quản trị viên')

# ---- cột trái trên: Khách vãng lai ---------------------------------------------------------------
LX = 620
RXL = 168
for i, (k, y, t) in enumerate([('tk', 150, 'Tìm kiếm và lọc tour'), ('lh', 260, 'Gửi liên hệ'), ('dk', 370, 'Đăng ký tài khoản'),
                                ('gh', 520, 'Quản lý giỏ hàng')]):
    ell(k, LX, y, t, FS_MAIN, rx=RXL)
CRUDX = 1010
for k, y, t in [('gh_t', 425, 'Thêm'), ('gh_s', 520, 'Sửa'), ('gh_x', 615, 'Xóa')]:
    ell(k, CRUDX, y, t, FS_CRUD, rx=46, ry=22)

# ---- giữa: Đăng nhập / Đăng xuất --------------------------------------------------------------------
ell('dn', 1290, 860, 'Đăng nhập', 28, rx=150, ry=54)
ell('dx', 1230, 600, 'Đăng xuất', FS_MAIN, rx=118)

# ---- cột trái dưới: Khách hàng ---------------------------------------------------------------------
ell('mgg', LX, 865, 'Áp dụng mã giảm giá', FS_SMALL, rx=135, ry=26)
ell('dt', LX, 965, 'Đặt tour', FS_MAIN, rx=RXL)
ell('hd', LX, 1065, 'Huỷ đơn đặt tour', FS_SMALL, rx=135, ry=26)
ell('ls', LX, 1165, 'Xem lịch sử đơn đặt', FS_MAIN, rx=RXL)
ell('dg', LX, 1275, 'Đánh giá tour đã đi', FS_SMALL, rx=135, ry=26)
ell('hs', LX, 1385, 'Cập nhật hồ sơ cá nhân', FS_MAIN, rx=RXL)
ell('mk', LX, 1490, 'Đổi mật khẩu', FS_MAIN, rx=RXL)

# ---- cột phải: Quản trị viên -------------------------------------------------------------------------
AX = 1930
CR = 145
RXA = 172
ADMIN = [('q1', 205, 'Quản lý danh mục tour'), ('q2', 415, 'Quản lý tour'), ('q3', 625, 'Quản lý lịch khởi hành'),
         ('q4', 835, 'Quản lý đơn đặt tour'), ('q5', 1045, 'Quản lý tài khoản'), ('q6', 1255, 'Quản lý mã khuyến mãi'),
         ('tkdt', 1345, 'Xem thống kê doanh thu'), ('kd', 1432, 'Kiểm duyệt đánh giá'), ('dlh', 1519, 'Duyệt liên hệ')]
for k, y, t in ADMIN:
    ell(k, AX, y, t, FS_MAIN, rx=RXA)
    if k in ('tkdt', 'kd', 'dlh'):
        continue
    if k == 'q4':
        ell('q4_s', AX, y - 130, 'Sửa', FS_CRUD, rx=46, ry=22)
    else:
        for tag, dx, dy, t2 in (('t', -CR, 115, 'Thêm'), ('s', 0, 145, 'Sửa'), ('x', CR, 115, 'Xóa')):
            ell(f'{k}_{tag}', AX + dx, y - dy, t2, FS_CRUD, rx=46, ry=22)

# ---- khung và tiêu đề ----------------------------------------------------------------------------
title = 'Website đặt tour du lịch trực tuyến'
tw_ = tw(title, FS_TITLE, True)
tbox = [1281 - tw_ / 2 - 4, 40, 1281 + tw_ / 2 + 4, 40 + FS_TITLE + 10]
shapes.append({'id': 'tieu_de', 'kind': 'rect', 'bbox': tbox, 'inside': 'boundary'})
containers = [{'id': 'boundary', 'bbox': [BX0, BY0, BX1, BY1]}]

# ====================================================================================================
# Đường nối
# liên kết tác nhân - use case
hk = hand('KVL', 'R')
for k in ('tk', 'lh', 'dk', 'gh'):
    add_edge('lk_KVL_' + k, 'KVL', k, [hk, ell_pt(k, 180)], 'assoc')
add_edge('lk_KVL_dn', 'KVL', 'dn', [hk, ell_pt('dn', 170)], 'assoc')
hh = hand('KH', 'R')
for k in ('dt', 'ls', 'hs', 'mk'):
    add_edge('lk_KH_' + k, 'KH', k, [hh, ell_pt(k, 180)], 'assoc')
hq = hand('QT', 'L')
for k, _, _ in ADMIN:
    add_edge('lk_QT_' + k, 'QT', k, [hq, ell_pt(k, 0)], 'assoc')

# kế thừa
kh_top = (ACT['KH']['x'], ACT['KH']['fig'][1])
kvl_bot = (ACT['KVL']['x'], ACT['KVL']['lbl'][3])
add_edge('kt_KH_KVL', 'KH', 'KVL', [kh_top, kvl_bot], 'gen')
qt_bot = (ACT['QT']['x'], ACT['QT']['lbl'][3])
khl = (ACT['KH']['fig'][0], ACT['KH']['y'] - 14)
add_edge('kt_QT_KH', 'QT', 'KH', [qt_bot, (ACT['QT']['x'], 1618), (44, 1618), (44, khl[1]), khl], 'gen')

# «include» về Đăng nhập
for (k, phi) in (('dt', 196), ('ls', 214), ('hs', 232), ('mk', 252)):
    tgt = ell_pt('dn', phi)
    add_edge('inc_' + k, k, 'dn', [ell_pt(k, 0), tgt], 'dash', 'include')
phis = [72, 54, 36, 18, 0, -20, -42, -64, -86]
ELBOW = {'tkdt': 1560, 'kd': 1500, 'dlh': 1440}      # ba use case dưới cùng: đi ngang rồi mới chéo lên, để nhãn có chỗ
for (k, _, _), phi in zip(ADMIN, phis):
    tgt = ell_pt('dn', phi)
    tip = ell_pt(k, 180)
    pts = [tip, tgt] if k not in ELBOW else [tip, (ELBOW[k], tip[1]), tgt]
    add_edge('inc_' + k, k, 'dn', pts, 'dash', 'include')

# «extend»
for k in ('gh_t', 'gh_s', 'gh_x'):
    link('ext_' + k, k, 'gh', 'dash', 'extend')
link('ext_dx', 'dx', 'dn', 'dash', 'extend', p_b=ell_pt('dn', 112))
link('ext_mgg', 'mgg', 'dt', 'dash', 'extend')
link('ext_hd', 'hd', 'dt', 'dash', 'extend')
link('ext_dg', 'dg', 'ls', 'dash', 'extend')
for k, _, _ in ADMIN:
    if k in ('tkdt', 'kd', 'dlh'):
        continue
    if k == 'q4':
        link('ext_q4_s', 'q4_s', 'q4', 'dash', 'extend')
    else:
        for tag in 'tsx':
            link(f'ext_{k}_{tag}', f'{k}_{tag}', k, 'dash', 'extend')

# ====================================================================================================
# Nhãn «include» / «extend»: đặt cạnh đoạn thẳng, né mọi hình, đường, ký hiệu và nhãn khác
def seg_of(e):
    return e['points'][-2], e['points'][-1]


def glyph_rects():
    out = []
    for e in edges:
        for g in e['glyphs']:
            (x1, y1), (x2, y2) = g['p']
            out.append((min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2)))
    return out


def place_labels():
    placed = []
    labelled = [e for e in edges if e['label']]
    grects = glyph_rects()
    polys = [[tuple(p) for p in e['points']] for e in edges]
    # đường dài trước (ít lựa chọn hơn xếp trước ngược lại: đường ngắn trước)
    order = sorted(labelled, key=lambda e: sum(math.hypot(p[0] - q[0], p[1] - q[1]) for p, q in zip(e['points'], e['points'][1:])))
    res = {}

    def pdist(p, a, b):
        dx, dy = b[0] - a[0], b[1] - a[1]
        t = max(0, min(1, ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / (dx * dx + dy * dy)))
        return math.hypot(p[0] - a[0] - t * dx, p[1] - a[1] - t * dy)

    def ok(r, own):
        c = ((r[0] + r[2]) / 2, (r[1] + r[3]) / 2)
        di = {e['id']: min(pdist(c, a, b) for a, b in zip(e['points'], e['points'][1:])) for e in edges}
        if own.startswith('inc_') and any(v < di[own] + 1 for k, v in di.items() if k != own):
            return False
        if r[0] < 4 or r[1] < 4 or r[2] > W - 4 or r[3] > H - 4:
            return False
        for s in shapes:
            if sat_overlap(shape_poly(s, 4), [(r[0], r[1]), (r[2], r[1]), (r[2], r[3]), (r[0], r[3])]):
                return False
        for pts in polys:
            for a, b in zip(pts, pts[1:]):
                if seg_rect_hit(a, b, (r[0] - 3, r[1] - 3, r[2] + 3, r[3] + 3)):
                    return False
        for g in grects:
            if rect_overlap(r, g, 3):
                return False
        for q in placed:
            if rect_overlap(r, q, 4):
                return False
        return True

    for e in order:
        text = '«%s»' % e['label']
        w, h = tw(text, FS_LBL) + 6, FS_LBL + 6
        cands = []
        segs_ = list(zip(e['points'], e['points'][1:]))
        for si, ((x1, y1), (x2, y2)) in enumerate(segs_):
            L = math.hypot(x2 - x1, y2 - y1)
            if L < 40:
                continue
            ux, uy = (x2 - x1) / L, (y2 - y1) / L
            nx, ny = -uy, ux
            for t in (0.5, 0.42, 0.58, 0.34, 0.66, 0.26, 0.74, 0.18, 0.82, 0.1, 0.9, 0.05, 0.95):
                px, py = x1 + (x2 - x1) * t, y1 + (y2 - y1) * t
                for extra in (0, 10, 22):
                    for side in (1, -1):
                        off = (w / 2) * abs(nx) + (h / 2) * abs(ny) + 5 + extra
                        cx, cy = px + nx * side * off, py + ny * side * off
                        cands.append((abs(t - 0.5) * 100 + extra * 3 + (0 if side == 1 else 4) + (0 if si == len(segs_) - 1 else 30),
                                      (cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)))
        cands.sort(key=lambda c: c[0])
        for _, r in cands:
            if ok(r, e['id']):
                placed.append(r)
                res[e['id']] = {'bbox': list(r), 'text': text}
                break
        else:
            res[e['id']] = None
    return res


labels_placed = place_labels()
labels = [{'id': 'nhan_' + k, 'text': v['text'], 'bbox': v['bbox'], 'edge': k} for k, v in labels_placed.items() if v]
failed = ['nhan_' + k for k, v in labels_placed.items() if not v]

# ====================================================================================================
# SVG + PNG + JSON
out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
       f'<rect width="{W}" height="{H}" fill="white"/>',
       f'<rect x="{BX0}" y="{BY0}" width="{BX1-BX0}" height="{BY1-BY0}" fill="none" stroke="{STROKE}" stroke-width="1.8"/>',
       f'<text x="1281" y="{40 + FS_TITLE*0.9:.1f}" font-size="{FS_TITLE}" font-weight="bold" text-anchor="middle" font-family="{FAMILY}">{esc(title)}</text>']
out += svg_edges
out += svg_shapes
for lb in labels:
    x0, y0, x1, y1 = lb['bbox']
    out.append(f'<text x="{(x0+x1)/2:.1f}" y="{(y0+y1)/2 + FS_LBL*0.35:.1f}" font-size="{FS_LBL}" text-anchor="middle" '
               f'font-family="{FAMILY}">{esc(lb["text"])}</text>')
out.append('</svg>')
svg = '\n'.join(out)
open(os.path.join(HERE, 'UseCase_TongQuat.svg'), 'w', encoding='utf8').write(svg)
cairosvg.svg2png(bytestring=svg.encode('utf8'), write_to=os.path.join(HERE, '..', 'png', 'UseCase_TongQuat.png'), scale=1.25)
layout = {'name': 'UseCase_TongQuat', 'canvas': [W, H], 'margin': 6, 'shape_gap': 2.0, 'shapes': shapes, 'containers': containers,
          'edges': [{k: v for k, v in e.items() if k != 'label'} for e in edges], 'labels': labels, 'failed_labels': failed,
          'expected': {'crossings': 0}}
dump(os.path.join(HERE, 'UseCase_TongQuat.layout.json'), layout)
print('ok', len(shapes), 'hình', len(edges), 'đường', len(labels), 'nhãn; nhãn lỗi:', failed)
