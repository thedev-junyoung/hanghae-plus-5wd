package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * 쿠폰 발급 엔티티 ( == UserCoupon)
 */
@Entity
@Table(name = "coupon_issue")
@Getter
@NoArgsConstructor
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private boolean isUsed = false;

    // 생성자
    public CouponIssue(Long userId, Coupon coupon) {
        this.userId = userId;
        this.coupon = coupon;
        this.issuedAt = LocalDateTime.now();
    }

    public static CouponIssue create(Long userId, Coupon coupon) {
        coupon.validateUsable();
        coupon.decreaseQuantity();
        return new CouponIssue(userId, coupon);
    }


    public void markAsUsed() {
        if (this.isUsed) {
            throw new CouponException.AlreadyIssuedException(userId, coupon.getCode());
        }
        this.isUsed = true;
    }

    /**
     * 유저가 발급받은 쿠폰의 사용 가능 여부를 검증
     * - 이미 사용한 쿠폰인지 확인
     * - 쿠폰 정책이 여전히 유효한지 확인(정책 위임)
     * 이 메서드는 "사용자 관점"에서 쿠폰을 실제 사용할 수 있는지를 판단하며,
     * 내부적으로는 정책 유효성 또한 포함
     */
    public void validateUsable() {
        if (isUsed) {
            throw new CouponException.AlreadyIssuedException(userId, coupon.getCode());
        }
        coupon.validateUsable(); // 쿠폰 정책에 위임
    }

}
