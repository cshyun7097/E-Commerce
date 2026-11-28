package study.seunghyun.ecommerce.global;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private final SecretKey secretKey;

    public SecurityConfig(@Value("${jwt.secret}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        // HS256은 32바이트 이상 키 필요. 짧으면 첫 로그인이 아니라 서버 시작 시점에 실패시킴
        if (key.length < 32) {
            throw new IllegalStateException("jwt.secret은 32바이트 이상이어야 합니다.");
        }
        this.secretKey = new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        // 토큰의 roles 클레임(ROLE_BUYER 등)을 그대로 권한으로 사용
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        // 필터 단계(토큰 없음/위조/만료)에서 나가는 오류도 ApiResponse 형식으로 응답
        AuthenticationEntryPoint entryPoint = (request, response, e) ->
                writeError(response, objectMapper, ErrorCode.UNAUTHORIZED);

        return http
                // 쿠키를 쓰지 않는 Bearer 토큰 방식이라 CSRF 불필요
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/members/signup", "/api/members/login", "/api/members/reissue").permitAll()
                        // 순서 중요: 목록 조회 규칙이 SELLER 규칙보다 먼저 와야 함
                        .requestMatchers(HttpMethod.GET, "/api/stores").hasAnyRole("BUYER", "SELLER")
                        .requestMatchers("/api/stores/**").hasRole("SELLER")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, objectMapper, ErrorCode.FORBIDDEN)))
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint))
                .build();
    }

    private static void writeError(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
