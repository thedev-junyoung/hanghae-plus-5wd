package kr.hhplus.be.server.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CouponIssueTest {

    private final String code = "TEST10";
    private final long userId = 1L;

    @Test
    @DisplayName("쿠폰 발급 시 수량이 차감되고 발급 시간이 저장된다")
    void create_shouldDecreaseQuantityAndSetIssuedAt() {
        Coupon coupon = Coupon.create(code, CouponType.FIXED, 1000, 10,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        CouponIssue issue = CouponIssue.create(userId, coupon);

        assertThat(issue.getUserId()).isEqualTo(userId);
        assertThat(issue.getCoupon()).isEqualTo(coupon);
        assertThat(issue.isUsed()).isFalse();
        assertThat(issue.getIssuedAt()).isNotNull();
        assertThat(coupon.getRemainingQuantity()).isEqualTo(9); // 수량 차감 확인
    }

    @Test
    @DisplayName("markAsUsed 호출 시 isUsed가 true가 된다")
    void markAsUsed_shouldSetIsUsedTrue() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon);

        issue.markAsUsed();

        assertThat(issue.isUsed()).isTrue();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰에 markAsUsed 호출 시 예외 발생")
    void markAsUsed_shouldThrowIfAlreadyUsed() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon);
        issue.markAsUsed();

        assertThatThrownBy(issue::markAsUsed)
                .isInstanceOf(CouponException.AlreadyIssuedException.class);
    }

    @Test
    @DisplayName("사용자 관점 유효성 검사 통과 (정상)")
    void validateUsable_shouldPassWhenValid() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon);

        // when & then
        assertThatCode(issue::validateUsable).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 사용한 쿠폰은 validateUsable 실패")
    void validateUsable_shouldFailIfUsed() {
        Coupon coupon = createValidCoupon();
        CouponIssue issue = CouponIssue.create(userId, coupon);
        issue.markAsUsed();

        assertThatThrownBy(issue::validateUsable)
                .isInstanceOf(CouponException.AlreadyIssuedException.class);
    }

    @Test
    @DisplayName("만료된 쿠폰은 validateUsable 실패")
    void validateUsable_shouldFailIfExpired() {
        Coupon coupon = Coupon.create(code, CouponType.PERCENTAGE, 10, 10,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1)); // 만료됨

        assertThatThrownBy(() -> CouponIssue.create(userId, coupon))
                .isInstanceOf(CouponException.ExpiredException.class);
    }

    private Coupon createValidCoupon() {
        return Coupon.create(code, CouponType.FIXED, 1000, 10,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
    }
}
