# -*- coding: utf-8 -*-
"""Hình 3.4.1.1 - ERD của TourBookingDB (17 bảng): hộp màu cam, ký hiệu chân quạ (crow's foot), đường nối đứt,
nhãn tên khoá ngoại, kiểu dữ liệu và ký hiệu N cho cột cho phép NULL (giữ phong cách ERD cũ).
Dữ liệu lấy thẳng từ database/02_schema.sql (schema_model.py).
Bảng user_events không có khoá ngoại nên đứng riêng kèm ghi chú (không vẽ đường nối).
Chạy: python3 ERD.py  ->  ERD.svg, ERD.layout.json, ../png/ERD.png ; kiểm tra: python3 check_layout.py"""
import sys
from diagram_lib import *
import table_diagram

G, ROW, HDR = 11, 22, 33
FS, FS_HDR, FS_LBL = 14, 15, 12
FILL, BORDER, TEXT = '#f4b77a', '#644424', '#1d1d1d'
TYPE_COL = '#4a3418'


def size(t):
    nw = max(tw(c['name'], FS, c['pk']) for c in t['cols'])
    yw = max(tw(c['type'], FS) for c in t['cols'])
    w = 8 + 24 + nw + 12 + yw + 6 + 16 + 6
    w = max(w, tw(t['name'], FS_HDR, True) + 28)
    return int(math.ceil(w / G) * G), HDR + ROW * len(t['cols'])


def key_icon(x, y):
    # chìa khoá vàng: vòng tròn + thân + răng
    return (f'<circle cx="{x+4.5}" cy="{y+4.5}" r="3.8" fill="#f2c30f" stroke="#7a5b00" stroke-width="1"/>'
            f'<path d="M{x+7} {y+7} L{x+13} {y+13} M{x+10} {y+10} L{x+12} {y+8} M{x+12} {y+12} L{x+14} {y+10}" '
            f'stroke="#7a5b00" stroke-width="1.5" fill="none"/>')


def draw_table(t, x, y, w, h):
    o = [f'<g><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{FILL}" stroke="{BORDER}" stroke-width="1.6"/>',
         f'<text x="{x + w/2}" y="{y + HDR*0.68}" font-size="{FS_HDR}" font-weight="bold" text-anchor="middle" fill="{TEXT}" '
         f'font-family="{FAMILY}">{esc(t["name"])}</text>',
         f'<line x1="{x}" y1="{y+HDR-4}" x2="{x+w}" y2="{y+HDR-4}" stroke="{BORDER}" stroke-width="1.2"/>']
    nw = max(tw(c['name'], FS, c['pk']) for c in t['cols'])
    tx = x + 8 + 24 + nw + 12
    for i, c in enumerate(t['cols']):
        ry = y + HDR + i * ROW
        cy = ry + ROW / 2
        if c['pk']:
            o.append(key_icon(x + 8, cy - 7))
        elif c['fk']:
            o.append(f'<rect x="{x+6}" y="{cy-7}" width="19" height="14" rx="3" fill="#2d6cb5" stroke="#173f6f" stroke-width="1"/>'
                     f'<text x="{x+15.5}" y="{cy+3.6}" font-size="10" font-weight="bold" text-anchor="middle" fill="white" '
                     f'font-family="{FAMILY}">FK</text>')
        else:
            o.append(f'<rect x="{x+10}" y="{cy-6}" width="8" height="12" rx="1.5" fill="#5aa0e6" stroke="#245a9e" stroke-width="1"/>')
        o.append(f'<text x="{x+8+24}" y="{cy+5}" font-size="{FS}" {"font-weight=" + chr(34) + "bold" + chr(34) if c["pk"] else ""} '
                 f'fill="{TEXT}" font-family="{FAMILY}">{esc(c["name"])}</text>')
        o.append(f'<text x="{tx}" y="{cy+5}" font-size="{FS}" fill="{TYPE_COL}" font-family="{FAMILY}">{esc(c["type"])}</text>')
        if c['null']:
            o.append(f'<rect x="{x+w-6-16}" y="{cy-8}" width="16" height="16" fill="white" stroke="#222" stroke-width="1"/>'
                     f'<text x="{x+w-14}" y="{cy+4.5}" font-size="12" font-weight="bold" text-anchor="middle" fill="#111" '
                     f'font-family="{FAMILY}">N</text>')
    o.append('</g>')
    return ''.join(o)


def end_kind(side, fk):
    if side == 'parent':
        return 'zero_one' if fk['null'] else 'one'
    return 'zero_one' if fk['unique'] else 'zero_many'


def glyphs(kind, P, d):
    """vẽ ký hiệu tại đầu đường nối, P = điểm trên biên hộp, d = hướng ra ngoài"""
    n = (-d[1], d[0])

    def pt(a, b):
        return (P[0] + d[0] * a + n[0] * b, P[1] + d[1] * a + n[1] * b)

    svg, prims = [], []

    def line(a1, b1, a2, b2):
        p1, p2 = pt(a1, b1), pt(a2, b2)
        svg.append(f'<line x1="{p1[0]:.1f}" y1="{p1[1]:.1f}" x2="{p2[0]:.1f}" y2="{p2[1]:.1f}" stroke="#222" stroke-width="1.5"/>')
        prims.append({'t': 'line', 'p': [list(p1), list(p2)]})

    def circle(a, r):
        c = pt(a, 0)
        svg.append(f'<circle cx="{c[0]:.1f}" cy="{c[1]:.1f}" r="{r}" fill="white" stroke="#222" stroke-width="1.5"/>')
        prims.append({'t': 'circle', 'c': list(c), 'r': r})

    if kind == 'one':
        line(8, -7, 8, 7)
        line(14, -7, 14, 7)
    elif kind == 'zero_one':
        line(8, -7, 8, 7)
        circle(18, 5)
    elif kind == 'zero_many':
        line(14, 0, 0, -8)
        line(14, 0, 0, 0)
        line(14, 0, 0, 8)
        circle(20, 5)
    return ''.join(svg), prims


def edge_svg(pts):
    return ('<polyline points="%s" fill="none" stroke="#222" stroke-width="1.4" stroke-dasharray="7,5" '
            'stroke-linejoin="round"/>' % ' '.join(f'{x},{y}' for x, y in pts))


def label_svg(lines, bb, rot):
    x0, y0, x1, y1 = bb
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    n = len(lines)
    ts = ''.join(f'<tspan x="{cx}" dy="{0 if i == 0 else FS_LBL + 2}">{esc(l)}</tspan>' for i, l in enumerate(lines))
    y = cy + 4 - (n - 1) * (FS_LBL + 2) / 2
    tr = f' transform="rotate(-90 {cx} {cy})"' if rot else ''
    return (f'<text x="{cx}" y="{y}" font-size="{FS_LBL}" text-anchor="middle" fill="#222" font-family="{FAMILY}"{tr}>{ts}</text>')


STYLE = dict(G=G, margin=33, size=size, draw_table=draw_table, end_kind=end_kind, glyphs=glyphs, edge_svg=edge_svg,
             label_svg=label_svg, label_size=FS_LBL, labels=True, bg='white', check_margin=6)

# --- bố cục ---------------------------------------------------------------------------------------
COLUMNS = [
    [('tour_categories', 0), ('tours', 9), ('contact_messages', 9), ('password_reset_tokens', 6)],
    [('tour_images', 0), ('tour_itineraries', 6), ('tour_departures', 6), ('reviews', 6), ('users', 9)],
    [('booking_details', 0), ('bookings', 9), ('coupon_usages', 9), ('user_events', 12)],
    [('booking_passengers', 0), ('payments', 6), ('booking_status_history', 6), ('promotions', 6)],
]
GAPS = [G * 15, G * 13, G * 14]

NOTE_LINES = ['user_events KHÔNG có khoá ngoại (có chủ đích):',
              'user_id, tour_id, departure_id chỉ tham chiếu logic',
              'tới users, tours, tour_departures (không vẽ đường nối),',
              'để ghi sự kiện nhanh và không cản việc xoá tour hay',
              'tài khoản.']


def snap(v):
    return int(math.ceil(v / G) * G)


def extras(boxes, sizes, G):
    x4, y4, w4, h4 = boxes['promotions']
    out = []
    # ghi chú về user_events
    nw = max(tw(l, 13, i == 0) for i, l in enumerate(NOTE_LINES)) + 24
    nw = snap(max(nw, w4))
    nh = snap(24 + 18 * (len(NOTE_LINES) - 1) + 16)
    ny = y4 + h4 + 4 * G
    nx = x4
    svg = [f'<g><rect x="{nx}" y="{ny}" width="{nw}" height="{nh}" rx="6" fill="#fff6e8" stroke="{BORDER}" stroke-width="1.4" stroke-dasharray="6,4"/>']
    for i, l in enumerate(NOTE_LINES):
        svg.append(f'<text x="{nx+12}" y="{ny+24+i*18}" font-size="13" {"font-weight=" + chr(34) + "bold" + chr(34) if i == 0 else ""} fill="{TEXT}" font-family="{FAMILY}">{esc(l)}</text>')
    svg.append('</g>')
    out.append({'id': 'note_user_events', 'x': nx, 'y': ny, 'w': nw, 'h': nh, 'svg': ''.join(svg)})
    # chú thích ký hiệu (hai cột)
    ly = ny + nh + 3 * G
    items = [('pk', 'khoá chính (PK)'), ('fk', 'khoá ngoại (FK)'), ('null', 'cột cho phép NULL'),
             ('one', 'bắt buộc: đúng một'), ('zero_one', 'tuỳ chọn: 0 hoặc 1'), ('zero_many', '0 hoặc nhiều')]
    half = (len(items) + 1) // 2
    colw = max(tw(t, 13) for _, t in items) + 64
    lw, lh = snap(max(nw, 2 * colw + 12)), snap(30 + 24 * half + 8)
    sv = [f'<g><rect x="{nx}" y="{ly}" width="{lw}" height="{lh}" rx="6" fill="white" stroke="{BORDER}" stroke-width="1.4"/>',
          f'<text x="{nx+12}" y="{ly+21}" font-size="13" font-weight="bold" fill="{TEXT}" font-family="{FAMILY}">Chú thích</text>']
    for i, (k, t) in enumerate(items):
        cx0 = nx + (i // half) * (lw // 2)
        cy = ly + 40 + (i % half) * 24
        if k == 'pk':
            sv.append(key_icon(cx0 + 14, cy - 7))
        elif k == 'fk':
            sv.append(f'<rect x="{cx0+12}" y="{cy-7}" width="19" height="14" rx="3" fill="#2d6cb5" stroke="#173f6f"/>'
                      f'<text x="{cx0+21.5}" y="{cy+3.6}" font-size="10" font-weight="bold" text-anchor="middle" fill="white" font-family="{FAMILY}">FK</text>')
        elif k == 'null':
            sv.append(f'<rect x="{cx0+14}" y="{cy-8}" width="16" height="16" fill="white" stroke="#222"/>'
                      f'<text x="{cx0+22}" y="{cy+4.5}" font-size="12" font-weight="bold" text-anchor="middle" font-family="{FAMILY}">N</text>')
        else:
            g, _ = glyphs(k, (cx0 + 10, cy), (1, 0))
            sv.append(f'<line x1="{cx0+10}" y1="{cy}" x2="{cx0+46}" y2="{cy}" stroke="#222" stroke-width="1.4" stroke-dasharray="6,4"/>' + g)
        sv.append(f'<text x="{cx0+56}" y="{cy+5}" font-size="13" fill="{TEXT}" font-family="{FAMILY}">{esc(t)}</text>')
    sv.append('</g>')
    out.append({'id': 'legend', 'x': nx, 'y': ly, 'w': lw, 'h': lh, 'svg': ''.join(sv)})
    return out


if __name__ == '__main__':
    import sys
    run = table_diagram.run('ERD', STYLE, COLUMNS, GAPS, extras=extras, tries=int(sys.argv[1]) if len(sys.argv) > 1 else 30)
