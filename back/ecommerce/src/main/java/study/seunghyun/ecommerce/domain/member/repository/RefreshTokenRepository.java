package study.seunghyun.ecommerce.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import study.seunghyun.ecommerce.domain.member.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 동시 로그인에도 PK 충돌 없이 저장/교체 (PostgreSQL 전용)
    @Modifying
    @Query(value = """
            INSERT INTO refresh_token (login_id, token_hash, expires_at)
            VALUES (:loginId, :tokenHash, :expiresAt)
            ON CONFLICT (login_id) DO UPDATE
            SET token_hash = EXCLUDED.token_hash, expires_at = EXCLUDED.expires_at
            """, nativeQuery = true)
    void upsert(String loginId, String tokenHash, Instant expiresAt);
}
