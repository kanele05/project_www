# Sơ đồ UML của báo cáo

Thư mục này chứa nguồn và ảnh của các sơ đồ trong chương 2 của `BaoCao_TourBooking.docx`, đánh số theo
**21 use case mới** (thứ tự biểu đồ tổng quát, xem mục 2.6.3 của báo cáo):

- `src/UC0xx_activity.puml`, `src/UC0xx_sequence.puml` (và `png/` tương ứng): sơ đồ hoạt động và tuần tự của
  UC001-UC012, UC019, UC020, UC021 (mỗi use case một cặp).
- UC013 Quản lý danh mục tour: ba cặp `UC013_Them_*`, `UC013_Sua_*`, `UC013_Xoa_*` (Thêm, Sửa, Xoá).
- UC016 Quản lý đơn đặt tour: `UC016_activity` và hai phần tuần tự `UC016_sequence_p1`, `UC016_sequence_p2`.
- UC014, UC015, UC017, UC018 chỉ nằm trong bảng danh sách use case, không có sơ đồ riêng.
- `png/UseCase_TongQuat.png` là Hình 2.2 (biểu đồ use case tổng quát), đặt trên trang ngang của Word. Ảnh do `src/UseCase_TongQuat.py`
  vẽ (SVG + PNG, cần `cairosvg`) với toạ độ tường minh: Đăng nhập nằm giữa hai nhóm tác nhân, các đường «include» là đoạn thẳng
  từ đầu oval, ba use case cuối của quản trị viên đi ngang rồi mới chéo lên để nhãn có chỗ. `src/UseCase_TongQuat.puml` chỉ là bản nguồn
  ngữ nghĩa, PlantUML tự bố trí nên không dùng cho ảnh trong báo cáo.
- `png/ERD.png` (Hình 3.4.1.1) và `png/DatabaseDiagram.png` (Hình 3.4.1.2): đủ 17 bảng, 21 khoá ngoại, **đọc thẳng từ `database/02_schema.sql`**
  (`src/schema_model.py`). ERD kiểu chân quạ, hộp cam, có kiểu dữ liệu, ký hiệu N (cho phép NULL), FK và tên khoá ngoại; Database Diagram kiểu SSMS,
  chỉ tên cột, chìa khoá vàng ở bảng cha, vô cực ở bảng con. `user_events` không có khoá ngoại: ERD vẽ riêng kèm ghi chú, Database Diagram không có đường nối.
  Đường nối là đường vuông góc do `src/diagram_lib.py` (A*) định tuyến; `src/table_diagram.py` là quy trình chung.

## Kiểm tra hình học

```bash
cd docs/report/diagrams/src
python3 ERD.py 60 && python3 DatabaseDiagram.py 40 && python3 UseCase_TongQuat.py   # vẽ lại, ghi *.layout.json
python3 check_layout.py                                                              # phải in "KẾT QUẢ: ĐẠT"
```

`check_layout.py` đọc `*.layout.json` (hình, đường gấp khúc, ký hiệu đầu mút, nhãn) và kiểm: (1) không đoạn nối nào xuyên/chạm hình không phải
đầu mút của nó, kể cả tiêu đề khung; (2) không hai đường chung đoạn; (3) nhãn không đè hình, đường, ký hiệu hay nhãn khác; (4) không hai hình chồng nhau,
mọi use case nằm trong khung, tác nhân nằm ngoài khung; (5) đếm số lần cắt giữa các đường (ERD và Database Diagram bắt buộc bằng 0); (6) đầu mút nằm đúng
trên biên hình, chữ nằm trọn trong oval. Đã thử đột biến (cố ý xuyên hộp, chồng đoạn, đè nhãn, chồng hình, cắt đường) để chắc checker bắt được lỗi.

Nhãn khung của từng sơ đồ dùng đúng mã mới, ví dụ `act [UC006_Đặt tour]`, `sd [UC016_Quản lý đơn đặt tour (phần 1/2)]`.

## Xuất lại ảnh

- Phiên bản PlantUML: 1.2025.4, cần Java và phông Liberation Sans. Sơ đồ hoạt động và tuần tự không cần Graphviz.
- Các sơ đồ dùng `skinparam dpi 140`.

```bash
cd docs/report/diagrams
java -jar plantuml-1.2025.4.jar -tpng -charset UTF-8 -o ../png src/UC0*.puml
```

Tên tệp PNG lấy từ tên sau `@startuml` trong mỗi tệp `.puml`. Sau khi xuất lại, ảnh trong Word phải được thay bằng
cách chèn đè trực tiếp (không chạy lại `build_docx.py`, vì nó sẽ làm mất các thay đổi người dùng đã sửa tay).
