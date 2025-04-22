package kr.hhplus.be.server.infrastructure.product;

import kr.hhplus.be.server.domain.product.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductStockJpaRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByProductIdAndSize(Long productId, int size);
    Optional<ProductStock> findByProductId(Long productId); // 구현 목적에 따라 다르게 쿼리 가능

    List<ProductStock> findAllByProductId(Long productId);
}
