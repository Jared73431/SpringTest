package com.example.demo.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.compoundKey.OrderItemPK;
import com.example.demo.exception.BusinessRuleViolationException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;

/**
 * OrderService 的實作，方法說明請見 OrderService interface。
 * 類別層級 @Transactional：每個 public 方法都是一個交易，訂單與庫存的修改要嘛全部成功、要嘛全部 rollback。
 * 交易在方法結束時 commit，回傳的 Entity 會在 Controller 轉 DTO 時才載入 Lazy 關聯（依賴 Open Session In View）。
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final ProductRepository productRepository;

    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities) {
        // 先檢查所有數量，避免負數數量反而增加庫存
        productQuantities.forEach(this::requirePositiveQuantity);

        // 生成訂單ID
        String orderId = UUID.randomUUID().toString();

        // 建立訂單物件
        Order order = new Order(orderId, customerId);
        order.setShippingAddress(shippingAddress);

        // 保存訂單（使用 save 回傳的 managed 物件，之後加入的 OrderItem 都掛在它上面）
        order = orderRepository.save(order);

        // 新增訂單項
        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();

            // 查找商品：商品 ID 來自 Request Body 而非 URL，所以是 400 而不是 404
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new InvalidRequestException("商品不存在: " + productId));

            // 檢查庫存；在迴圈中途拋例外時，前面已保存的訂單與已扣的庫存都會隨交易 rollback
            if (product.getStock() < quantity) {
                throw new BusinessRuleViolationException("商品庫存不足: " + product.getName());
            }

            // 建立訂單項（addItem 同時維護 Order ↔ OrderItem 雙向關聯）
            OrderItem orderItem = new OrderItem(order, product, quantity);
            order.addItem(orderItem);

            // 減少商品庫存
            // [Learning] product 是交易內的 managed entity，commit 時 dirty checking 會自動 UPDATE，
            // 這裡的 save() 不是必要的，但明確寫出可讓讀者看出「庫存有被修改」
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        // 保存更新後的訂單
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(String orderId) {
        Order order = findOrder(orderId);

        // 依狀態機檢查是否可以取消（已取消的訂單不能再取消，避免重複補回庫存）
        if (!order.getStatus().canTransitionTo(Order.OrderStatus.CANCELLED)) {
            throw new BusinessRuleViolationException("訂單狀態為 " + order.getStatus() + "，無法取消");
        }

        // 更新訂單狀態
        order.setStatus(Order.OrderStatus.CANCELLED);

        // 恢復商品庫存：下單時扣掉的數量要還回去，否則取消的訂單會永久佔用庫存
        // Product 有 @Version，若同時有其他交易修改同一商品，commit 時會發生樂觀鎖衝突（409）
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        return orderRepository.save(order);
    }

    @Override
    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = findOrder(orderId);

        // 取消需要補回庫存，只能透過取消訂單 API
        if (status == Order.OrderStatus.CANCELLED) {
            throw new BusinessRuleViolationException("取消訂單請使用 POST /api/orders/{id}/cancel，才會補回庫存");
        }
        if (!order.getStatus().canTransitionTo(status)) {
            throw new BusinessRuleViolationException(
                    "訂單狀態無法從 " + order.getStatus() + " 變更為 " + status);
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public Order addOrderItem(String orderId, String productId, int quantity) {
        requirePositiveQuantity(productId, quantity);
        Order order = findOrder(orderId);

        // 檢查訂單狀態
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessRuleViolationException("只能修改待處理狀態的訂單");
        }

        // 查找商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new InvalidRequestException("商品不存在: " + productId));

        // 檢查庫存
        // [Potential Bug] 若訂單已包含該商品，可用庫存其實是「目前庫存 + 原數量」，
        // 但這裡只比較目前庫存，可能誤判庫存不足；修改前需先確認預期行為並補測試
        if (product.getStock() < quantity) {
            throw new BusinessRuleViolationException("商品庫存不足: " + product.getName());
        }

        // 檢查訂單是否已包含該商品
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        Optional<OrderItem> existingItem = orderItemRepository.findById(pk);

        if (existingItem.isPresent()) {
            // 更新已有訂單項的數量：新數量「取代」原數量（不是累加），所以庫存要先補回再扣
            OrderItem item = existingItem.get();
            // 先恢復原來的庫存
            product.setStock(product.getStock() + item.getQuantity());
            // 設置新數量
            item.setQuantity(quantity);
            // 再減去新的庫存
            product.reduceStock(quantity);
            productRepository.save(product);
        } else {
            // 建立新的訂單項
            OrderItem newItem = new OrderItem(order, product, quantity);
            order.addItem(newItem);
            // 減少商品庫存
            product.reduceStock(quantity);
            productRepository.save(product);
        }

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order removeOrderItem(String orderId, String productId) {
        Order order = findOrder(orderId);

        // 檢查訂單狀態
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessRuleViolationException("只能修改待處理狀態的訂單");
        }

        // 查找訂單項
        OrderItemPK pk = new OrderItemPK(orderId, productId);
        OrderItem item = orderItemRepository.findById(pk)
                .orElseThrow(() -> new ResourceNotFoundException("訂單項", productId));

        // 恢復商品庫存
        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);

        // 移除訂單項：先從 Order 的集合移除（維護雙向關聯），之後 recalculateTotalAmount 才不會算到它。
        // Order.items 有 orphanRemoval = true，flush 時本來就會 DELETE；這裡的 delete() 只是明確寫出意圖
        order.removeItem(item);
        orderItemRepository.delete(item);

        // 重新計算訂單總金額
        order.recalculateTotalAmount();

        return orderRepository.save(order);
    }

    @Override
    public Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單", orderId));
    }

    @Override
    public List<Order> findCustomerOrders(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    // 數量必須 > 0：負數會讓 reduceStock 反而增加庫存，因此必須在動到庫存之前先驗證
    private void requirePositiveQuantity(String productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidRequestException("商品數量必須大於 0: " + productId);
        }
    }
}
