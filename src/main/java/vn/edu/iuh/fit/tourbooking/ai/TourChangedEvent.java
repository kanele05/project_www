package vn.edu.iuh.fit.tourbooking.ai;

// Sự kiện "tour vừa được thêm/sửa/xoá/đổi trạng thái" để đồng bộ chỉ mục vector sau khi giao dịch commit.
public record TourChangedEvent(Long tourId) {
}
