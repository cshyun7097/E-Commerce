package study.seunghyun.ecommerce.domain.store.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import study.seunghyun.ecommerce.domain.store.service.StoreService;
import study.seunghyun.ecommerce.global.GlobalExceptionHandler;
import study.seunghyun.ecommerce.global.SecurityConfig;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class StoreControllerSecurityTest {

    @Autowired MockMvc mvc;
    @MockitoBean StoreService storeService;
    @MockitoBean UserDetailsService userDetailsService;

    String body = "{\"name\":\"옷가게\"}";

    @Test
    void 판매자는_상점을_등록하고_삭제할_수_있다() throws Exception {
        mvc.perform(post("/api/stores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/stores/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isOk());
    }

    @Test
    void 구매자는_403() throws Exception {
        mvc.perform(post("/api/stores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(delete("/api/stores/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BUYER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 목록은_구매자와_판매자_모두_조회할_수_있다() throws Exception {
        mvc.perform(get("/api/stores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_BUYER"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/stores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isOk());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mvc.perform(post("/api/stores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/stores")).andExpect(status().isUnauthorized());
    }

    @Test
    void 상점_이름이_비어있으면_400() throws Exception {
        mvc.perform(post("/api/stores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
