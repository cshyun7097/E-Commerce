package study.seunghyun.ecommerce.domain.store.dto;

import lombok.Builder;
import study.seunghyun.ecommerce.domain.store.domain.Store;
import study.seunghyun.ecommerce.domain.store.domain.StoreStatus;

@Builder
public record StoreResponse(Long id, String name, Long memberId, StoreStatus status) {

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                // LAZY 프록시의 getId()는 member를 조회하지 않음 (목록에서 N+1 없음)
                .memberId(store.getMember().getId())
                .status(store.getStatus())
                .build();
    }
}
