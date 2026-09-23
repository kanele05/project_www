package vn.edu.iuh.fit.tourbooking.dto.view;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Khuôn JSON phân trang chung cho REST, chuyển từ Page<E> của Spring Data sang DTO hiển thị D.
public record PageResponse<T>(List<T> content,
                              int page,
                              int size,
                              long totalElements,
                              int totalPages,
                              boolean first,
                              boolean last) {

    public static <E, D> PageResponse<D> of(Page<E> source, Function<E, D> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast());
    }
}
