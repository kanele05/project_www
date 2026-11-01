# -*- coding: utf-8 -*-
"""Hình 3.4.1.2 - Database Diagram của TourBookingDB (17 bảng) theo phong cách SQL Server Management Studio:
hộp bảng có tiêu đề xám, chỉ liệt kê tên cột (đúng như sơ đồ cũ), biểu tượng chìa khoá vàng cho khoá chính, đường nối
hai nét xám với chìa khoá vàng ở đầu bảng cha và biểu tượng vô cực ở đầu bảng con. Không có nhãn trên đường nối.
Dữ liệu lấy thẳng từ database/02_schema.sql (schema_model.py). user_events không có khoá ngoại nên đứng riêng, không có đường nối.
Chạy: python3 DatabaseDiagram.py -> DatabaseDiagram.svg, DatabaseDiagram.layout.json, ../png/DatabaseDiagram.png"""
import sys
from diagram_lib import *
import table_diagram

G, ROW, HDR = 12, 24, 36
FS, FS_HDR = 15, 17
HEAD, BORDER, TEXT = '#dee2e8', '#3d3d3d', '#111111'
KEY = '#ebc81e'


def size(t):
    nw = max(tw(c['name'], FS) for c in t['cols'])
    w = 8 + 24 + nw + 14
    w = max(w, tw(t['name'], FS_HDR, True) + 24)
    return int(math.ceil(w / G) * G), HDR + ROW * len(t['cols'])


def key_icon(x, y, color=KEY, edge='#8a6d00'):
    return (f'<circle cx="{x+5}" cy="{y+5}" r="4.2" fill="{color}" stroke="{edge}" stroke-width="1.1"/>'
            f'<path d="M{x+8} {y+8} L{x+15} {y+15} M{x+11.5} {y+11.5} L{x+14} {y+9.5} M{x+13.5} {y+13.5} L{x+16} {y+11.5}" '
            f'stroke="{edge}" stroke-width="1.7" fill="none"/>')


def draw_table(t, x, y, w, h):
    o = [f'<g><rect x="{x+4}" y="{y+4}" width="{w}" height="{h}" fill="#b4b8bf"/>',
         f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="white" stroke="{BORDER}" stroke-width="1.4"/>',
         f'<rect x="{x}" y="{y}" width="{w}" height="{HDR}" fill="{HEAD}" stroke="{BORDER}" stroke-width="1.4"/>',
         f'<text x="{x+9}" y="{y+HDR*0.68}" font-size="{FS_HDR}" font-weight="bold" fill="{TEXT}" font-family="{FAMILY}">{esc(t["name"])}</text>']
    for i, c in enumerate(t['cols']):
        ry = y + HDR + i * ROW
        cy = ry + ROW / 2
        if i:
            o.append(f'<line x1="{x+32}" y1="{ry}" x2="{x+w-4}" y2="{ry}" stroke="#d6d9de" stroke-width="1"/>')
        if c['pk']:
            o.append(key_icon(x + 7, cy - 8))
        elif c['fk']:
            o.append(key_icon(x + 7, cy - 8, '#c9d1dc', '#5b6b80'))
        o.append(f'<text x="{x+32}" y="{cy+5.5}" font-size="{FS}" fill="{TEXT}" font-family="{FAMILY}">{esc(c["name"])}</text>')
    o.append('</g>')
    return ''.join(o)


def end_kind(side, fk):
    return 'key' if side == 'parent' else 'inf'


def glyphs(kind, P, d):
    n = (-d[1], d[0])

    def pt(a, b):
        return (P[0] + d[0] * a + n[0] * b, P[1] + d[1] * a + n[1] * b)

    svg, prims = [], []

    def circle(a, r, fill, stroke, sw=1.6):
        c = pt(a, 0)
        svg.append(f'<circle cx="{c[0]:.1f}" cy="{c[1]:.1f}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')
        prims.append({'t': 'circle', 'c': list(c), 'r': r + 0.8})
        return c
    if kind == 'key':
        c = circle(15, 8, KEY, '#7a6400', 1.5)
        svg.append(f'<circle cx="{c[0]-2:.1f}" cy="{c[1]-2:.1f}" r="2.6" fill="#fff6b8" opacity="0.9"/>')
    else:
        circle(10, 4.6, 'white', '#555', 1.5)
        circle(20, 4.6, 'white', '#555', 1.5)
    return ''.join(svg), prims


def edge_svg(pts):
    p = ' '.join(f'{x},{y}' for x, y in pts)
    return (f'<polyline points="{p}" fill="none" stroke="#6d7480" stroke-width="5" stroke-linejoin="miter"/>'
            f'<polyline points="{p}" fill="none" stroke="white" stroke-width="2" stroke-linejoin="miter"/>')


STYLE = dict(G=G, margin=36, size=size, draw_table=draw_table, end_kind=end_kind, glyphs=glyphs, edge_svg=edge_svg,
             labels=False, bg='white', check_margin=6, label_size=12, label_svg=None)

COLUMNS = [
    [('tour_categories', 0), ('tours', 7), ('contact_messages', 7), ('password_reset_tokens', 5)],
    [('tour_images', 0), ('tour_itineraries', 5), ('tour_departures', 5), ('reviews', 5), ('users', 7)],
    [('booking_details', 0), ('bookings', 7), ('coupon_usages', 7), ('user_events', 9)],
    [('booking_passengers', 0), ('payments', 5), ('booking_status_history', 5), ('promotions', 5)],
]
GAPS = [G * 10, G * 10, G * 10]


def snap(v):
    return int(math.ceil(v / G) * G)


def extras(boxes, sizes, G):
    x4, y4, w4, h4 = boxes['promotions']
    items = [('pk', 'khoá chính'), ('fk', 'cột là khoá ngoại'), ('rel', 'quan hệ: chìa khoá = bảng cha, ∞ = bảng con'),
             ('note', 'user_events: không có khoá ngoại, không có đường nối')]
    lw = snap(max(tw(t, 13) for _, t in items) + 44)
    lh = snap(34 + 22 * len(items) + 4)
    lx, ly = x4, y4 + h4 + 4 * G
    sv = [f'<g><rect x="{lx}" y="{ly}" width="{lw}" height="{lh}" fill="white" stroke="{BORDER}" stroke-width="1.2"/>',
          f'<text x="{lx+10}" y="{ly+22}" font-size="14" font-weight="bold" fill="{TEXT}" font-family="{FAMILY}">Chú thích</text>']
    for i, (k, t) in enumerate(items):
        cy = ly + 42 + i * 22
        if k == 'pk':
            sv.append(key_icon(lx + 10, cy - 8))
        elif k == 'fk':
            sv.append(key_icon(lx + 10, cy - 8, '#c9d1dc', '#5b6b80'))
        sv.append(f'<text x="{lx+(10 if k in ("rel", "note") else 34)}" y="{cy+5}" font-size="13" fill="{TEXT}" font-family="{FAMILY}">{esc(t)}</text>')
    sv.append('</g>')
    return [{'id': 'legend', 'x': lx, 'y': ly, 'w': lw, 'h': lh, 'svg': ''.join(sv)}]


if __name__ == '__main__':
    table_diagram.run('DatabaseDiagram', STYLE, COLUMNS, GAPS, extras=extras, tries=int(sys.argv[1]) if len(sys.argv) > 1 else 30)
