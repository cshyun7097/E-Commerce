package study.seunghyun.ecommerce.domain.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import study.seunghyun.ecommerce.domain.member.domain.Member;

// member_id: PostgreSQL은 FK에 인덱스를 자동 생성하지 않음 (판매자별 상점 일괄 비활성화에 사용)
@Entity
@Table(indexes = @Index(name = "idx_store_member_id", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 상점 주인(판매자). member_id FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 판매자가 삭제하거나 탈퇴하면 INACTIVE (상품·주문 기록이 참조할 수 있도록 행은 남김). 기존 행이 있어도 컬럼 추가가 실패하지 않도록 DB 기본값 지정
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    @Column(nullable = false)
    private StoreStatus status;

    @Builder
    public Store(String name, Member member) {
        this.name = name;
        this.member = member;
        this.status = StoreStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == StoreStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = StoreStatus.INACTIVE;
    }

    public boolean isOwnedBy(String loginId) {
        return member.getLoginId().equals(loginId);
    }
}
