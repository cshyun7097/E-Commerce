package study.seunghyun.ecommerce.domain.store.service;

import org.junit.jupiter.api.Test;
import study.seunghyun.ecommerce.domain.member.domain.Member;
import study.seunghyun.ecommerce.domain.member.domain.Role;
import study.seunghyun.ecommerce.domain.member.repository.MemberRepository;
import study.seunghyun.ecommerce.domain.store.domain.Store;
import study.seunghyun.ecommerce.domain.store.dto.StoreCreateRequest;
import study.seunghyun.ecommerce.domain.store.dto.StoreResponse;
import study.seunghyun.ecommerce.domain.store.repository.StoreRepository;
import study.seunghyun.ecommerce.global.BusinessException;
import study.seunghyun.ecommerce.global.ErrorCode;
import study.seunghyun.ecommerce.global.PageResponse;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import study.seunghyun.ecommerce.domain.store.domain.StoreStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StoreServiceImplTest {

    StoreRepository storeRepository = mock(StoreRepository.class);
    MemberRepository memberRepository = mock(MemberRepository.class);
    StoreServiceImpl service = new StoreServiceImpl(storeRepository, memberRepository);

    Member seller = Member.builder().name("판매자").phoneNumber("01012345678").address("서울")
            .loginId("seller1").password("x").role(Role.SELLER).build();

    @Test
    void 상점을_등록하면_로그인한_판매자가_주인이_된다() {
        when(memberRepository.findByLoginId("seller1")).thenReturn(Optional.of(seller));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StoreResponse response = service.create("seller1", StoreCreateRequest.builder().name("옷가게").build());

        assertThat(response.name()).isEqualTo("옷가게");
        verify(storeRepository).save(argThat(s -> s.getMember() == seller));
    }

    @Test
    void 탈퇴한_판매자는_상점을_등록할_수_없다() {
        seller.withdraw();
        when(memberRepository.findByLoginId("seller1")).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.create("seller1", StoreCreateRequest.builder().name("옷가게").build()))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_WITHDRAWN);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void 본인_상점만_삭제할_수_있다() {
        Store store = Store.builder().name("옷가게").member(seller).build();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.delete("seller2", 1L))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.NOT_STORE_OWNER);
        assertThat(store.isActive()).isTrue();

        StoreResponse response = service.delete("seller1", 1L);
        assertThat(response.status()).isEqualTo(StoreStatus.INACTIVE);
        verify(storeRepository, never()).delete(any());
    }

    @Test
    void 이미_삭제된_상점은_다시_삭제할_수_없다() {
        Store store = Store.builder().name("옷가게").member(seller).build();
        store.deactivate();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.delete("seller1", 1L))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void 탈퇴한_판매자는_상점을_삭제할_수_없다() {
        Store store = Store.builder().name("옷가게").member(seller).build();
        seller.withdraw();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.delete("seller1", 1L))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.ALREADY_WITHDRAWN);
        assertThat(store.isActive()).isTrue();
    }

    @Test
    void 목록은_활성_상점만_최신순으로_조회하고_클라이언트_정렬은_무시한다() {
        PageRequest expected = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(storeRepository.findByStatus(StoreStatus.ACTIVE, expected))
                .thenReturn(new PageImpl<>(List.of(Store.builder().name("옷가게").member(seller).build()), expected, 41));

        PageResponse<StoreResponse> response = service.getActiveStores(PageRequest.of(0, 20, Sort.by("member.password")));

        assertThat(response.content()).extracting(StoreResponse::name).containsExactly("옷가게");
        assertThat(response.totalElements()).isEqualTo(41);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.page()).isZero();
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 없는_상점은_삭제할_수_없다() {
        when(storeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("seller1", 1L))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }
}
