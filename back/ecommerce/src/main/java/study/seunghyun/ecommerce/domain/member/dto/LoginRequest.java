package study.seunghyun.ecommerce.domain.member.dto;

import lombok.Builder;
import jakarta.validation.constraints.NotBlank;

@Builder
public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
}
