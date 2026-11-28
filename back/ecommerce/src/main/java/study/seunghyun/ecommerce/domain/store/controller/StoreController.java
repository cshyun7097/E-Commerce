package study.seunghyun.ecommerce.domain.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import study.seunghyun.ecommerce.domain.store.dto.StoreCreateRequest;
import study.seunghyun.ecommerce.domain.store.dto.StoreResponse;
import study.seunghyun.ecommerce.domain.store.service.StoreService;
import study.seunghyun.ecommerce.global.ApiResponse;
import study.seunghyun.ecommerce.global.PageResponse;

// 목록 조회(GET /api/stores)는 로그인한 회원 모두, 그 외 /api/stores/** 는 SELLER만 (SecurityConfig)
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // ?page=0&size=20 (최대 100), 최신 상점부터 (정렬은 서버 고정)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StoreResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.getActiveStores(pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> create(Authentication authentication,
                                                             @RequestBody @Valid StoreCreateRequest request) {
        StoreResponse response = storeService.create(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponse>> delete(Authentication authentication,
                                                             @PathVariable Long storeId) {
        StoreResponse response = storeService.delete(authentication.getName(), storeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
