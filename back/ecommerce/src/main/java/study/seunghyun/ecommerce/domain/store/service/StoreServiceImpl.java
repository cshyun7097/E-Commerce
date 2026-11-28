package study.seunghyun.ecommerce.domain.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.store.domain.Store;
import study.seunghyun.ecommerce.domain.store.domain.StoreStatus;
import study.seunghyun.ecommerce.domain.store.dto.StoreCreateRequest;
import study.seunghyun.ecommerce.domain.store.dto.StoreResponse;
import study.seunghyun.ecommerce.domain.store.repository.StoreRepository;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;
import study.seunghyun.ecommerce.global.PageResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    // SELLER 권한 검사는 SecurityConfig에서 처리. 여기서는 탈퇴 여부만 확인 (탈퇴 후에도 액세스 토큰은 만료 전까지 유효)
    @Override
    public StoreResponse create(String loginId, StoreCreateRequest request) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_WITHDRAWN);
        }
        Store store = Store.builder()
                .name(request.name())
                .member(member)
                .build();
        return StoreResponse.from(storeRepository.save(store));
    }

    @Override
    public StoreResponse delete(String loginId, Long storeId) {
        Store store = storeRepository.findById(storeId)
                .filter(Store::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        if (!store.isOwnedBy(loginId)) {
            throw new BusinessException(ErrorCode.NOT_STORE_OWNER);
        }
        if (!store.getMember().isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_WITHDRAWN);
        }
        store.deactivate();
        return StoreResponse.from(store);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> getActiveStores(Pageable pageable) {
        // 클라이언트 sort 값은 무시하고 최신순 고정 (잘못된 속성은 500, member.password 같은 정렬도 막음)
        Pageable latestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(storeRepository.findByStatus(StoreStatus.ACTIVE, latestFirst).map(StoreResponse::from));
    }

    @Override
    public void deactivateAllOf(Long memberId) {
        storeRepository.deactivateAllByMemberId(memberId);
    }
}
