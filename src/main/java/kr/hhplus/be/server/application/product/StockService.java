package kr.hhplus.be.server.application.product;

import kr.hhplus.be.server.domain.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(DecreaseStockCommand command) {
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductException.NotFoundException(command.productId()));

        ProductStock stock = productStockRepository.findByProductIdAndSizeForUpdate(command.productId(), command.size())
                .orElseThrow(ProductException.InsufficientStockException::new);

        product.validateOrderable(stock.getStockQuantity());
        stock.decreaseStock(command.quantity());
        productStockRepository.save(stock);
    }
}
