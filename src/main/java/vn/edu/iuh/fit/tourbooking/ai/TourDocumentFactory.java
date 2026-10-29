package vn.edu.iuh.fit.tourbooking.ai;

import org.springframework.ai.document.Document;
import vn.edu.iuh.fit.tourbooking.entity.Tour;

import java.util.LinkedHashMap;
import java.util.Map;

// Biến một Tour thành Document để nhúng vector: id xác định (upsert), nội dung mô tả, metadata để lọc.
public final class TourDocumentFactory {

    public static final String ID_PREFIX = "tour-";

    private static final int MAX_DESCRIPTION_CHARS = 400;

    private TourDocumentFactory() {
    }

    public static String documentId(Long tourId) {
        return ID_PREFIX + tourId;
    }

    public static Document toDocument(Tour tour) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tourId", tour.getId());
        metadata.put("categoryId", tour.getCategory().getId());
        metadata.put("category", tour.getCategory().getName());
        metadata.put("destination", tour.getDestination());
        metadata.put("price", tour.getBasePrice().longValue());
        metadata.put("days", tour.getDurationDays());
        return new Document(documentId(tour.getId()), text(tour), metadata);
    }

    // Nội dung nhúng: tên, danh mục, điểm đến, nơi khởi hành, thời lượng, phương tiện, điểm nhấn và mô tả (cắt gọn).
    public static String text(Tour tour) {
        StringBuilder sb = new StringBuilder();
        sb.append(tour.getName()).append(". ");
        sb.append("Danh mục: ").append(tour.getCategory().getName()).append(". ");
        sb.append("Điểm đến: ").append(tour.getDestination()).append(". ");
        sb.append("Khởi hành từ ").append(tour.getDepartureLocation()).append(", ")
                .append(tour.getDurationText());
        if (tour.getTransportation() != null) {
            sb.append(", đi bằng ").append(tour.getTransportation());
        }
        sb.append(". ");
        if (tour.getShortDescription() != null) {
            sb.append(tour.getShortDescription()).append(' ');
        }
        if (tour.getDescription() != null) {
            String description = tour.getDescription().replaceAll("\\s+", " ").trim();
            sb.append(description.length() > MAX_DESCRIPTION_CHARS
                    ? description.substring(0, MAX_DESCRIPTION_CHARS) : description);
        }
        return sb.toString().trim();
    }
}
