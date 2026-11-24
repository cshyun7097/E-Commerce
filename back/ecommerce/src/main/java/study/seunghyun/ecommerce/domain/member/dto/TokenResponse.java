package study.seunghyun.ecommerce.domain.member.dto;

import lombok.Builder;
@Builder
public record TokenResponse(String accessToken, String refreshToken, String tokenType) {

    public static TokenResponse bearer(String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
