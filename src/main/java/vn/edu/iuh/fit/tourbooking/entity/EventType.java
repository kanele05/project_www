package vn.edu.iuh.fit.tourbooking.entity;

// Loại hành vi giao diện được ghi vào bảng user_events.
public enum EventType {

    VIEW(1),

    CLICK(2),

    ADD_TO_CART(3),

    SEARCH(0),

    TIME_ON_PAGE(0);

    private final int weight;

    EventType(int weight) {
        this.weight = weight;
    }

    // Trọng số tín hiệu quan tâm khi dựng gợi ý (0 = không dùng để gợi ý).
    public int getWeight() {
        return weight;
    }

    public String getMessageKey() {
        return "tracking.event." + name();
    }
}
