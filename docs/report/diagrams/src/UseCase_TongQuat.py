# -*- coding: utf-8 -*-
"""Vẽ biểu đồ use case tổng quát (Hình 2.2) bố cục theo bản gốc của người dùng.
Chạy: python3 UseCase_TongQuat.py  ->  UseCase_TongQuat.svg + ../png/UseCase_TongQuat.png"""
import math, os, cairosvg
FILL='#7FC8F5'; FONT='Liberation Sans, Arial, sans-serif'
W,H=2000,1380
els=[]
def esc(t): return t.replace('&','&amp;').replace('<','&lt;')
E={}   # tên -> (cx,cy,rx,ry)
def ell(name,cx,cy,text,rx=104,ry=21,fs=14):
    rx=int(rx*1.2); ry=int(ry*1.15); fs=int(fs*1.4)
    E[name]=(cx,cy,rx,ry)
    els.append(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{FILL}" stroke="#222" stroke-width="1.2"/>')
    lines=text.split('\n'); y0=cy-(len(lines)-1)*10+7
    for i,l in enumerate(lines):
        els.append(f'<text x="{cx}" y="{y0+i*20}" font-size="{fs}" text-anchor="middle" font-family="{FONT}">{esc(l)}</text>')
def edge_pt(name,tx,ty):
    cx,cy,rx,ry=E[name]; dx,dy=tx-cx,ty-cy
    k=1/math.sqrt((dx/rx)**2+(dy/ry)**2) if (dx or dy) else 1
    return cx+dx*k, cy+dy*k
def line(x1,y1,x2,y2,dash=False,arrow=None):
    st=' stroke-dasharray="7,5"' if dash else ''
    mk=' marker-end="url(#open)"' if arrow=='open' else (' marker-end="url(#tri)"' if arrow=='tri' else '')
    els.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="#222" stroke-width="1.2"{st}{mk}/>')
def poly(pts,arrow=None):
    mk=' marker-end="url(#tri)"' if arrow=='tri' else ''
    els.append('<polyline points="%s" fill="none" stroke="#222" stroke-width="1.2"%s/>'%(' '.join(f'{x},{y}' for x,y in pts),mk))
def label(x,y,t,fs=12):
    fs=int(fs*1.45); els.append(f'<text x="{x:.1f}" y="{y:.1f}" font-size="{fs}" text-anchor="middle" font-family="{FONT}">{esc(t)}</text>')
def rel(a,b,kind,dx=0,dy=-4):
    """mũi tên đứt a -> b có nhãn «include»/«extend»"""
    ax,ay,_,_=E[a]; bx,by,_,_=E[b]
    x1,y1=edge_pt(a,bx,by); x2,y2=edge_pt(b,ax,ay)
    line(x1,y1,x2,y2,dash=True,arrow='open')
    label((x1+x2)/2+dx,(y1+y2)/2+dy,'«%s»'%kind)
def actor(x,y,name):
    els.append(f'<circle cx="{x}" cy="{y-30}" r="11" fill="{FILL}" stroke="#222" stroke-width="1.3"/>')
    line(x,y-19,x,y+12); line(x-20,y-9,x+20,y-9); line(x,y+12,x-15,y+38); line(x,y+12,x+15,y+38)
    label(x,y+60,name,fs=13)
els.append(f'<rect width="{W}" height="{H}" fill="white"/>')
els.append('''<defs><marker id="open" markerWidth="12" markerHeight="10" refX="11" refY="5" orient="auto"><path d="M1,1 L11,5 L1,9" fill="none" stroke="#222" stroke-width="1.2"/></marker>
<marker id="tri" markerWidth="16" markerHeight="14" refX="15" refY="7" orient="auto"><path d="M1,1 L15,7 L1,13 Z" fill="white" stroke="#222" stroke-width="1.2"/></marker></defs>''')
# ranh giới hệ thống
els.append('<rect x="182" y="38" width="1590" height="1222" fill="none" stroke="#222" stroke-width="1.3"/>')
label(977,62,'Website đặt tour du lịch trực tuyến',fs=14); els[-1]=els[-1].replace('<text','<text font-weight="bold"')
# --- cột trái
for n,y,t in [('tk',82,'Tìm kiếm và lọc tour'),('lh',164,'Gửi liên hệ'),('dk',246,'Đăng ký tài khoản'),('gh',340,'Quản lý giỏ hàng'),
              ('dt',657,'Đặt tour'),('ls',767,'Xem lịch sử đơn đặt'),('hs',877,'Cập nhật hồ sơ cá nhân'),('mk',987,'Đổi mật khẩu')]:
    ell(n,548,y,t)
ell('gh1',822,285,'Thêm',rx=36,ry=15,fs=12); ell('gh2',822,340,'Sửa',rx=36,ry=15,fs=12); ell('gh3',822,394,'Xóa',rx=36,ry=15,fs=12)
ell('dx',986,416,'Đăng xuất',rx=82,ry=22); ell('dn',986,548,'Đăng nhập',rx=94,ry=22)
ell('mgg',274,631,'Áp dụng mã\ngiảm giá',rx=82,ry=19,fs=12); ell('hd',274,712,'Huỷ đơn\nđặt tour',rx=82,ry=19,fs=12); ell('dg',274,794,'Đánh giá\ntour đã đi',rx=82,ry=19,fs=12)
# --- cột phải
Q=[('q1',186,'Quản lý danh mục tour'),('q2',326,'Quản lý tour'),('q3',465,'Quản lý lịch khởi hành'),('q4',605,'Quản lý đơn đặt tour'),('q5',745,'Quản lý tài khoản'),('q6',885,'Quản lý mã khuyến mãi')]
for n,y,t in Q: ell(n,1590,y,t,rx=110)
ell('tkdt',1590,1003,'Xem thống kê doanh thu',rx=110); ell('kd',1590,1074,'Kiểm duyệt đánh giá',rx=110); ell('dlh',1590,1145,'Duyệt liên hệ',rx=110)
for n,y,_ in Q:
    if n=='q4':
        ell(n+'s',1589,y-76,'Sửa',rx=33,ry=15,fs=12)
    else:
        for i,(k,x) in enumerate([('t',1496),('s',1589),('x',1682)]):
            ell(n+k,x,y-77,['Thêm','Sửa','Xóa'][i],rx=33,ry=15,fs=12)
# --- tác nhân
actor(70,265,'Khách vãng lai'); actor(70,1040,'Khách hàng'); actor(1862,650,'Quản trị viên')
# liên kết tác nhân - use case
for n in ['tk','lh','dk','gh','dn']:
    x2,y2=edge_pt(n,95,262); line(92,263,x2,y2)
for n,off in [('dt',109),('ls',131),('hs',153),('mk',176)]:
    cx,cy,rx,ry=E[n]; poly([(92,1031),(off,1031),(off,cy),(cx-rx,cy)])
line(70,1000,70,336,arrow='tri')                      # Khách hàng kế thừa Khách vãng lai
poly([(1862,745),(1862,1315),(28,1315),(28,1031),(46,1031)],arrow='tri')   # Quản trị viên kế thừa Khách hàng
for n,_,_ in Q+[('tkdt',0,0),('kd',0,0),('dlh',0,0)]:
    cx,cy,rx,ry=E[n]; line(1845,645,cx+rx,cy)
# include / extend
for n in ['dt','ls','hs','mk']: rel(n,'dn','include')
for n,_,_ in Q+[('tkdt',0,0),('kd',0,0),('dlh',0,0)]: rel(n,'dn','include')
for k in ['gh1','gh2','gh3']: rel(k,'gh','extend')
rel('dx','dn','extend'); rel('mgg','dt','extend'); rel('hd','dt','extend'); rel('dg','ls','extend')
for n,_,_ in Q:
    if n=='q4': rel('q4s','q4','extend'); continue
    for k,dx in (('t',-30),('s',0),('x',30)): rel(n+k,n,'extend',dx=dx,dy=2 if k!='s' else -2)
svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'+'\n'.join(els)+'</svg>'
here=os.path.dirname(os.path.abspath(__file__))
open(os.path.join(here,'UseCase_TongQuat.svg'),'w',encoding='utf8').write(svg)
cairosvg.svg2png(bytestring=svg.encode('utf8'),write_to=os.path.join(here,'..','png','UseCase_TongQuat.png'),scale=1.0)
print('ok')
