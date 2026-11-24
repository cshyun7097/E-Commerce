package study.seunghyun.ecommerce.domain.member.dto;

import lombok.Builder;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.member.domain.Withdrawal;

@Builder
public record MemberResponse(Long id, String name, String phoneNumber, String address, String loginId,
                             Role role, Withdrawal withdrawal) {

    public static MemberResponse from(Member m) {
        return MemberResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .phoneNumber(m.getPhoneNumber())
                .address(m.getAddress())
                .loginId(m.getLoginId())
                .role(m.getRole())
                .withdrawal(m.getWithdrawal())
                .build();
    }
}
