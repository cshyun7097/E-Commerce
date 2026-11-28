package study.seunghyun.ecommerce.domain.store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import study.seunghyun.ecommerce.domain.store.domain.Store;
import study.seunghyun.ecommerce.domain.store.domain.StoreStatus;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Page<Store> findByStatus(StoreStatus status, Pageable pageable);

    // 판매자의 상점을 UPDATE 한 번으로 비활성화 (상점을 하나씩 불러오지 않음)
    @Modifying
    @Query("update Store s set s.status = study.seunghyun.ecommerce.domain.store.domain.StoreStatus.INACTIVE where s.member.id = :memberId")
    int deactivateAllByMemberId(Long memberId);
}
