package study.seunghyun.ecommerce.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// shortcut: 회원당 1개만 저장해 새 로그인 시 이전 기기의 리프레시 토큰이 무효화됨, 다중 기기 로그인 필요 시 id를 별도 PK로 분리
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private String loginId;

    // 원문이 아닌 SHA-256 해시 저장 (DB 유출 시 토큰 악용 방지)
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder
    public RefreshToken(String loginId, String tokenHash, Instant expiresAt) {
        this.loginId = loginId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }
}
