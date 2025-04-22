package kr.hhplus.be.server.domain.coupon;

public interface CouponRepository {
    /**
     * 쿠폰을 저장하거나 업데이트한다.
     */
    void save(Coupon coupon);

    /**
     * 쿠폰을 삭제한다.
     */
    Coupon findByCode(String code); // 못 찾으면 CouponNotFoundException
}
