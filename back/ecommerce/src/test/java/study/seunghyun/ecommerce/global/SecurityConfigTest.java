package study.seunghyun.ecommerce.global;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    @Test
    void jwt_secret이_32바이트_미만이면_시작_시_실패한다() {
        assertThatThrownBy(() -> new SecurityConfig("short-secret")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new SecurityConfig("a".repeat(32))).doesNotThrowAnyException();
    }
}
