package study.seunghyun.ecommerce.domain.member.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    Member member = Member.builder().name("홍길동").phoneNumber("010-1234-5678").address("서울")
            .loginId("hong123").password("x").role(Role.BUYER).build();

    @Test
    void 로그인_5회_연속_실패하면_잠긴다() {
        for (int i = 0; i < 4; i++) member.loginFailed();
        assertThat(member.isLocked()).isFalse();

        member.loginFailed();
        assertThat(member.isLocked()).isTrue();
    }

    @Test
    void 로그인에_성공하면_실패_횟수와_잠금이_초기화된다() {
        for (int i = 0; i < 4; i++) member.loginFailed();
        member.loginSucceeded();
        member.loginFailed();

        assertThat(member.isLocked()).isFalse();
        assertThat(member.getFailedLoginCount()).isEqualTo(1);
    }
}
