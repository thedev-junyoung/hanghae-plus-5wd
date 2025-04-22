package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.*;
import kr.hhplus.be.server.common.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductStockRepository productStockRepository;

    @InjectMocks
    StockService stockService;

    @Test
    @DisplayName("재고 차감 성공")
    void decrease_success() {
        // given
        Long productId = 1L;
        int size = 270;
        int quantity = 3;

        Product product = Product.create("Jordan 1", "Nike", Money.wons(200000),
                LocalDate.of(2024, 1, 1), "image.jpg", "desc");

        ProductStock stock = ProductStock.of(productId, size, 10);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productStockRepository.findByProductIdAndSizeForUpdate(productId, size))
                .thenReturn(Optional.of(stock));

        // when
        stockService.decrease(DecreaseStockCommand.of(productId, size, quantity));

        // then
        assertThat(stock.getStockQuantity()).isEqualTo(7); // 10 - 3
        verify(productStockRepository).save(stock);
    }

    @Test
    @DisplayName("재고 차감 실패 - 상품 없음")
    void decrease_fail_product_not_found() {
        // given
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        // expect
        assertThatThrownBy(() ->
                stockService.decrease(DecreaseStockCommand.of(1L, 270, 1)))
                .isInstanceOf(ProductException.NotFoundException.class);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 없음")
    void decrease_fail_stock_not_found() {
        // given
        Product product = Product.create("Jordan 1", "Nike", Money.wons(200000),
                LocalDate.of(2024, 1, 1), "image.jpg", "desc");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productStockRepository.findByProductIdAndSizeForUpdate(1L, 270))
                .thenReturn(Optional.empty());

        // expect
        assertThatThrownBy(() ->
                stockService.decrease(DecreaseStockCommand.of(1L, 270, 1)))
                .isInstanceOf(ProductException.InsufficientStockException.class);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void decrease_fail_insufficient() {
        // given
        Long productId = 1L;
        int size = 270;
        int quantity = 5;

        Product product = Product.create("Jordan 1", "Nike", Money.wons(200000),
                LocalDate.of(2024, 1, 1), "image.jpg", "desc");

        ProductStock stock = ProductStock.of(productId, size, 2); // 부족한 재고

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productStockRepository.findByProductIdAndSizeForUpdate(productId, size))
                .thenReturn(Optional.of(stock));

        // expect
        assertThatThrownBy(() ->
                stockService.decrease(DecreaseStockCommand.of(productId, size, quantity)))
                .isInstanceOf(ProductException.InsufficientStockException.class);
    }
}
