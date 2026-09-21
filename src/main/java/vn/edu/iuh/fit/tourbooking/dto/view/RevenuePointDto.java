package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;

/**
 * Một cột của biểu đồ doanh thu theo tháng.
 *
 * <p>Tồn tại riêng bên cạnh {@code StatisticsService.MonthlyRevenue} vì một lý do
 * rất cụ thể: Jackson tuần tự hoá <b>record</b> theo đúng các thành phần khai báo
 * trong hàm dựng, nên phương thức thêm vào như {@code label()} sẽ <b>không</b>
 * xuất hiện trong JSON. Bản ghi này đưa {@code label} thành một thành phần thật
 * để biểu đồ có sẵn nhãn trục hoành mà không phải ghép chuỗi ở phía trình duyệt.</p>
 */
public record RevenuePointDto(String label,
                              int year,
                              int month,
                              BigDecimal amount,
                              int percentOfMax) {
}
