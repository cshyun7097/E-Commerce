package study.seunghyun.ecommerce.domain.store.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.store.domain.Store;
import study.seunghyun.ecommerce.domain.store.domain.StoreStatus;

import static org.assertj.core.api.Assertions.assertThat;

// JPQL 일괄 UPDATE와 상태 조건 조회를 실제 DB로 검증. Docker가 없으면 건너뜀
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class StoreRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired StoreRepository repository;
    @Autowired EntityManager em;

    Member seller(String loginId) {
        Member member = Member.builder().name("판매자").phoneNumber("01012345678").address("서울")
                .loginId(loginId).password("x").role(Role.SELLER).build();
        em.persist(member);
        return member;
    }

    @Test
    void 판매자의_상점만_비활성화되고_목록에서_빠진다() {
        Member seller1 = seller("seller1");
        Member seller2 = seller("seller2");
        repository.save(Store.builder().name("A").member(seller1).build());
        repository.save(Store.builder().name("B").member(seller1).build());
        repository.save(Store.builder().name("C").member(seller2).build());
        em.flush();

        assertThat(repository.deactivateAllByMemberId(seller1.getId())).isEqualTo(2);
        em.clear();

        Page<Store> active = repository.findByStatus(StoreStatus.ACTIVE, PageRequest.of(0, 20));
        assertThat(active.getContent()).extracting(Store::getName).containsExactly("C");
        assertThat(active.getTotalElements()).isEqualTo(1);
    }
}
