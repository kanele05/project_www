# -*- coding: utf-8 -*-
"""Quy trình chung cho ERD và Database Diagram: đặt hộp bảng theo cột, định tuyến khoá ngoại bằng đường vuông góc
không cắt nhau, đặt nhãn, vẽ SVG/PNG và ghi bố cục ra JSON cho check_layout.py."""
import math, os
import cairosvg
import schema_model
from diagram_lib import *


def layout_columns(sizes, columns, gaps, margin, G):
    """columns: list các cột; mỗi mục (tên, khoảng_cách_ô) xếp chồng, hoặc ('@tên', y_ô) đặt tuyệt đối."""
    pos = {}
    x = margin
    col_x = []
    for ci, col in enumerate(columns):
        names = [n.lstrip('@') for n, _ in col]
        cw = max(sizes[n][0] for n in names)
        col_x.append((x, cw))
        y = margin
        for n, v in col:
            if n.startswith('@'):
                y = v * G
                n = n[1:]
            else:
                y += v * G
            pos[n] = (x, y)
            y += sizes[n][1]
        x += cw + (gaps[ci] if ci < len(gaps) else 0)
    return pos, col_x


def run(name, style, columns, gaps, extras=None, tries=40, seed=1, log=print, hints=None, out_dir=None,
        png_scale=1.6, min_size=(0, 0)):
    """style: dict(G, margin, size(table)->(w,h), draw_table(table,x,y,w,h)->svg, glyphs(kind,P,d)->(svg,prims),
    edge_svg(points)->svg, label_size, label_svg(text,bbox,rot)->svg, bg, defs)
    extras: list dict(id, x, y, w, h, svg) (hộp thêm, toạ độ px bội của G)"""
    G = style['G']
    T = schema_model.load()
    sizes = {n: style['size'](t) for n, t in T.items()}
    pos, col_x = layout_columns(sizes, columns, gaps, style['margin'], G)
    boxes = {n: (pos[n][0], pos[n][1], sizes[n][0], sizes[n][1]) for n in pos}
    extras = extras(boxes, sizes, G) if callable(extras) else (extras or [])
    for e in extras:
        boxes[e['id']] = (e['x'], e['y'], e['w'], e['h'])
    W = max(b[0] + b[2] for b in boxes.values()) + style['margin']
    H = max(b[1] + b[3] for b in boxes.values()) + style['margin']
    W = max(W, min_size[0])
    H = max(H, min_size[1])
    W, H = int(math.ceil(W / G) * G), int(math.ceil(H / G) * G)
    R = Router(W, H, G, boxes, stub=style.get('stub', 3), bend=style.get('bend', 7), cross=400)
    edges = []
    for n, t in T.items():
        for fk in t['fks']:
            edges.append({'id': fk['name'], 'a': n, 'b': fk['ref'], 'fk': fk})
    res = route_all(R, edges, tries=tries, seed=seed, log=log, hints=hints)
    if res is None:
        log('KHÔNG ĐỊNH TUYẾN ĐƯỢC')
        return None
    routes, info = res
    ncross = count_cross(routes)
    log('Số lần cắt sau định tuyến:', ncross)
    polys = {}
    svg_edges, glyph_prims, glyph_svg, glyph_rects = [], {}, [], []
    for e in edges:
        sa, pa, sb, pb = info[e['id']]
        pts = simplify(R.px(routes[e['id']]))
        polys[e['id']] = pts
    # glyph
    for e in edges:
        pts = polys[e['id']]
        fk = e['fk']
        prims = []
        for end, kind_side in ((0, 'child'), (1, 'parent')):
            P = pts[0] if end == 0 else pts[-1]
            Q = pts[1] if end == 0 else pts[-2]
            d = ((Q[0] - P[0]) // max(1, abs(Q[0] - P[0])) if Q[0] != P[0] else 0,
                 (Q[1] - P[1]) // max(1, abs(Q[1] - P[1])) if Q[1] != P[1] else 0)
            kind = style['end_kind'](kind_side, fk)
            s, pr = style['glyphs'](kind, P, d)
            glyph_svg.append(s)
            prims += pr
        glyph_prims[e['id']] = prims
        for p in prims:
            if p['t'] == 'circle':
                glyph_rects.append((p['c'][0] - p['r'], p['c'][1] - p['r'], p['c'][0] + p['r'], p['c'][1] + p['r']))
            else:
                (x1, y1), (x2, y2) = p['p']
                glyph_rects.append((min(x1, x2) - 0.5, min(y1, y2) - 0.5, max(x1, x2) + 0.5, max(y1, y2) + 0.5))
    # nhãn
    labels, placed = [], {}
    if style.get('labels'):
        labels = [{'id': 'lbl_' + e['id'], 'text': e['id'], 'edge': e['id']} for e in edges]
        brects = [(b[0], b[1], b[0] + b[2], b[1] + b[3]) for b in boxes.values()]
        # glyph rects của chính các đường nối chưa chắc cần né ở khe 1px; né với 1px
        placed = place_labels(labels, polys, brects, glyph_rects, W, H, style['label_size'])
        bad = [k for k, v in placed.items() if v is None]
        log('Nhãn không đặt được:', bad)
    # SVG
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
           style.get('defs', ''), f'<rect width="{W}" height="{H}" fill="{style["bg"]}"/>']
    for e in edges:
        out.append(style['edge_svg'](polys[e['id']]))
    out += glyph_svg
    for n in pos:
        b = boxes[n]
        out.append(style['draw_table'](T[n], b[0], b[1], b[2], b[3]))
    for e in extras:
        out.append(e['svg'])
    for k, v in placed.items():
        if v:
            out.append(style['label_svg'](v['lines'], v['bbox'], v['rot']))
    out.append('</svg>')
    svg = '\n'.join(out)
    out_dir = out_dir or os.path.dirname(os.path.abspath(__file__))
    open(os.path.join(out_dir, name + '.svg'), 'w', encoding='utf8').write(svg)
    png = os.path.join(out_dir, '..', 'png', name + '.png')
    cairosvg.svg2png(bytestring=svg.encode('utf8'), write_to=png, scale=png_scale)
    layout = {
        'name': name, 'canvas': [W, H], 'margin': style.get('check_margin', 6),
        'shapes': [{'id': n, 'kind': 'rect', 'bbox': [b[0], b[1], b[0] + b[2], b[1] + b[3]]} for n, b in boxes.items()],
        'edges': [{'id': e['id'], 'from': e['a'], 'to': e['b'], 'points': polys[e['id']], 'glyphs': glyph_prims[e['id']]}
                  for e in edges],
        'labels': [{'id': k, 'text': v['text'], 'bbox': list(v['bbox']), 'edge': v['edge']} for k, v in placed.items() if v],
        'failed_labels': [k for k, v in placed.items() if not v],
        'expected': {'crossings': 0},
    }
    dump(os.path.join(out_dir, name + '.layout.json'), layout)
    log('PNG:', png, 'canvas', W, 'x', H)
    return layout
