package study.seunghyun.ecommerce.domain.member.service;

import study.seunghyun.ecommerce.domain.member.dto.LoginRequest;
import study.seunghyun.ecommerce.domain.member.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse reissue(String refreshToken);

    void logout(String loginId);
}
