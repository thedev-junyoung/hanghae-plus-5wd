package kr.hhplus.be.server.application.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponIssueRepository;
import kr.hhplus.be.server.domain.coupon.CouponRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * 동시성 이슈를 재현하기 위한 통합 테스트 클래스.
 *
 * <p>이 테스트는 10명의 사용자가 동시에 같은 쿠폰을 발급받으려 할 때 발생할 수 있는
 * 재고 차감 관련 동시성 문제를 검증한다.</p>
 *
 * <p>시나리오:</p>
 * <ul>
 *   <li>초기 쿠폰 수량은 2개</li>
 *   <li>각 사용자당 1개씩 발급 요청</li>
 *   <li>10명의 사용자가 동시에 쿠폰을 요청하면 이론상 최대 2명만 성공 가능</li>
 * </ul>
 *
 * <p>기대 결과:</p>
 * <ul>
 *   <li>발급된 쿠폰 수는 2건 이하</li>
 * </ul>
 *
 * <p>⚠️ 해당 테스트는 동시성 처리를 하지 않은 상태에서 실패해야 정상이다.
 */
@SpringBootTest
public class CouponConcurrencyTest {


    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponService couponService;

    private static final String COUPON_CODE = "LIMITED1000-" + UUID.randomUUID();
    private static final int TOTAL_QUANTITY = 2;
    private static final int CONCURRENCY = 10;

    @BeforeEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setUp() {
        // 초기화: 쿠폰 직접 저장
        Coupon coupon = Coupon.createLimitedFixed(COUPON_CODE, 1000, TOTAL_QUANTITY,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30));
        couponRepository.save(coupon);
        System.out.println("테스트 시작 전 쿠폰 저장 완료");
    }

    @Test
    @DisplayName("여러 명이 동시에 쿠폰을 요청하면 수량 초과 발급이 발생할 수 있다")
    void should_fail_on_race_condition_when_multiple_users_issue_coupon_concurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(CONCURRENCY);

        List<Long> successUsers = new CopyOnWriteArrayList<>();
        List<Long> failedUsers = new CopyOnWriteArrayList<>();

        for (int i = 0; i < CONCURRENCY; i++) {
            long userId = 100L + i;
            executor.execute(() -> {
                try {
                    System.out.printf("사용자 %d → 쿠폰 요청\n", userId);
                    couponService.issueLimitedCoupon(new IssueLimitedCouponCommand(userId, COUPON_CODE));
                    successUsers.add(userId);
                    System.out.printf("사용자 %d → 쿠폰 발급 성공\n", userId);
                } catch (Exception e) {
                    failedUsers.add(userId);
                    System.out.printf("/사용자 %d → 발급 실패 (%s: %s)\n",
                            userId, e.getClass().getSimpleName(), e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long totalIssued = couponIssueRepository.countByCouponCode(COUPON_CODE);

        System.out.println("\n=== ✅ 결과 요약 ===");
        System.out.printf("총 요청 수: %d명\n", CONCURRENCY);
        System.out.printf("총 발급 성공 수: %d명\n", successUsers.size());
        System.out.printf("총 발급 실패 수: %d명\n", failedUsers.size());
        System.out.printf("DB 기준 실제 발급 수량: %d개\n", totalIssued);
        System.out.printf("발급 성공자: %s\n", successUsers);

        // ✅ 이 테스트는 동시성 문제가 발생해야 정상
        assertThat(totalIssued)
                .withFailMessage(" 예상보다 많은 쿠폰이 발급되었습니다. 동시성 제어가 필요합니다.")
                .isLessThanOrEqualTo(TOTAL_QUANTITY);
    }
}
