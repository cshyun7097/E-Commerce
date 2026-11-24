package study.seunghyun.ecommerce.domain.member.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

// upsert는 PostgreSQL 전용 SQL이라 실제 DB로 검증. Docker가 없으면 건너뜀
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired RefreshTokenRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void 같은_회원으로_다시_저장하면_기존_토큰을_교체한다() {
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        repository.upsert("hong123", "hash-1", expiresAt);
        repository.upsert("hong123", "hash-2", expiresAt);
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByTokenHash("hash-1")).isEmpty();
        assertThat(repository.findByTokenHash("hash-2")).isPresent();
    }
}
