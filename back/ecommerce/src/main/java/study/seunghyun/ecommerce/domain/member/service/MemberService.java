package study.seunghyun.ecommerce.domain.member.service;

import study.seunghyun.ecommerce.domain.member.dto.MemberResponse;
import study.seunghyun.ecommerce.domain.member.dto.MemberUpdateRequest;
import study.seunghyun.ecommerce.domain.member.dto.SignUpRequest;

public interface MemberService {

    MemberResponse signUp(SignUpRequest request);

    MemberResponse withdraw(String loginId, String password);

    MemberResponse update(String loginId, MemberUpdateRequest request);
}
