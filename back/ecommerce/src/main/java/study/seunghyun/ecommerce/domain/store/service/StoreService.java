package study.seunghyun.ecommerce.domain.store.service;

import study.seunghyun.ecommerce.domain.store.dto.StoreCreateRequest;
import study.seunghyun.ecommerce.domain.store.dto.StoreResponse;

import org.springframework.data.domain.Pageable;

import study.seunghyun.ecommerce.global.PageResponse;

public interface StoreService {

    StoreResponse create(String loginId, StoreCreateRequest request);

    StoreResponse delete(String loginId, Long storeId);

    PageResponse<StoreResponse> getActiveStores(Pageable pageable);

    void deactivateAllOf(Long memberId);
}
