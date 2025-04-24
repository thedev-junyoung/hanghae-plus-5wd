package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.coupon.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponIssueRepository couponIssueRepository;

    @InjectMocks
    private CouponService couponService;

    private final String couponCode = "TEST10";
    private final long userId = 1L;


    @Test
    @DisplayName("쿠폰 정상 발급 성공")
    void issueCoupon_success() {
        // given
        Coupon coupon = createValidCoupon();
        IssueLimitedCouponCommand command = new IssueLimitedCouponCommand(userId, couponCode);

        given(couponRepository.findByCodeForUpdate(couponCode)).willReturn(coupon);
        given(couponIssueRepository.hasIssued(userId, coupon.getId())).willReturn(false);

        // when
        CouponResult result = couponService.issueLimitedCoupon(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);

        // captor 생성
        ArgumentCaptor<CouponIssue> captor = ArgumentCaptor.forClass(CouponIssue.class);
        verify(couponIssueRepository).save(captor.capture()); // 실제로 넘긴 객체 캡처

        CouponIssue saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getCoupon()).isEqualTo(coupon); // 같은 객체인지 검증
    }

    @Test
    @DisplayName("이미 쿠폰을 발급받은 경우 예외 발생")
    void issueCoupon_fail_ifAlreadyIssued() {
        // given
        Coupon coupon = createValidCoupon();
        given(couponRepository.findByCodeForUpdate(couponCode)).willReturn(coupon);
        given(couponIssueRepository.hasIssued(userId, coupon.getId())).willReturn(true);

        // when & then
        assertThrows(CouponException.AlreadyIssuedException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode)));
    }

    @Test
    @DisplayName("만료된 쿠폰일 경우 예외 발생")
    void issueCoupon_fail_ifExpired() {
        // given
        Coupon coupon = Coupon.create(
                couponCode,
                CouponType.FIXED,
                1000,
                10,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(1)
        );

        given(couponRepository.findByCodeForUpdate(couponCode)).willReturn(coupon);

        // when & then
        assertThrows(CouponException.ExpiredException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode))
        );
    }


    @Test
    @DisplayName("수량 소진된 쿠폰일 경우 예외 발생")
    void issueCoupon_fail_ifSoldOut() {
        // given
        Coupon coupon = new Coupon(
                couponCode,
                CouponType.PERCENTAGE,
                20,
                10,
                0,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        given(couponRepository.findByCodeForUpdate(couponCode)).willReturn(coupon);

        // when & then
        assertThrows(CouponException.AlreadyExhaustedException.class, () ->
                couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, couponCode))
        );
    }


    @Test
    @DisplayName("쿠폰 적용 성공")
    void applyCoupon_success() {
        // given
        Coupon coupon = createValidCoupon();
        Money orderAmount = Money.wons(10000);
        Money expectedDiscount = coupon.calculateDiscount(orderAmount);
        CouponIssue issue = CouponIssue.create(userId, coupon);

        given(couponRepository.findByCode(couponCode)).willReturn(coupon);
        given(couponIssueRepository.findByUserIdAndCouponId(userId, coupon.getId()))
                .willReturn(java.util.Optional.of(issue));

        // when
        ApplyCouponResult result = couponService.applyCoupon(
                new ApplyCouponCommand(userId, couponCode, orderAmount)
        );

        // then
        assertThat(result.couponCode()).isEqualTo(couponCode);
        assertThat(result.discountAmount()).isEqualTo(expectedDiscount);

    }

    @Test
    @DisplayName("쿠폰 미발급 상태에서 쿠폰 적용 시 예외 발생")
    void applyCoupon_fail_ifNotIssued() {
        // given
        Coupon coupon = createValidCoupon();

        given(couponRepository.findByCode(couponCode)).willReturn(coupon);
        given(couponIssueRepository.findByUserIdAndCouponId(userId, coupon.getId()))
                .willReturn(java.util.Optional.empty());  // 변경 포인트!

        // when & then
        assertThrows(CouponException.NotIssuedException.class, () ->
                couponService.applyCoupon(new ApplyCouponCommand(userId, couponCode, Money.wons(10000))));
    }
    private Coupon createValidCoupon() {
        return Coupon.create(
                couponCode,
                CouponType.PERCENTAGE,
                10,
                100,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
    }
}


