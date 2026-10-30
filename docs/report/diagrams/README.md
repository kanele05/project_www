# Sơ đồ UML của báo cáo

Thư mục này chứa nguồn và ảnh của các sơ đồ trong chương 2 của `BaoCao_TourBooking.docx`, đánh số theo
**21 use case mới** (thứ tự biểu đồ tổng quát, xem mục 2.6.3 của báo cáo):

- `src/UC0xx_activity.puml`, `src/UC0xx_sequence.puml` (và `png/` tương ứng): sơ đồ hoạt động và tuần tự của
  UC001-UC012, UC019, UC020, UC021 (mỗi use case một cặp).
- UC013 Quản lý danh mục tour: ba cặp `UC013_Them_*`, `UC013_Sua_*`, `UC013_Xoa_*` (Thêm, Sửa, Xoá).
- UC016 Quản lý đơn đặt tour: `UC016_activity` và hai phần tuần tự `UC016_sequence_p1`, `UC016_sequence_p2`.
- UC014, UC015, UC017, UC018 chỉ nằm trong bảng danh sách use case, không có sơ đồ riêng.
- `png/UseCase_TongQuat.png` là Hình 2.2 (biểu đồ use case tổng quát). Ảnh do `src/UseCase_TongQuat.py` vẽ (xuất
  `src/UseCase_TongQuat.svg`, cần thư viện `cairosvg`) theo bố cục của biểu đồ gốc người dùng vẽ. `src/UseCase_TongQuat.puml` chỉ là bản
  nguồn ngữ nghĩa, PlantUML tự bố trí nên không dùng cho ảnh trong báo cáo.

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
