package kr.hhplus.be.server.application.order;

import kr.hhplus.be.server.domain.order.OrderRepository;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.domain.product.ProductRepository;
import kr.hhplus.be.server.domain.product.ProductStock;
import kr.hhplus.be.server.domain.product.ProductStockRepository;
import kr.hhplus.be.server.common.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;



/**
 * 동시성 이슈를 재현하기 위한 통합 테스트 클래스.
 *
 * <p>이 테스트는 3명의 사용자가 동시에 같은 상품을 주문할 때 발생할 수 있는
 * 재고 차감 관련 동시성 문제를 검증한다.</p>
 *
 * <p>시나리오:</p>
 * <ul>
 *   <li>초기 재고는 10개</li>
 *   <li>각 사용자당 5개씩 주문</li>
 *   <li>3명의 사용자가 동시에 주문을 시도하면 이론상 최대 2명만 성공 가능</li>
 * </ul>
 *
 * <p>기대 결과:</p>
 * <ul>
 *   <li>성공한 주문 수는 2건</li>
 *   <li>남은 재고는 0</li>
 *   <li>1명은 재고 부족 등의 이유로 주문 실패</li>
 * </ul>
 *
 * <p>⚠️ 해당 테스트는 동시성 처리를 하지 않은 상태에서 실패해야 정상이다.
 */
@SpringBootTest
public class OrderConcurrencyTest {

    @Autowired
    private OrderFacadeService orderFacadeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductStockRepository stockRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long productId;

    private static final int INIT_STOCK = 10;
    private static final int ORDER_QTY = 5;
    private static final int CONCURRENCY = 3;

    @BeforeEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setUp() {
        // 트랜잭션 없이 바로 DB 반영됨
        Product product = Product.create(
                "Test Product", "TestBrand", Money.wons(10000),
                LocalDate.now().minusDays(1), null, null
        );
        product = productRepository.save(product);

        stockRepository.save(ProductStock.of(product.getId(), 270, INIT_STOCK));

        this.productId = product.getId();
    }

    @Test
    @DisplayName("동시에 여러 명이 주문하면 재고가 정확히 차감되어야 한다")
    void should_decrease_stock_correctly_when_multiple_orders_are_placed_concurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch latch = new CountDownLatch(CONCURRENCY);

        // 💡 테스트 전에 초기 주문 수 저장
        long beforeCount = orderRepository.count();
        ProductStock initStock = stockRepository.findByProductIdAndSize(productId, 270)
                .orElseThrow(() -> new IllegalStateException("재고 없음"));

        System.out.println("초기 재고: " + initStock.getStockQuantity());

        for (int i = 0; i < CONCURRENCY; i++) {
            long userId = 100L + i;
            executor.execute(() -> {
                try {
                    CreateOrderCommand command = new CreateOrderCommand(
                            userId,
                            List.of(new CreateOrderCommand.OrderItemCommand(productId, ORDER_QTY, 270)),
                            null
                    );
                    orderFacadeService.createOrder(command);
                } catch (Exception e) {
                    System.out.println("주문 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 재고 확인
        ProductStock stock = stockRepository.findByProductIdAndSize(productId, 270)
                .orElseThrow(() -> new IllegalStateException("재고 없음"));

        // 최종 주문 수 측정
        long afterCount = orderRepository.count();
        long diff = afterCount - beforeCount;

        System.out.println("남은 재고: " + stock.getStockQuantity());
        System.out.println("신규 주문 수: " + diff);

        assertThat(stock.getStockQuantity()).isEqualTo(0);
        assertThat(diff).isEqualTo(2);
    }}
