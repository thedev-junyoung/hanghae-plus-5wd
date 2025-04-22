package kr.hhplus.be.server.domain.coupon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class CouponTest {

    private final LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
    private final LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

    @Test
    @DisplayName("만료된 쿠폰은 isExpired()가 true를 반환한다")
    void is_expired_should_return_true() {
        // given
        Coupon expired = Coupon.create("EXPIRED", CouponType.PERCENTAGE, 10, 100,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1));

        // expect
        assertThat(expired.isExpired()).isTrue();
    }

    @Test
    @DisplayName("유효한 기간의 쿠폰은 isExpired()가 false를 반환한다")
    void is_expired_should_return_false() {
        Coupon valid = Coupon.create("VALID", CouponType.PERCENTAGE, 10, 100, yesterday, tomorrow);
        assertThat(valid.isExpired()).isFalse();
    }

    @Test
    @DisplayName("잔여 수량이 0이면 isExhausted()는 true를 반환한다")
    void is_exhausted_should_return_true() {
        Coupon coupon = Coupon.create("LIMITED", CouponType.FIXED, 5000, 100, yesterday, tomorrow);
        // 수량 직접 0으로 세팅
        coupon.decreaseQuantity(); // 99
        for (int i = 0; i < 99; i++) {
            coupon.decreaseQuantity();
        }
        assertThat(coupon.isExhausted()).isTrue();
    }

    @Test
    @DisplayName("만료된 쿠폰은 validateUsable()에서 CouponExpiredException 발생")
    void validate_usable_should_throw_expired() {
        Coupon expired = Coupon.create("EXPIRED", CouponType.FIXED, 1000, 100,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(expired::validateUsable)
                .isInstanceOf(CouponException.ExpiredException.class);
    }

    @Test
    @DisplayName("소진된 쿠폰은 validateUsable()에서 CouponAlreadyExhaustedException 발생")
    void validate_usable_should_throw_exhausted() {
        Coupon coupon = Coupon.create("EXHAUSTED", CouponType.FIXED, 1000, 1, yesterday, tomorrow);
        coupon.decreaseQuantity(); // 수량 0

        assertThatThrownBy(coupon::validateUsable)
                .isInstanceOf(CouponException.AlreadyExhaustedException.class);
    }

    @Test
    @DisplayName("유효한 쿠폰은 수량 감소가 정상 동작한다")
    void decrease_quantity_should_succeed() {
        Coupon coupon = Coupon.create("VALID", CouponType.FIXED, 1000, 10, yesterday, tomorrow);
        coupon.decreaseQuantity();
        assertThat(coupon.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("PERCENTAGE 쿠폰은 올바르게 할인 적용한다")
    void percentage_coupon_should_apply_correct_discount() {
        int discounted = CouponType.PERCENTAGE.applyDiscount(10000, 10);
        assertThat(discounted).isEqualTo(9000);
    }

    @Test
    @DisplayName("FIXED 쿠폰은 고정 할인 적용 후 0원 미만 방지한다")
    void fixed_coupon_should_not_go_below_zero() {
        int discounted = CouponType.FIXED.applyDiscount(3000, 5000);
        assertThat(discounted).isEqualTo(0);
    }
}
