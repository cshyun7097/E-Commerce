package study.seunghyun.ecommerce.global;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

// page는 0부터 시작
@Builder
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                              boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
