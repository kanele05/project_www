package vn.edu.iuh.fit.tourbooking.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
// Đồng bộ chỉ mục vector sau khi giao dịch sửa tour commit; lỗi nhà cung cấp chỉ ghi log, không làm hỏng thao tác của quản trị viên.
public class TourIndexListener {

    private final TourVectorService vectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTourChanged(TourChangedEvent event) {
        if (!vectorService.isAvailable()) {
            return;
        }
        try {
            vectorService.upsert(event.tourId());
        } catch (RuntimeException e) {
            log.warn("Không đồng bộ được chỉ mục vector cho tour {}: {}", event.tourId(), e.getMessage());
        }
    }
}
