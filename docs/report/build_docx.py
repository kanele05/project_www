# -*- coding: utf-8 -*-
"""
Dựng BaoCao_TourBooking.docx từ BAO_CAO.md.

Kiến trúc: BAO_CAO.md là bản thảo nguồn (Markdown + một DSL nhỏ tự định nghĩa cho
bảng đặc tả use case / đặc tả thực thể, vì các bảng đó có ô gộp mà cú pháp bảng
Markdown chuẩn không biểu diễn gọn được). Script này đọc BAO_CAO.md và sinh ra
BaoCao_TourBooking.docx cạnh nó. Chạy lại bất cứ lúc nào sau khi sửa BAO_CAO.md
hoặc sau khi có thêm ảnh trong docs/uml/png/ hoặc docs/screenshots/.

Cú pháp nhận diện ở MỨC KHỐI:
- "# " / "## " / "### " / "#### "   -> heading cấp 1..4 (Heading 1..4 của Word).
                                        Heading cấp 1 dạng "CHƯƠNG N: ..." mở một
                                        chương mới -> số Hình/Bảng reset về N.1.
- "[[HINH: <ten>]]"                  -> chèn ảnh (tìm ở docs/uml/png/ rồi tới
                                          docs/screenshots/), kèm chú thích
                                          "Hình <chuong>.<so>: <ten>". Có thì chèn
                                          ảnh, chưa có thì chèn placeholder rõ ràng
                                          + cảnh báo ra stdout.
- Khối "| ... | ... |" liên tiếp     -> bảng Markdown chuẩn (hàng đầu làm tiêu đề,
                                          hàng "|---|---|" nếu có thì bỏ qua), có
                                          chú thích "Bảng <chuong>.<so>: <tieu de>"
                                          lấy theo heading gần nhất phía trên.
- Khối ">>USECASE ... >>END"         -> bảng đặc tả use case đúng khuôn file mẫu
- Khối ">>ENTITY ... >>END"          -> bảng đặc tả thực thể (STT|Công việc|Kiểu dữ
                                          liệu|Ràng buộc|Ghi chú)
- Dòng "- " hoặc "* "                -> đoạn danh sách dấu đầu dòng (List Bullet)
- Dòng "1. ", "2. "...                -> đoạn danh sách đánh số (List Number)
- Dòng còn lại không rỗng            -> đoạn văn thường, căn đều, thụt đầu dòng

Cú pháp nhận diện ở MỨC NỘI DÒNG (áp dụng cho MỌI đoạn văn, MỌI ô bảng, kể cả
trong hai loại bảng DSL ở trên):
- "**đậm**"   -> run in đậm, KHÔNG còn dấu ** nào sót lại trong văn bản
- "`mã`"      -> run phông Consolas (đánh dấu là mã/định danh kỹ thuật), KHÔNG còn
                 dấu ` nào sót lại
- "*nghiêng*" -> run in nghiêng

Chạy: python docs/report/build_docx.py

Tự kiểm bắt buộc sau khi build (xem hàm self_check()): trích ngược
word/document.xml của file .docx vừa sinh, đếm số dấu "**" và dấu "`" còn sót -
CẢ HAI PHẢI BẰNG 0. Nếu khác 0, in ra dòng chứa lỗi để dễ dò ngược lại BAO_CAO.md.
"""
import re
import sys
import zipfile
from pathlib import Path

# Console Windows mac dinh dung cp1252/cp437, khong bieu dien duoc tieng Viet.
# Ep stdout/stderr sang UTF-8 de moi dong "in canh bao" tieng Viet khong lam sap script.
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.shared import Pt, Cm, RGBColor
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

HERE = Path(__file__).resolve().parent
MD_PATH = HERE / "BAO_CAO.md"
OUT_PATH = HERE / "BaoCao_TourBooking.docx"
PNG_DIR = HERE.parent / "uml" / "png"
# Anh chup man hinh phai nam RIENG, khong duoc de chung voi PNG cua UML:
# docs/uml/tools/build.sh xoa sach docs/uml/png/ truoc khi xuat lai bieu do
# (vi ExportDiagramImage khong ghi de ma them so 2 vao ten file). De chung
# thi moi lan dung lai UML la mat sach anh chup - da xay ra that, mat 4/6 anh.
SHOT_DIR = HERE.parent / "screenshots"


def find_image(name):
    """Tim anh theo ten o ca hai thu muc; tra ve Path dau tien ton tai."""
    for d in (PNG_DIR, SHOT_DIR):
        p = d / f"{name}.png"
        if p.exists():
            return p
    return PNG_DIR / f"{name}.png"  # duong dan dung de bao loi cho de doc


FONT_NAME = "Times New Roman"
CODE_FONT_NAME = "Consolas"
FONT_SIZE = Pt(13)

# ---------------------------------------------------------------------------
# Bộ đếm để in báo cáo nghiệm thu ở cuối
# ---------------------------------------------------------------------------
counters = {
    "headings": 0,
    "tables": 0,
    "usecases": 0,
    "entities": 0,
    "images_found": 0,
    "images_missing": 0,
    "diagram_refs": 0,
}
missing_images = []

# Số Hình/Bảng đánh theo chương hiện hành: "Hình 2.3", "Bảng 3.1"...
numbering = {
    "chapter": 0,
    "fig": 0,
    "tab": 0,
    "last_heading": "",
    "heading_tab_count": 0,
}

CHAPTER_RE = re.compile(r'^CHƯƠNG\s+(\d+)', re.IGNORECASE)  # "CHƯƠNG N"


# ---------------------------------------------------------------------------
# Phân tích markdown NỘI DÒNG: **đậm** / `mã` / *nghiêng*
# ---------------------------------------------------------------------------

INLINE_RE = re.compile(r'\*\*(.+?)\*\*|`([^`]+?)`|\*(.+?)\*')


def set_run_font(run, bold=False, italic=False, size=None, color=None, font_name=None):
    name = font_name or FONT_NAME
    run.font.name = name
    run.font.size = size or FONT_SIZE
    run.bold = bold
    run.italic = italic
    if color:
        run.font.color.rgb = color
    # Đảm bảo phông cho cả ký tự Đông Á (Word đôi khi tách riêng rFonts eastAsia)
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rPr.append(rFonts)
    rFonts.set(qn('w:eastAsia'), name)


def add_inline_runs(paragraph, text, bold=False, italic=False, size=None, color=None):
    """Thêm nội dung `text` vào `paragraph` dưới dạng nhiều run, diễn giải
    **đậm**, `mã`, *nghiêng* thành định dạng thật thay vì để lọt ký tự markdown
    nguyên văn ra tài liệu Word.

    ĐỆ QUY có chủ đích: khi gặp **đậm** hoặc *nghiêng*, nội dung bên trong được
    đưa lại chính hàm này (kèm cờ bold/italic đã cộng dồn) thay vì chèn thẳng
    một run duy nhất - nếu không, một cụm lồng nhau kiểu **`approved = false`**
    (đậm bọc quanh một đoạn mã) sẽ giữ nguyên cặp dấu `` ` `` bên trong làm ký
    tự thô, đúng lỗi đã bị bắt ở lần tự kiểm đầu tiên."""
    if text is None:
        return
    pos = 0
    for m in INLINE_RE.finditer(text):
        if m.start() > pos:
            chunk = text[pos:m.start()]
            if chunk:
                r = paragraph.add_run(chunk)
                set_run_font(r, bold=bold, italic=italic, size=size, color=color)
        if m.group(1) is not None:  # **đậm**, có thể chứa `mã` hoặc *nghiêng* lồng bên trong
            add_inline_runs(paragraph, m.group(1), bold=True, italic=italic, size=size, color=color)
        elif m.group(2) is not None:  # `mã` - không đệ quy tiếp, nội dung mã giữ nguyên văn
            r = paragraph.add_run(m.group(2))
            set_run_font(r, bold=bold, italic=italic, size=size or Pt(11.5),
                         color=color, font_name=CODE_FONT_NAME)
        elif m.group(3) is not None:  # *nghiêng*, có thể chứa `mã` lồng bên trong
            add_inline_runs(paragraph, m.group(3), bold=bold, italic=True, size=size, color=color)
        pos = m.end()
    if pos < len(text):
        chunk = text[pos:]
        if chunk:
            r = paragraph.add_run(chunk)
            set_run_font(r, bold=bold, italic=italic, size=size, color=color)


# ---------------------------------------------------------------------------
# Tiện ích định dạng chung
# ---------------------------------------------------------------------------

def add_paragraph(doc, text, bold=False, italic=False, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
                   space_after=6, line_spacing=1.5, indent=False, style=None):
    p = doc.add_paragraph(style=style) if style else doc.add_paragraph()
    p.alignment = align
    pf = p.paragraph_format
    pf.line_spacing = line_spacing
    pf.space_after = Pt(space_after)
    if indent:
        pf.first_line_indent = Cm(1)
    add_inline_runs(p, text, bold=bold, italic=italic)
    return p


def add_list_paragraph(doc, text, numbered=False):
    style = "List Number" if numbered else "List Bullet"
    p = doc.add_paragraph(style=style)
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(4)
    add_inline_runs(p, text)
    return p


def add_heading(doc, text, level):
    # level 1..4 -> Heading 1..4. Dùng style có sẵn của Word rồi tự áp lại phông
    # để tiêu đề vẫn dùng Times New Roman đồng bộ với toàn bài thay vì phông
    # mặc định (Calibri Light) của theme.
    style_name = f"Heading {min(level, 4)}"
    p = doc.add_paragraph(style=style_name)
    sizes = {1: Pt(16), 2: Pt(14), 3: Pt(13.5), 4: Pt(13)}
    add_inline_runs(p, text, bold=True, size=sizes.get(level, Pt(13)))
    for run in p.runs:
        run.font.color.rgb = RGBColor(0x0A, 0x38, 0x33)
    if level == 1:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.page_break_before = True
        m = CHAPTER_RE.match(text.strip())
        if m:
            numbering["chapter"] = int(m.group(1))
        numbering["fig"] = 0
        numbering["tab"] = 0
    numbering["last_heading"] = text.strip()
    numbering["heading_tab_count"] = 0
    counters["headings"] += 1
    return p


def style_table(table):
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for row in table.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(2)
                p.paragraph_format.line_spacing = 1.15
                for run in p.runs:
                    set_run_font(run)


def set_repeat_header(row):
    """Đánh dấu một hàng là tiêu đề lặp lại khi bảng sang trang mới (w:tblHeader)."""
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement('w:tblHeader')
    el.set(qn('w:val'), 'true')
    trPr.append(el)


def set_cell_text(cell, text, bold=False, align=None, size=None):
    cell.text = ""
    p = cell.paragraphs[0]
    if align is not None:
        p.alignment = align
    add_inline_runs(p, text, bold=bold, size=size)


def shade_cell(cell, hex_color="DDEFE9"):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), hex_color)
    tcPr.append(shd)


def add_table_caption(doc, title, before=True):
    """Chú thích 'Bảng x.y: <tên>'. Theo quy ước báo cáo học thuật tiếng Việt,
    chú thích bảng đặt PHÍA TRÊN bảng (khác với chú thích hình đặt phía dưới)."""
    numbering["tab"] += 1
    label = f"Bảng {numbering['chapter']}.{numbering['tab']}"
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_before = Pt(8) if before else Pt(2)
    cap.paragraph_format.space_after = Pt(4) if before else Pt(8)
    run = cap.add_run(f"{label}: {title}")
    set_run_font(run, bold=True, size=Pt(12.5))
    return label


# ---------------------------------------------------------------------------
# Ảnh
# ---------------------------------------------------------------------------

# Tên file dùng làm khoá tra cứu trong docs/uml/png hoặc docs/screenshots thường
# mang quy ước đặt tên nội bộ (tiền tố "01.", "AC-UC007.", "SQ-UC007.", hoặc một
# slug không dấu như "so-do-phan-cap"). Chú thích in ra báo cáo TUYỆT ĐỐI không
# được lộ quy ước này - phải là câu tiếng Việt tử tế. Hai cách cung cấp chú thích:
#   1) Tường minh trong thẻ nguồn: [[HINH: ten-file | Câu chú thích]]
#   2) Tự suy ra từ tên file cho đúng hai mẫu "AC-UCxxx. Biểu đồ hoạt động - ..."
#      và "SQ-UCxxx. Biểu đồ tuần tự - ..." (dùng trong khối DIAGRAMS của use case,
#      nơi tên file được liệt kê thẳng chứ không đi qua thẻ [[HINH:).
AC_UC_RE = re.compile(r'^AC-UC\d+\.\s*Biểu đồ hoạt động\s*-\s*(.+)$')
SQ_UC_RE = re.compile(r'^SQ-UC\d+\.\s*Biểu đồ tuần tự\s*-\s*(.+)$')
NUM_PREFIX_RE = re.compile(r'^\d+[a-z]?\.\s*(.+)$')


def _fix_slash_placeholder(text):
    """Tên file trên Windows không được chứa dấu '/', nên lúc xuất ảnh công cụ
    UML đã thay '/' bằng '_' (ví dụ "Đăng nhập / Đăng xuất" -> "Đăng nhập _ Đăng
    xuất" trong tên file). Đổi lại đúng dấu gạch chéo cho chú thích hiển thị."""
    return re.sub(r'(?<=\s)_(?=\s)', '/', text)


def derive_caption(name):
    """Suy ra chú thích tiếng Việt tử tế từ tên file, bỏ mọi tiền tố quy ước
    (AC-UCxxx., SQ-UCxxx., 01., 02b., ...). Chỉ dùng khi thẻ nguồn không tự
    cung cấp chú thích tường minh qua cú pháp `| Chú thích`."""
    m = AC_UC_RE.match(name)
    if m:
        return f"Biểu đồ hoạt động: {_fix_slash_placeholder(m.group(1))}"
    m = SQ_UC_RE.match(name)
    if m:
        return f"Biểu đồ tuần tự: {_fix_slash_placeholder(m.group(1))}"
    m = NUM_PREFIX_RE.match(name)
    if m:
        return _fix_slash_placeholder(m.group(1))
    # Không khớp mẫu nào đã biết (ví dụ một slug trần trụi) - đây là lỗi soạn
    # thảo: BAO_CAO.md phải cung cấp chú thích tường minh cho trường hợp này.
    print(f"[CANH BAO] Khong suy ra duoc chu thich tieng Viet cho '{name}' - "
          f"hay them '| Chu thich' vao the [[HINH: ... ]] tuong ung.")
    return name


def insert_image(doc, name, caption=None):
    counters["diagram_refs"] += 1
    path = find_image(name)
    numbering["fig"] += 1
    label = f"Hình {numbering['chapter']}.{numbering['fig']}"
    caption_text = caption if caption else derive_caption(name)
    if path.exists():
        counters["images_found"] += 1
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = p.add_run()
        try:
            run.add_picture(str(path), width=Cm(15))
        except Exception as exc:  # ảnh hỏng/không đọc được
            run2 = p.add_run(f"[Không chèn được ảnh {name}.png: {exc}]")
            set_run_font(run2, italic=True)
        cap = doc.add_paragraph()
        cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run_cap = cap.add_run(f"{label}: {caption_text}")
        set_run_font(run_cap, italic=True, size=Pt(12))
        cap.paragraph_format.space_before = Pt(4)
        cap.paragraph_format.space_after = Pt(14)
    else:
        counters["images_missing"] += 1
        missing_images.append(name)
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        pf = p.paragraph_format
        pf.space_before = Pt(6)
        pf.space_after = Pt(6)
        run = p.add_run(f"[[ CHƯA CÓ HÌNH: {name} - chờ subagent UML/chụp màn hình, chạy lại build_docx.py sau ]]")
        set_run_font(run, bold=True, italic=True, color=RGBColor(0xB0, 0x2A, 0x2A))
        # khung viền quanh đoạn placeholder cho dễ thấy khi mở file
        pPr = p._p.get_or_add_pPr()
        pBdr = OxmlElement('w:pBdr')
        for edge in ("top", "left", "bottom", "right"):
            el = OxmlElement(f'w:{edge}')
            el.set(qn('w:val'), 'single')
            el.set(qn('w:sz'), '12')
            el.set(qn('w:space'), '4')
            el.set(qn('w:color'), 'B02A2A')
            pBdr.append(el)
        pPr.append(pBdr)
        cap = doc.add_paragraph()
        cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run_cap = cap.add_run(f"{label}: {caption_text}")
        set_run_font(run_cap, italic=True, size=Pt(12))
        cap.paragraph_format.space_before = Pt(4)
        cap.paragraph_format.space_after = Pt(14)
        print(f"[CANH BAO] Thieu anh: {path}")


# ---------------------------------------------------------------------------
# Bảng Markdown chuẩn (| a | b | c |)
# ---------------------------------------------------------------------------

def parse_pipe_row(line):
    line = line.strip()
    if line.startswith("|"):
        line = line[1:]
    if line.endswith("|"):
        line = line[:-1]
    return [c.strip() for c in line.split("|")]


def render_generic_table(doc, lines):
    rows = [parse_pipe_row(l) for l in lines if l.strip()]
    # loại hàng phân cách kiểu |---|---|
    rows = [r for r in rows if not all(re.match(r'^:?-{2,}:?$', c or '-') for c in r)]
    if not rows:
        return
    ncols = max(len(r) for r in rows)

    numbering["heading_tab_count"] += 1
    title = numbering["last_heading"] or "Bảng số liệu"
    if numbering["heading_tab_count"] > 1:
        title = f"{title} (tiếp theo)"
    add_table_caption(doc, title, before=True)

    table = doc.add_table(rows=0, cols=ncols)
    style_table(table)
    for i, row in enumerate(rows):
        cells = table.add_row().cells
        for j in range(ncols):
            text = row[j] if j < len(row) else ""
            header = (i == 0)
            set_cell_text(cells[j], text, bold=header)
            if header:
                shade_cell(cells[j])
    set_repeat_header(table.rows[0])
    counters["tables"] += 1
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


# ---------------------------------------------------------------------------
# Khối >>USECASE ... >>END
# ---------------------------------------------------------------------------

def parse_usecase_block(lines):
    data = {"main": [], "alt": [], "diagrams": []}
    mode = None
    for raw in lines:
        line = raw.rstrip("\n")
        if not line.strip():
            continue
        if line.strip() == "MAIN:":
            mode = "main"
            continue
        if line.strip() == "ALT:":
            mode = "alt"
            continue
        if line.strip() == "DIAGRAMS:":
            mode = "diagrams"
            continue
        if mode == "main" or mode == "alt":
            parts = line.split("|", 2)
            if len(parts) == 3:
                step, role, text = parts
                data[mode].append((step.strip(), role.strip(), text.strip()))
            continue
        if mode == "diagrams":
            data["diagrams"].append(line.strip())
            continue
        if ":" in line:
            key, val = line.split(":", 1)
            data[key.strip().lower()] = val.strip()
    return data


def render_usecase(doc, data):
    counters["usecases"] += 1
    uid = data.get("id", "")
    name = data.get("name", "")

    add_table_caption(doc, f"Đặc tả use case {uid}_{name}", before=True)

    table = doc.add_table(rows=0, cols=2)
    style_table(table)

    # Hàng tiêu đề gộp
    r = table.add_row().cells
    set_cell_text(r[0], f"Use case: {uid}_{name}", bold=True)
    shade_cell(r[0], "0A3833")
    for run in r[0].paragraphs[0].runs:
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
    r[0].merge(r[1])

    def kv_row(label, key):
        val = data.get(key, "")
        cells = table.add_row().cells
        set_cell_text(cells[0], label, bold=True)
        shade_cell(cells[0])
        set_cell_text(cells[1], val)

    kv_row("Mục đích:", "purpose")
    kv_row("Mô tả:", "desc")
    kv_row("Tác nhân:", "actors")
    kv_row("Điều kiện trước:", "pre")
    kv_row("Điều kiện sau:", "post")

    # Luồng sự kiện chính
    hdr = table.add_row().cells
    set_cell_text(hdr[0], "Luồng sự kiện chính (Basic Flows)", bold=True)
    shade_cell(hdr[0], "1B7A6B")
    for run in hdr[0].paragraphs[0].runs:
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
    hdr[0].merge(hdr[1])

    col = table.add_row().cells
    set_cell_text(col[0], "Actor", bold=True)
    set_cell_text(col[1], "System", bold=True)
    shade_cell(col[0])
    shade_cell(col[1])

    for step, role, text in data["main"]:
        cells = table.add_row().cells
        content = f"{step}. {text}"
        if role.upper() == "A":
            set_cell_text(cells[0], content)
            set_cell_text(cells[1], "")
        else:
            set_cell_text(cells[0], "")
            set_cell_text(cells[1], content)

    if data["alt"]:
        hdr2 = table.add_row().cells
        set_cell_text(hdr2[0], "Luồng sự kiện phụ (Alternative Flows)", bold=True)
        shade_cell(hdr2[0], "C2622D")
        for run in hdr2[0].paragraphs[0].runs:
            run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        hdr2[0].merge(hdr2[1])

        col2 = table.add_row().cells
        set_cell_text(col2[0], "Actor", bold=True)
        set_cell_text(col2[1], "System", bold=True)
        shade_cell(col2[0])
        shade_cell(col2[1])

        for step, role, text in data["alt"]:
            cells = table.add_row().cells
            content = f"{step}. {text}"
            if role.upper() == "A":
                set_cell_text(cells[0], content)
                set_cell_text(cells[1], "")
            else:
                set_cell_text(cells[0], "")
                set_cell_text(cells[1], content)

    counters["tables"] += 1
    doc.add_paragraph().paragraph_format.space_after = Pt(2)

    # Sơ đồ Activity / Sequence
    diagrams = data["diagrams"]
    ac = [d for d in diagrams if d.startswith("AC-")]
    sq = [d for d in diagrams if d.startswith("SQ-")]
    if ac:
        add_paragraph(doc, "Sơ đồ Activity:", bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=2)
        for d in ac:
            insert_image(doc, d)
    if sq:
        add_paragraph(doc, "Sơ đồ Sequence:", bold=True, align=WD_ALIGN_PARAGRAPH.LEFT, space_after=2)
        for d in sq:
            insert_image(doc, d)


# ---------------------------------------------------------------------------
# Khối >>ENTITY ... >>END
# ---------------------------------------------------------------------------

def parse_entity_block(lines):
    data = {"fields": [], "rels": [], "methods": []}
    for raw in lines:
        line = raw.rstrip("\n")
        if not line.strip():
            continue
        # maxsplit giới hạn đúng số cột: cột cuối (Ghi chú) có thể chứa chuỗi
        # Java thật với "||" (OR) - split không giới hạn sẽ bổ đôi luôn cặp "||"
        # thành nhiều mảnh rỗng và làm lệch toàn bộ cột, cắt cụt cặp dấu `` ` ``.
        if line.startswith("FIELD:"):
            parts = line[len("FIELD:"):].split("|", 3)
            parts = [p.strip() for p in parts]
            while len(parts) < 4:
                parts.append("")
            data["fields"].append(parts[:4])
            continue
        if line.startswith("REL:"):
            parts = line[len("REL:"):].split("|", 3)
            parts = [p.strip() for p in parts]
            while len(parts) < 4:
                parts.append("")
            data["rels"].append(parts[:4])
            continue
        if line.startswith("METHOD:"):
            parts = line[len("METHOD:"):].split("|", 2)
            parts = [p.strip() for p in parts]
            while len(parts) < 3:
                parts.append("")
            data["methods"].append(parts[:3])
            continue
        if ":" in line:
            key, val = line.split(":", 1)
            data[key.strip().lower()] = val.strip()
    return data


def render_entity(doc, data):
    counters["entities"] += 1
    name = data.get("name", "")
    table_name = data.get("table", "")
    desc = data.get("desc", "")
    if desc:
        add_paragraph(doc, desc, space_after=6, indent=True)
    if table_name:
        add_paragraph(doc, f"Bảng CSDL tương ứng: {table_name}", italic=True, space_after=6)

    add_table_caption(doc, f"Đặc tả thực thể {name}", before=True)

    table = doc.add_table(rows=0, cols=5)
    style_table(table)
    hdr = table.add_row().cells
    headers = ["STT", "Công việc", "Kiểu dữ liệu", "Ràng buộc", "Ghi chú"]
    for j, h in enumerate(headers):
        set_cell_text(hdr[j], h, bold=True)
        shade_cell(hdr[j])
    set_repeat_header(table.rows[0])

    attrs = data["fields"] + data["rels"]
    if attrs:
        r = table.add_row().cells
        set_cell_text(r[0], "1", bold=True)
        set_cell_text(r[1], "Khai báo thuộc tính", bold=True)
        shade_cell(r[1], "F0EAD6")
        for i, (fname, ftype, frule, fnote) in enumerate(attrs, start=1):
            row = table.add_row().cells
            set_cell_text(row[0], f"1.{i}")
            set_cell_text(row[1], fname)
            set_cell_text(row[2], ftype)
            set_cell_text(row[3], frule)
            set_cell_text(row[4], fnote)

    methods = data["methods"]
    if methods:
        r = table.add_row().cells
        set_cell_text(r[0], "2", bold=True)
        set_cell_text(r[1], "Phương thức", bold=True)
        shade_cell(r[1], "F0EAD6")
        for i, (mname, mtype, mnote) in enumerate(methods, start=1):
            row = table.add_row().cells
            set_cell_text(row[0], f"2.{i}")
            set_cell_text(row[1], mname)
            set_cell_text(row[2], mtype)
            set_cell_text(row[3], mnote)
            set_cell_text(row[4], "")

    counters["tables"] += 1
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


# ---------------------------------------------------------------------------
# TOC field
# ---------------------------------------------------------------------------

def add_toc(doc):
    p = doc.add_paragraph()
    run = p.add_run()
    set_run_font(run)
    fld_begin = OxmlElement('w:fldChar')
    fld_begin.set(qn('w:fldCharType'), 'begin')
    instr = OxmlElement('w:instrText')
    instr.set(qn('xml:space'), 'preserve')
    instr.text = 'TOC \\o "1-3" \\h \\z \\u'
    fld_sep = OxmlElement('w:fldChar')
    fld_sep.set(qn('w:fldCharType'), 'separate')
    fld_text = OxmlElement('w:t')
    fld_text.text = "Nhấn F9 hoặc bấm 'Cập nhật mục lục' trong Word để tạo Mục lục."
    fld_end = OxmlElement('w:fldChar')
    fld_end.set(qn('w:fldCharType'), 'end')

    r_element = run._r
    r_element.append(fld_begin)
    r_element.append(instr)
    r_element.append(fld_sep)
    r_element.append(fld_text)
    r_element.append(fld_end)


# ---------------------------------------------------------------------------
# Trang bìa
# ---------------------------------------------------------------------------

def add_cover_page(doc):
    def centered(text, bold=False, size=13, space_after=4):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(space_after)
        run = p.add_run(text)
        set_run_font(run, bold=bold, size=Pt(size))
        return p

    centered("BỘ CÔNG THƯƠNG", bold=True, size=13)
    centered("ĐẠI HỌC CÔNG NGHIỆP THÀNH PHỐ HỒ CHÍ MINH", bold=True, size=14)
    centered("KHOA CÔNG NGHỆ THÔNG TIN", bold=True, size=13)
    centered("«Chèn logo trường tại đây»", size=12)
    for _ in range(2):
        doc.add_paragraph()
    centered("MÔN: LẬP TRÌNH WWW (JAVA)", bold=True, size=14)
    doc.add_paragraph()
    centered("BÁO CÁO ĐỀ TÀI", bold=True, size=20)
    centered("Đề tài:", size=13)
    centered("WEBSITE GIỚI THIỆU TOUR DU LỊCH VÀ CHO PHÉP", bold=True, size=17)
    centered("ĐĂNG KÝ TOUR TRỰC TUYẾN (TOURBOOKING)", bold=True, size=17)
    doc.add_paragraph()
    centered("Lớp học phần: «điền sau»", size=13)
    centered("Nhóm: «điền sau»", size=13)
    centered("GVHD: «điền sau»", size=13)
    doc.add_paragraph()

    table = doc.add_table(rows=1, cols=4)
    style_table(table)
    hdr = table.rows[0].cells
    for j, h in enumerate(["STT", "HỌ VÀ TÊN", "MSSV", "CHỮ KÝ"]):
        set_cell_text(hdr[j], h, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
        shade_cell(hdr[j])
    set_repeat_header(table.rows[0])
    for i in range(1, 6):
        row = table.add_row().cells
        set_cell_text(row[0], str(i), align=WD_ALIGN_PARAGRAPH.CENTER)
        set_cell_text(row[1], "«điền sau»")
        set_cell_text(row[2], "«điền sau»")
        set_cell_text(row[3], "")
    counters["tables"] += 1

    doc.add_paragraph()
    centered("Thành phố Hồ Chí Minh, năm 2026", size=13)

    p = doc.add_paragraph()
    p.paragraph_format.page_break_before = True


# ---------------------------------------------------------------------------
# Vòng lặp chính: đọc BAO_CAO.md, phân khối, render
# ---------------------------------------------------------------------------

HEADING_RE = re.compile(r'^(#{1,4})\s+(.*)$')
# Cú pháp: [[HINH: ten-file]] (tương thích cũ, chú thích tự suy ra) hoặc
# [[HINH: ten-file | Chú thích tiếng Việt]] (chú thích tường minh, ưu tiên dùng
# cho mọi tên file dạng slug không có cấu trúc để tự suy ra được, ví dụ ảnh
# chụp màn hình hoặc "so-do-phan-cap").
IMAGE_RE = re.compile(r'\[\[HINH:\s*([^|\]]+?)\s*(?:\|\s*(.+?)\s*)?\]\]')
BULLET_RE = re.compile(r'^[-*]\s+(.*)$')
NUMLIST_RE = re.compile(r'^(\d+)\.\s+(.*)$')


def build():
    text = MD_PATH.read_text(encoding="utf-8")
    lines = text.split("\n")

    doc = Document()

    # Style mặc định Normal -> Times New Roman 13, giãn dòng 1.5
    normal = doc.styles["Normal"]
    normal.font.name = FONT_NAME
    normal.font.size = FONT_SIZE
    normal.paragraph_format.line_spacing = 1.5
    rpr = normal.element.get_or_add_rPr()
    rFonts = rpr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rpr.append(rFonts)
    rFonts.set(qn('w:eastAsia'), FONT_NAME)

    # Áp Times New Roman cho các style List Bullet/List Number có sẵn của Word,
    # nếu không chúng vẫn dùng phông theme mặc định (Calibri).
    for style_name in ("List Bullet", "List Number"):
        try:
            st = doc.styles[style_name]
            st.font.name = FONT_NAME
            st.font.size = FONT_SIZE
        except KeyError:
            pass

    for sec in doc.sections:
        sec.left_margin = Cm(3)
        sec.right_margin = Cm(2)
        sec.top_margin = Cm(2)
        sec.bottom_margin = Cm(2)

    add_cover_page(doc)

    toc_heading = doc.add_paragraph(style="Heading 1")
    toc_run = toc_heading.add_run("MỤC LỤC")
    set_run_font(toc_run, bold=True, size=Pt(16))
    toc_heading.alignment = WD_ALIGN_PARAGRAPH.CENTER
    add_toc(doc)
    doc.add_paragraph().paragraph_format.page_break_before = True

    i = 0
    n = len(lines)

    while i < n:
        line = lines[i]
        stripped = line.strip()

        if not stripped:
            i += 1
            continue

        m = HEADING_RE.match(line)
        if m:
            level = len(m.group(1))
            add_heading(doc, m.group(2).strip(), level)
            i += 1
            continue

        if stripped == ">>USECASE":
            block = []
            i += 1
            while i < n and lines[i].strip() != ">>END":
                block.append(lines[i])
                i += 1
            i += 1  # bỏ qua >>END
            data = parse_usecase_block(block)
            render_usecase(doc, data)
            continue

        if stripped == ">>ENTITY":
            block = []
            i += 1
            while i < n and lines[i].strip() != ">>END":
                block.append(lines[i])
                i += 1
            i += 1
            data = parse_entity_block(block)
            render_entity(doc, data)
            continue

        img_m = IMAGE_RE.match(stripped)
        if img_m:
            insert_image(doc, img_m.group(1), caption=img_m.group(2))
            i += 1
            continue

        if stripped.startswith("|"):
            block = []
            while i < n and lines[i].strip().startswith("|"):
                block.append(lines[i])
                i += 1
            render_generic_table(doc, block)
            continue

        bm = BULLET_RE.match(stripped)
        if bm:
            add_list_paragraph(doc, bm.group(1), numbered=False)
            i += 1
            continue

        nm = NUMLIST_RE.match(stripped)
        if nm:
            add_list_paragraph(doc, nm.group(2), numbered=True)
            i += 1
            continue

        # đoạn văn thường - mỗi dòng nguồn là một đoạn Word riêng (đã xác nhận
        # BAO_CAO.md không có đoạn nào bị bẻ dòng thủ công cần nối lại)
        add_paragraph(doc, stripped, indent=True)
        i += 1

    # Lưu qua một file tạm rồi mới thay vào chỗ cũ: nếu BaoCao_TourBooking.docx
    # đang mở trong Word (đúng tình huống hay gặp khi user đang xem báo cáo),
    # ZipFile mở ghi trực tiếp sẽ ném PermissionError. Không được để mất bản mới
    # dựng được chỉ vì file đích đang khoá.
    tmp_path = OUT_PATH.with_suffix(".tmp.docx")
    doc.save(str(tmp_path))
    try:
        tmp_path.replace(OUT_PATH)
        return doc, OUT_PATH
    except PermissionError:
        fallback = OUT_PATH.with_name(OUT_PATH.stem + ".MOI_NHAT.docx")
        tmp_path.replace(fallback)
        print("=" * 70)
        print(f"[CANH BAO] Khong ghi de duoc {OUT_PATH.name} - file dang bi khoa")
        print("(rat co the dang mo trong Word). Da luu ban moi nhat vao:")
        print(f"  {fallback}")
        print("Dong file cu trong Word roi doi ten fallback de thay the, hoac chay lai script.")
        print("=" * 70)
        return doc, fallback


# ---------------------------------------------------------------------------
# Tự kiểm sau khi build: trích ngược document.xml, đếm ký tự markdown còn sót
# ---------------------------------------------------------------------------

def self_check(out_path):
    with zipfile.ZipFile(out_path) as z:
        xml = z.read("word/document.xml").decode("utf-8")

    # Trích "chặt": không chèn newline ở ranh giới đoạn - đây là kiểu trích thô
    # nhất có thể, nếu markdown/tên file nội bộ vẫn lọt qua kiểu trích này thì
    # chắc chắn cũng lọt qua bất kỳ cách trích nào khác (kể cả cách python-docx
    # đọc .paragraphs, vốn đã tách đúng từng đoạn).
    plain = re.sub(r"<[^>]+>", "", xml)

    # Trích "có ranh giới đoạn" - dùng cho các phép kiểm còn lại, tránh dương
    # tính giả kiểu "chú thíchĐoạn văn sau" chỉ vì thiếu khoảng trắng ở chỗ nối.
    xml_with_breaks = xml.replace("</w:p>", "</w:p>\n")
    plain_with_breaks = re.sub(r"<[^>]+>", "", xml_with_breaks)

    star_count = plain.count("**")
    backtick_count = plain.count("`")

    print("-" * 70)
    print("TU KIEM 1: markdown con sot trong .docx (BAT BUOC = 0 CA HAI)")
    print(f"  count('**') = {star_count}")
    print(f"  count('`')  = {backtick_count}")
    if star_count or backtick_count:
        print("  [CANH BAO] Van con ky tu markdown chua duoc xu ly! Vi du xung quanh:")
        for token in ("**", "`"):
            idx = plain.find(token)
            if idx != -1:
                start = max(0, idx - 60)
                print(f"    ...{plain[start:idx+60]}...")

    print("-" * 70)
    print("TU KIEM 2: ten file/quy uoc dat ten noi bo lot ra chu thich (BAT BUOC = 0 TAT CA)")
    leak_patterns = ["screenshot-", "so-do-phan-cap", "screen-flow", "AC-UC", "SQ-UC", "409"]
    leak_total = 0
    for pat in leak_patterns:
        n = plain.count(pat)
        leak_total += n
        print(f"  count('{pat}') = {n}")
        if n:
            idx = plain.find(pat)
            start = max(0, idx - 60)
            print(f"    [CANH BAO] vi du: ...{plain[start:idx+70]}...")

    print("-" * 70)
    print("TU KIEM 3: chu thich hinh dinh lien doan van ngay sau (kiem bang ranh gioi doan)")
    # Tìm mọi chú thích "Hình x.y: ..." rồi xem ký tự ngay sau đoạn đó (ở bản
    # trích CÓ ranh giới đoạn) có phải là xuống dòng hay không.
    stuck = 0
    for m in re.finditer(r"Hình \d+\.\d+: [^\n]*", plain_with_breaks):
        end = m.end()
        if end < len(plain_with_breaks) and plain_with_breaks[end] not in ("\n", ""):
            stuck += 1
            print(f"    [CANH BAO] dinh lien: ...{plain_with_breaks[end-40:end+40]}...")
    print(f"  so chu thich dinh lien doan sau = {stuck}")
    print("-" * 70)

    ok = star_count == 0 and backtick_count == 0 and leak_total == 0 and stuck == 0
    return ok


def report(out_path):
    print("=" * 70)
    print("NGHIEM THU BaoCao_TourBooking.docx")
    print("=" * 70)
    size = out_path.stat().st_size
    print(f"File: {out_path}")
    print(f"Dung luong: {size:,} bytes")
    print(f"So heading: {counters['headings']}")
    print(f"So bang: {counters['tables']}")
    print(f"So use case da dung bang dac ta: {counters['usecases']} / 22")
    print(f"So thuc the da dung bang dac ta: {counters['entities']} / 18 (16 entity + Cart + CartItem)")
    print(f"So cho [[HINH: ... da xu ly: {counters['diagram_refs']}")
    print(f"  - Anh da chen (co file png): {counters['images_found']}")
    print(f"  - Anh con la placeholder (chua co file png): {counters['images_missing']}")
    if missing_images:
        print("\nDanh sach [[HINH: ... con la placeholder:")
        for mmg in missing_images:
            print(f"  - {mmg}")
    print("=" * 70)


if __name__ == "__main__":
    _, saved_path = build()
    report(saved_path)
    ok = self_check(saved_path)
    if not ok:
        sys.exit(1)
