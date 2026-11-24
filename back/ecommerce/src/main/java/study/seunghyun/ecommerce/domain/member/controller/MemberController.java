package study.seunghyun.ecommerce.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import study.seunghyun.ecommerce.domain.member.dto.LoginRequest;
import study.seunghyun.ecommerce.domain.member.dto.MemberResponse;
import study.seunghyun.ecommerce.domain.member.dto.MemberUpdateRequest;
import study.seunghyun.ecommerce.domain.member.dto.ReissueRequest;
import study.seunghyun.ecommerce.domain.member.dto.SignUpRequest;
import study.seunghyun.ecommerce.domain.member.dto.TokenResponse;
import study.seunghyun.ecommerce.domain.member.dto.WithdrawRequest;
import study.seunghyun.ecommerce.domain.member.service.AuthService;
import study.seunghyun.ecommerce.domain.member.service.MemberService;
import study.seunghyun.ecommerce.global.ApiResponse;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<MemberResponse>> signUp(@RequestBody @Valid SignUpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.signUp(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestBody @Valid ReissueRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.reissue(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // /me: 토큰의 subject(loginId)로 본인을 찾으므로 다른 회원은 지정할 수 없음
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> update(Authentication authentication,
                                                              @RequestBody @Valid MemberUpdateRequest request) {
        MemberResponse response = memberService.update(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> withdraw(Authentication authentication,
                                                                @RequestBody @Valid WithdrawRequest request) {
        MemberResponse response = memberService.withdraw(authentication.getName(), request.password());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
