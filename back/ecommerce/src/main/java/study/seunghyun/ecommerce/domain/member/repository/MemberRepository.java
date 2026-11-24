package study.seunghyun.ecommerce.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.seunghyun.ecommerce.domain.member.domain.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByLoginId(String loginId);

    Optional<Member> findByLoginId(String loginId);
}
