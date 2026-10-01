package com.example.demo.controller;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.Order;
import com.example.demo.request.AddOrderItemRequest;
import com.example.demo.request.CreateOrderRequest;
import com.example.demo.service.OrderService;

import jakarta.validation.Valid;

/**
 * 訂單 REST API：Controller 只負責 HTTP 轉換，商業邏輯與交易都在 OrderService。
 * 不在這裡 try/catch，Service 拋出的例外由 GlobalExceptionHandler 統一轉成 ProblemDetail。
 * 已知限制：Entity 轉 OrderDTO 發生在 Service 交易結束後，讀取 LAZY 關聯依賴 Open Session In View。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /api/orders/{id}：取得單筆訂單（含訂單項）。
     * 200；訂單不存在 → 404。
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String id) {
        Order order = orderService.findOrder(id);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    /**
     * GET /api/orders/customer/{customerId}：取得客戶的所有訂單。
     * 200；客戶沒有訂單時回傳空陣列（Order 只存 customerId，沒有客戶資源可用來判斷 404）。
     * [Learning] 每筆訂單轉 DTO 時會 LAZY 載入訂單項，訂單數多時會產生 N+1 查詢。
     */
    @GetMapping("/customer/{customerId}")
    public List<OrderDTO> getCustomerOrders(@PathVariable String customerId) {
        return orderService.findCustomerOrders(customerId).stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * POST /api/orders：建立訂單並扣減庫存。
     * 201 + Location；驗證失敗、數量 ≤ 0 或商品不存在 → 400；庫存不足 → 409。
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(
                request.getCustomerId(),
                request.getShippingAddress(),
                request.getProductQuantities());

        return ResponseEntity.created(URI.create("/api/orders/" + order.getId()))
                .body(new OrderDTO(order));
    }

    /**
     * POST /api/orders/{id}/cancel：取消訂單並補回庫存。
     * 用 POST 表示「動作」而不是 PUT status，因為取消還帶有補回庫存的副作用。
     * 200；訂單不存在 → 404；目前狀態不可取消 → 409。
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable String id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    /**
     * PUT /api/orders/{id}/status?status=XXX：依狀態機變更訂單狀態。
     * 200；訂單不存在 → 404；status 缺少或不是合法的列舉值 → 400；目標為 CANCELLED 或不合法的狀態轉換 → 409。
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(new OrderDTO(order));
    }

    /**
     * POST /api/orders/{id}/items：新增商品到訂單；商品已存在時以新數量取代。
     * 200（回傳整張訂單）；訂單不存在 → 404；驗證失敗或商品不存在 → 400；訂單非 PENDING 或庫存不足 → 409。
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDTO> addOrderItem(
            @PathVariable String id,
            @Valid @RequestBody AddOrderItemRequest request) {
        Order order = orderService.addOrderItem(id, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(new OrderDTO(order));
    }

    /**
     * DELETE /api/orders/{orderId}/items/{productId}：從訂單移除商品並補回庫存。
     * 200（回傳更新後的訂單而非 204，方便用戶端取得新的總金額）；訂單或訂單項不存在 → 404；訂單非 PENDING → 409。
     */
    @DeleteMapping("/{orderId}/items/{productId}")
    public ResponseEntity<OrderDTO> removeOrderItem(
            @PathVariable String orderId,
            @PathVariable String productId) {
        Order order = orderService.removeOrderItem(orderId, productId);
        return ResponseEntity.ok(new OrderDTO(order));
    }
}
