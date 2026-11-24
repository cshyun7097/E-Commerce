package study.seunghyun.ecommerce.domain.member.dto;

import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// null인 항목은 변경하지 않음. 본인 확인을 위해 현재 비밀번호 필수
@Builder
public record MemberUpdateRequest(
        @NotBlank String currentPassword,
        @Pattern(regexp = "^01\\d-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.") String phoneNumber,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "주소를 입력해주세요.") String address,
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.") String newPassword
) {
}
