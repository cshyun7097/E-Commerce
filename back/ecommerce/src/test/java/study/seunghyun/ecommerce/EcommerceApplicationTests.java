package study.seunghyun.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Docker가 없으면 건너뜀. 있으면 테스트용 PostgreSQL을 띄워 앱 전체 설정을 검증
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EcommerceApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Test
	void contextLoads() {
	}

}
