# Sơ đồ UML của báo cáo

Thư mục này chứa mã nguồn PlantUML (`src/*.puml`) và ảnh PNG đã xuất (`png/*.png`) của 22 sơ đồ
hoạt động và tuần tự cho UC001 đến UC004 và UC011 đến UC017, đã chèn vào `BaoCao_TourBooking.docx`.

`png/UseCase_TongQuat.png` là biểu đồ use case tổng quát (Hình 2.2), do người dùng vẽ, không có tệp `.puml`.

## Xuất lại ảnh

- Phiên bản PlantUML: 1.2025.4 (`plantuml-1.2025.4.jar`), cần Java và phông Liberation Sans.
- Các sơ đồ dùng `skinparam dpi 140`, không cần Graphviz.

```bash
cd docs/report/diagrams
java -jar plantuml-1.2025.4.jar -tpng -charset UTF-8 -o ../png src/*.puml
```

Tên tệp PNG lấy từ tên sau `@startuml` trong mỗi tệp `.puml`. Với cùng phiên bản, kết quả giống từng byte.
Sau khi xuất lại, ảnh trong Word phải được thay bằng cách chèn đè trực tiếp (không chạy lại `build_docx.py`,
vì nó sẽ làm mất các ảnh người dùng đã tự chèn).
