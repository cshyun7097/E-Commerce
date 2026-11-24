package study.seunghyun.ecommerce.domain.member.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.member.repository.RefreshTokenRepository;
import study.seunghyun.ecommerce.domain.member.service.AuthServiceImpl;
import study.seunghyun.ecommerce.domain.member.service.MemberService;
import study.seunghyun.ecommerce.global.GlobalExceptionHandler;
import study.seunghyun.ecommerce.global.JwtTokenProvider;
import study.seunghyun.ecommerce.global.SecurityConfig;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtTokenProvider.class, AuthServiceImpl.class})
class MemberControllerSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean MemberService memberService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean RefreshTokenRepository refreshTokenRepository;
    @MockitoBean MemberRepository memberRepository;

    @Test
    void 로그인하지_않으면_수정과_탈퇴는_401() throws Exception {
        mvc.perform(patch("/api/members/me").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"password1\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/members/me").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 로그인하면_발급된_토큰으로_인증된다() throws Exception {
        when(userDetailsService.loadUserByUsername("hong123"))
                .thenReturn(User.withUsername("hong123").password(encoder.encode("password1")).roles("BUYER").build());

        String body = mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"hong123\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

        mvc.perform(patch("/api/members/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"password1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 재발급은_토큰_없이_호출할_수_있고_로그아웃은_인증이_필요하다() throws Exception {
        mvc.perform(post("/api/members/reissue").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 리프레시 토큰입니다."));
        mvc.perform(post("/api/members/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void 아이디는_영문_소문자와_숫자만_허용한다() throws Exception {
        mvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phoneNumber\":\"010-1234-5678\",\"address\":\"서울\",\"loginId\":\"Hong123\",\"password\":\"password1\",\"role\":\"BUYER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("loginId: 아이디는 영문 소문자/숫자 4~20자여야 합니다."));
    }

    @Test
    void 입력값이_잘못되면_400과_필드별_메시지() throws Exception {
        mvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phoneNumber\":\"123\",\"address\":\"서울\",\"loginId\":\"hong123\",\"password\":\"password1\",\"role\":\"BUYER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("phoneNumber: 전화번호 형식이 올바르지 않습니다."));
    }

    @Test
    void 잘못된_JSON은_400() throws Exception {
        mvc.perform(post("/api/members/signup").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    void 주소를_공백으로_수정할_수_없다() throws Exception {
        mvc.perform(patch("/api/members/me").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"password1\",\"address\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("address: 주소를 입력해주세요."));
    }

    @Test
    void 잠긴_계정은_비밀번호가_맞아도_로그인할_수_없다() throws Exception {
        when(userDetailsService.loadUserByUsername("hong123"))
                .thenReturn(User.withUsername("hong123").password(encoder.encode("password1")).roles("BUYER").accountLocked(true).build());

        mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"hong123\",\"password\":\"password1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void 로그인_중_DB_장애는_비밀번호_틀림이_아니라_500() throws Exception {
        when(userDetailsService.loadUserByUsername("hong123")).thenThrow(new IllegalStateException("DB down"));

        mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"hong123\",\"password\":\"password1\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void 위조된_토큰은_401() throws Exception {
        mvc.perform(patch("/api/members/me").header("Authorization", "Bearer abc.def.ghi")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"password1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비밀번호가_틀리면_로그인_401() throws Exception {
        when(userDetailsService.loadUserByUsername("hong123"))
                .thenReturn(User.withUsername("hong123").password(encoder.encode("password1")).roles("BUYER").build());

        mvc.perform(post("/api/members/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"hong123\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }
}
