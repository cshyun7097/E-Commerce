package study.seunghyun.ecommerce.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(@NotBlank String refreshToken) {
}
