package study.seunghyun.ecommerce.domain.member.dto;

import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import study.seunghyun.ecommerce.domain.member.domain.Role;

@Builder
public record SignUpRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자/숫자 4~20자여야 합니다.") String loginId,
        @NotBlank @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.") String password,
        @NotNull Role role
) {
}
