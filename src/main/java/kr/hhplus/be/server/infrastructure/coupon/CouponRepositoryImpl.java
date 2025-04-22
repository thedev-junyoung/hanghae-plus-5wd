package kr.hhplus.be.server.infrastructure.coupon;


import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.CouponException;
import kr.hhplus.be.server.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository jpaRepository;

    @Override
    public void save(Coupon coupon) {
        jpaRepository.save(coupon);
    }

    @Override
    public Coupon findByCode(String code) {
        return jpaRepository.findByCode(code)
                .orElseThrow(() -> new CouponException.NotFoundException(code));
    }
}
