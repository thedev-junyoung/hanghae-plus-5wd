package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.common.vo.Money;
import kr.hhplus.be.server.domain.product.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductStockRepository productStockRepository;

    @Autowired
    ProductService productService;

    @Test
    @DisplayName("상품 상세 조회 시 재고 정보도 함께 반환된다")
    void getProductDetail_shouldIncludeStockQuantity() {
        // given
        Long productId = 1L;  // New Balance 993
        int size = 270;       // 재고 50개

        // when
        ProductDetailResult result = productService.getProductDetail(
                new GetProductDetailCommand(productId, size)
        );

        // then
        assertThat(result.product().stockQuantity()).isEqualTo(50);
        assertThat(result.product().name()).isEqualTo("New Balance 993");
    }
    @Test
    @DisplayName("상품 목록 조회 시 재고 정보가 포함된다")
    void getProductList_shouldIncludeStock() {
        // given
        GetProductListCommand command = new GetProductListCommand(0, 5, null);

        // when
        ProductListResult result = productService.getProductList(command);

        // then
        assertThat(result.products()).isNotEmpty();
        assertThat(result.products().get(0).stockQuantity()).isGreaterThanOrEqualTo(0);
    }
    @Test
    @DisplayName("상품 재고 차감이 성공하면 실제 수량이 줄어든다")
    void decreaseStock_shouldDeductStockQuantity() {
        // given
        Long productId = 1L; // New Balance 993
        int size = 270;
        int originalQuantity = productService.getProductDetail(
                new GetProductDetailCommand(productId, size)
        ).product().stockQuantity();

        int decreaseBy = 3;
        DecreaseStockCommand command = new DecreaseStockCommand(productId, size, decreaseBy);

        // when
        boolean result = productService.decreaseStock(command);

        // then
        assertThat(result).isTrue();

        int updatedQuantity = productService.getProductDetail(
                new GetProductDetailCommand(productId, size)
        ).product().stockQuantity();

        assertThat(updatedQuantity).isEqualTo(originalQuantity - decreaseBy);
    }
    @Test
    @DisplayName("출시 전 상품은 재고 차감 시 예외가 발생한다")
    void decreaseStock_shouldFailIfProductNotReleased() {
        // given
        Long productId = 12L; // 미래 출시일로 세팅된 On Cloudstratus
        int size = 270;

        ProductStock stock = productStockRepository.findByProductIdAndSize(productId, size)
                .orElseGet(() -> productStockRepository.save(ProductStock.of(productId, size, 10)));

        // when & then
        assertThatThrownBy(() ->
                productService.decreaseStock(new DecreaseStockCommand(stock.getProductId(), stock.getSize(), 1)))
                .isInstanceOf(ProductException.NotReleasedException.class);
    }


/**
 * 테스트 제거 이유:
 * 해당 테스트는 도메인 객체(Product)의 내부 검증 로직(validateOrderable)을 검증하고 있음.
 * 그러나 이는 DB와 연동된 통합 테스트 수준에서 다룰 필요가 없고,
 * 단위 테스트로 충분히 커버 가능하며,
 * 실제 주문 흐름(OrderFacade 등)에서도 간접적으로 검증되고 있음.
 * 또한, 테스트 대상인 `validateOrderable()`은 외부 의존성 없이 동작하는 순수 메서드이므로,
 * Product 클래스 자체의 단위 테스트에서 다루는 것이 적절하다.
 * 따라서 해당 테스트는 ProductServiceIntegrationTest에서 제거함.
 */

//
//    @Test
//    @DisplayName("출시 전 상품은 주문할 수 없다")
//    void validateOrderable_shouldFailIfNotReleased() {
//        // given: 실제 DB 사용 대신 따로 저장 (출시일 = 내일)
//        Product futureProduct = Product.create(
//                "조던 미래", "Nike", Money.wons(150000),
//                LocalDate.now().plusDays(1),
//                "fut.jpg", "미래 신발"
//        );
//        productRepository.save(futureProduct);
//
//        // when & then
//        assertThatThrownBy(() -> futureProduct.validateOrderable(1))
//                .isInstanceOf(ProductException.NotReleasedException.class);
//    }
//
//    @Test
//    @DisplayName("재고가 0이면 주문할 수 없다")
//    void validateOrderable_shouldFailIfOutOfStock() {
//        // given: 출시일은 과거지만 재고가 0
//        Product product = Product.create(
//                "조던 제로", "Nike", Money.wons(150000),
//                LocalDate.now().minusDays(1),
//                "zero.jpg", "없는 재고"
//        );
//        productRepository.save(product);
//
//        // when & then
//        assertThatThrownBy(() -> product.validateOrderable(0))
//                .isInstanceOf(ProductException.OutOfStockException.class);
//    }
}

