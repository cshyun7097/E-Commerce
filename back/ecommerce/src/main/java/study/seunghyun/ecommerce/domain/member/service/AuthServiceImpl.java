package study.seunghyun.ecommerce.domain.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.RefreshToken;
import study.seunghyun.ecommerce.domain.member.dto.LoginRequest;
import study.seunghyun.ecommerce.domain.member.dto.TokenResponse;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.member.repository.RefreshTokenRepository;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;
import study.seunghyun.ecommerce.global.JwtTokenProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final Duration refreshExpiration;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                           JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository,
                           MemberRepository memberRepository,
                           @Value("${jwt.refresh-expiration}") Duration refreshExpiration) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        this.refreshExpiration = refreshExpiration;
    }

    // 실패 횟수 기록이 예외로 롤백되지 않도록 noRollbackFor 지정
    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.loginId(), request.password()));
        } catch (BadCredentialsException e) {
            memberRepository.findByLoginId(request.loginId()).ifPresent(Member::loginFailed);
            throw e;
        }
        memberRepository.findByLoginId(request.loginId()).ifPresent(Member::loginSucceeded);
        return issueTokens(authentication);
    }

    @Override
    public TokenResponse reissue(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .filter(token -> !token.isExpired())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        UserDetails user = userDetailsService.loadUserByUsername(saved.getLoginId());
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 새 리프레시 토큰으로 덮어써서 사용한 토큰은 재사용 불가 (rotation)
        return issueTokens(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    @Override
    public void logout(String loginId) {
        refreshTokenRepository.deleteById(loginId);
    }

    private TokenResponse issueTokens(Authentication authentication) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.upsert(authentication.getName(), hash(refreshToken), Instant.now().plus(refreshExpiration));
        return TokenResponse.bearer(jwtTokenProvider.createToken(authentication), refreshToken);
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
