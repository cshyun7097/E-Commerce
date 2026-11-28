package study.seunghyun.ecommerce.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.member.dto.MemberResponse;
import study.seunghyun.ecommerce.domain.member.dto.MemberUpdateRequest;
import study.seunghyun.ecommerce.domain.member.dto.SignUpRequest;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.member.repository.RefreshTokenRepository;
import study.seunghyun.ecommerce.domain.store.service.StoreService;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StoreService storeService;

    @Override
    public MemberResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        Member member = Member.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();
        try {
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException e) {
            // 동시 가입으로 existsByLoginId 검사를 통과한 중복 아이디 (loginId unique 제약)
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    @Override
    public MemberResponse withdraw(String loginId, String password) {
        Member member = getActiveMemberWithPassword(loginId, password);
        member.withdraw();
        if (member.getRole() == Role.SELLER) {
            storeService.deactivateAllOf(member.getId());
        }
        return MemberResponse.from(member);
    }

    @Override
    public MemberResponse update(String loginId, MemberUpdateRequest request) {
        Member member = getActiveMemberWithPassword(loginId, request.currentPassword());
        if (request.address() != null) member.changeAddress(request.address());
        if (request.phoneNumber() != null) member.changePhoneNumber(request.phoneNumber());
        if (request.newPassword() != null) {
            member.changePassword(passwordEncoder.encode(request.newPassword()));
            // 탈취된 리프레시 토큰으로 계속 재발급받지 못하도록 무효화
            refreshTokenRepository.deleteById(loginId);
        }
        return MemberResponse.from(member);
    }

    // loginId: 로그인한 사용자(토큰의 subject)
    private Member getActiveMemberWithPassword(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_WITHDRAWN);
        }
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        return member;
    }
}
