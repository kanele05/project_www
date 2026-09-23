package vn.edu.iuh.fit.tourbooking.dto.view;

import java.math.BigDecimal;

// Một điểm dữ liệu doanh thu theo tháng, dùng để vẽ biểu đồ ở trang chủ quản trị.
public record RevenuePointDto(String label,
                              int year,
                              int month,
                              BigDecimal amount,
                              int percentOfMax) {
}
