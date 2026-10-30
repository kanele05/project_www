package vn.edu.iuh.fit.tourbooking.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// Khi ứng dụng sẵn sàng (sau cả bước đổ dữ liệu mẫu): nếu AI bật thì nạp chỉ mục vector (từ tệp đã lưu hoặc lập mới) trên luồng riêng để không chặn việc mở web.
public class TourIndexInitializer {

    private final TourVectorService vectorService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!vectorService.isAvailable()) {
            log.info("AI đang tắt - bỏ qua lập chỉ mục vector (tìm kiếm dùng từ khoá).");
            return;
        }
        Thread.ofVirtual().name("ai-index-init").start(() -> {
            try {
                vectorService.loadOrIndex();
            } catch (RuntimeException e) {
                log.warn("Chưa lập được chỉ mục vector lúc khởi động (kiểm tra khoá API/mạng), "
                        + "quản trị viên có thể bấm Lập chỉ mục lại sau: {}", e.getMessage());
            }
        });
    }
}
