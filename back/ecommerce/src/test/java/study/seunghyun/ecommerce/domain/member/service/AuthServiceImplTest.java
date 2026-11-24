package study.seunghyun.ecommerce.domain.member.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.RefreshToken;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.member.dto.LoginRequest;
import study.seunghyun.ecommerce.domain.member.dto.TokenResponse;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.member.repository.RefreshTokenRepository;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;
import study.seunghyun.ecommerce.global.JwtTokenProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    MemberRepository memberRepository = mock(MemberRepository.class);
    AuthServiceImpl service = new AuthServiceImpl(authenticationManager, userDetailsService, jwtTokenProvider,
            repository, memberRepository, Duration.ofDays(14));
    LoginRequest loginRequest = LoginRequest.builder().loginId("hong123").password("password1").build();

    RefreshToken refreshToken(String token, Instant expiresAt) {
        return RefreshToken.builder().loginId("hong123").tokenHash(AuthServiceImpl.hash(token)).expiresAt(expiresAt).build();
    }

    UserDetails user(boolean enabled) {
        return User.withUsername("hong123").password("x").roles("BUYER").disabled(!enabled).build();
    }

    Member member() {
        return Member.builder().name("홍길동").phoneNumber("01012345678").address("서울")
                .loginId("hong123").password("x").role(Role.BUYER).build();
    }

    @Test
    void 로그인하면_리프레시_토큰의_해시만_저장한다() {
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("hong123", null, user(true).getAuthorities()));
        when(jwtTokenProvider.createToken(any())).thenReturn("access");

        TokenResponse response = service.login(loginRequest);

        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        verify(repository).upsert(eq("hong123"), savedHash.capture(), any());
        assertThat(savedHash.getValue()).isNotEqualTo(response.refreshToken())
                .isEqualTo(AuthServiceImpl.hash(response.refreshToken()));
        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void 로그인에_실패하면_실패_횟수를_기록하고_성공하면_초기화한다() {
        Member member = member();
        when(memberRepository.findByLoginId("hong123")).thenReturn(Optional.of(member));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> service.login(loginRequest)).isInstanceOf(BadCredentialsException.class);
        assertThat(member.getFailedLoginCount()).isEqualTo(1);

        reset(authenticationManager);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("hong123", null, user(true).getAuthorities()));
        service.login(loginRequest);
        assertThat(member.getFailedLoginCount()).isZero();
    }

    @Test
    void 재발급하면_새_리프레시_토큰으로_교체된다() {
        when(repository.findByTokenHash(AuthServiceImpl.hash("old")))
                .thenReturn(Optional.of(refreshToken("old", Instant.now().plusSeconds(60))));
        when(userDetailsService.loadUserByUsername("hong123")).thenReturn(user(true));
        when(jwtTokenProvider.createToken(any())).thenReturn("access");

        TokenResponse response = service.reissue("old");

        assertThat(response.refreshToken()).isNotEqualTo("old");
        verify(repository).upsert(eq("hong123"), eq(AuthServiceImpl.hash(response.refreshToken())), any());
    }

    @Test
    void 없거나_만료된_토큰은_재발급할_수_없다() {
        when(repository.findByTokenHash(AuthServiceImpl.hash("expired")))
                .thenReturn(Optional.of(refreshToken("expired", Instant.now().minusSeconds(1))));

        assertThatThrownBy(() -> service.reissue("unknown"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        assertThatThrownBy(() -> service.reissue("expired"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 탈퇴한_회원은_재발급할_수_없다() {
        when(repository.findByTokenHash(AuthServiceImpl.hash("token")))
                .thenReturn(Optional.of(refreshToken("token", Instant.now().plusSeconds(60))));
        when(userDetailsService.loadUserByUsername("hong123")).thenReturn(user(false));

        assertThatThrownBy(() -> service.reissue("token"))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        verify(repository, never()).upsert(anyString(), anyString(), any());
    }
}
