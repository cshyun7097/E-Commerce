package study.seunghyun.ecommerce.domain.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Duration;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Withdrawal withdrawal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 기존 행이 있어도 컬럼 추가가 실패하지 않도록 DB 기본값 지정
    @ColumnDefault("0")
    @Column(nullable = false)
    private int failedLoginCount;

    private Instant lockedUntil;

    @Builder
    public Member(String name, String phoneNumber, String address, String loginId, String password, Role role) {
        this.name = name;
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
        this.address = address;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.withdrawal = Withdrawal.ACTIVE;
    }

    public boolean isActive() {
        return withdrawal == Withdrawal.ACTIVE;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    // 연속 실패가 MAX_LOGIN_FAILURES회가 되면 LOCK_DURATION 동안 잠금
    public void loginFailed() {
        failedLoginCount++;
        if (failedLoginCount >= MAX_LOGIN_FAILURES) {
            lockedUntil = Instant.now().plus(LOCK_DURATION);
            failedLoginCount = 0;
        }
    }

    public void loginSucceeded() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public void withdraw() {
        this.withdrawal = Withdrawal.INACTIVE;
    }

    public void changeAddress(String address) {
        this.address = address;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
    }

    public void changePassword(String password) {
        this.password = password;
    }

    // 010-1234-5678, 01012345678 모두 01012345678로 저장
    private static String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replace("-", "");
    }
}
