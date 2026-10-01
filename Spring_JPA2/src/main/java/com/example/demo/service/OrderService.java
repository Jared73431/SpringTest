package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.entity.Order;

/**
 * 訂單相關的商業邏輯：建立訂單、取消、狀態變更、增減訂單項，以及對應的庫存扣減／補回。
 * 以 interface + impl 分離，是早期 Spring 專案常見寫法；只有單一實作時直接使用 class 也可以。
 * 錯誤以例外表示：ResourceNotFoundException → 404、InvalidRequestException → 400、BusinessRuleViolationException → 409。
 */
public interface OrderService {

    /**
     * 建立訂單並扣減各商品庫存。
     * 數量 ≤ 0 或商品不存在 → InvalidRequestException（400）；庫存不足 → BusinessRuleViolationException（409）。
     * 任一商品失敗時整筆交易 rollback，不會留下部分扣減的庫存。
     */
    Order createOrder(String customerId, String shippingAddress, Map<String, Integer> productQuantities);

    /**
     * 取消訂單並補回庫存，是唯一能把訂單改成 CANCELLED 的途徑。
     * 訂單不存在 → 404；目前狀態不允許取消（依 Order.OrderStatus.canTransitionTo）→ 409。
     */
    Order cancelOrder(String orderId);

    /**
     * 依狀態機變更訂單狀態（不含取消）。
     * 訂單不存在 → 404；目標為 CANCELLED 或不合法的狀態轉換 → 409。
     */
    Order updateOrderStatus(String orderId, Order.OrderStatus status);

    /**
     * 在 PENDING 訂單中新增商品；若已存在則以新數量取代原數量並調整庫存。
     * 訂單不存在 → 404；數量 ≤ 0 或商品不存在 → 400；訂單非 PENDING 或庫存不足 → 409。
     */
    Order addOrderItem(String orderId, String productId, int quantity);

    /**
     * 從 PENDING 訂單移除商品並補回庫存。
     * 訂單或訂單項不存在 → 404；訂單非 PENDING → 409。
     */
    Order removeOrderItem(String orderId, String productId);

    /** 查詢訂單；不存在時拋出 ResourceNotFoundException（404）。 */
    Order findOrder(String orderId);

    /** 查詢客戶的所有訂單；查無資料時回傳空 List，而不是 404。 */
    List<Order> findCustomerOrders(String customerId);
}
