package study.seunghyun.ecommerce.domain.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record StoreCreateRequest(@NotBlank @Size(max = 50) String name) {
}
