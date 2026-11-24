package study.seunghyun.ecommerce.domain.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.member.domain.Withdrawal;
import study.seunghyun.ecommerce.domain.member.dto.MemberUpdateRequest;
import study.seunghyun.ecommerce.domain.member.dto.SignUpRequest;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.member.repository.RefreshTokenRepository;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;
import study.seunghyun.ecommerce.domain.member.service.MemberServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemberServiceImplTest {

    MemberRepository repository = mock(MemberRepository.class);
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    MemberServiceImpl service = new MemberServiceImpl(repository, encoder, refreshTokenRepository);
    Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .name("홍길동").phoneNumber("010-1234-5678").address("서울")
                .loginId("hong123").password(encoder.encode("password1")).role(Role.BUYER)
                .build();
        when(repository.findByLoginId("hong123")).thenReturn(Optional.of(member));
    }

    SignUpRequest signUpRequest(Role role) {
        return SignUpRequest.builder()
                .name("홍길동").phoneNumber("010-1234-5678").address("서울")
                .loginId("hong123").password("password1").role(role)
                .build();
    }

    @Test
    void 회원가입시_비밀번호를_암호화해서_저장한다() {
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.signUp(signUpRequest(Role.SELLER));

        verify(repository).saveAndFlush(argThat(m -> !m.getPassword().equals("password1")
                && encoder.matches("password1", m.getPassword())
                && m.getPhoneNumber().equals("01012345678")
                && m.getWithdrawal() == Withdrawal.ACTIVE));
    }

    @Test
    void 중복_아이디는_가입할_수_없다() {
        when(repository.existsByLoginId("hong123")).thenReturn(true);

        assertThatThrownBy(() -> service.signUp(
                signUpRequest(Role.BUYER)))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
    }

    @Test
    void 탈퇴하면_INACTIVE가_되고_다시_탈퇴나_수정할_수_없다() {
        service.withdraw("hong123", "password1");

        assertThat(member.getWithdrawal()).isEqualTo(Withdrawal.INACTIVE);
        assertThatThrownBy(() -> service.withdraw("hong123", "password1")).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_WITHDRAWN);
        assertThatThrownBy(() -> service.update("hong123", MemberUpdateRequest.builder().currentPassword("password1").address("부산").build()))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_WITHDRAWN);
    }

    @Test
    void 비밀번호가_틀리면_탈퇴할_수_없다() {
        assertThatThrownBy(() -> service.withdraw("hong123", "wrong-pass")).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.WRONG_PASSWORD);
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void 없는_회원은_수정하거나_탈퇴할_수_없다() {
        assertThatThrownBy(() -> service.withdraw("other", "password1"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThatThrownBy(() -> service.update("other", MemberUpdateRequest.builder().currentPassword("password1").address("부산").build()))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 정보수정은_입력한_항목만_바꾼다() {
        service.update("hong123", MemberUpdateRequest.builder().currentPassword("password1").address("부산").newPassword("newPassword1").build());

        assertThat(member.getAddress()).isEqualTo("부산");
        assertThat(member.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(encoder.matches("newPassword1", member.getPassword())).isTrue();
        verify(refreshTokenRepository).deleteById("hong123");
    }

    @Test
    void 전화번호는_하이픈을_빼고_저장한다() {
        service.update("hong123", MemberUpdateRequest.builder().currentPassword("password1").phoneNumber("010-9876-5432").build());

        assertThat(member.getPhoneNumber()).isEqualTo("01098765432");
    }

    @Test
    void 비밀번호를_바꾸지_않으면_리프레시_토큰을_유지한다() {
        service.update("hong123", MemberUpdateRequest.builder().currentPassword("password1").address("부산").build());

        verify(refreshTokenRepository, never()).deleteById(any());
    }

    @Test
    void 동시_가입으로_DB_unique_제약에_걸려도_중복_아이디로_응답한다() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.signUp(signUpRequest(Role.BUYER)))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
    }
}
