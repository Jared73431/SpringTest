package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 訂單（Aggregate Root）：透過 {@link OrderItem} 與 Product 形成「帶額外欄位的多對多」
 * （數量、單價無法放在單純的 @ManyToMany 中間表，所以拆成獨立的 Entity）。
 * 訂單項的新增 / 移除與總金額計算集中在本類別的便利方法中，確保資料一致。
 */
@Entity
@Getter
@Setter
@Table(name = "orders") // 使用"orders"而不是"order"，因為"order"是SQL關鍵字
public class Order {

    // 沒有 @GeneratedValue：主鍵由應用程式自行指定（長度 36 對應 UUID 字串格式）
    @Id
    @Column(name = "order_id", length = 36)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "order_date", nullable = false)
    private Date orderDate;

    // EnumType.STRING 以名稱（如 "PENDING"）存入資料庫；
    // 預設的 ORDINAL 存的是順序數字，日後調整 enum 順序或插入新值會讓舊資料對應錯誤
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    // mappedBy = "order"：外鍵在 order_item 表，由 OrderItem.order 維護（多的一方是擁有方）。
    // cascade = ALL：儲存 / 刪除訂單時連同訂單項一起處理，訂單項的生命週期完全依附訂單。
    // orphanRemoval = true：從 items 移除的訂單項（成為孤兒）會在 flush 時自動 DELETE。
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /**
     * 訂單狀態與允許的轉換（狀態機）。
     *
     * <pre>
     *   PENDING ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
     *      │             │
     *      └──────┬──────┘
     *             ▼
     *         CANCELLED
     * </pre>
     *
     * <ul>
     *   <li>只能依箭頭方向前進一步，不能跳過狀態，也不能倒退</li>
     *   <li>只有 PENDING、PROCESSING 可以取消；取消必須透過「取消訂單」API，才會補回庫存</li>
     *   <li>DELIVERED、CANCELLED 為最終狀態，之後不能再變更</li>
     * </ul>
     *
     * 規則集中定義在這裡，Service 只需要呼叫 {@link #canTransitionTo(OrderStatus)}，
     * 不必在每個方法各自判斷「哪些狀態可以做什麼」。
     */
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED;

        /** 是否可以從目前的狀態轉換到 target */
        public boolean canTransitionTo(OrderStatus target) {
            return switch (this) {
                case PENDING -> target == PROCESSING || target == CANCELLED;
                case PROCESSING -> target == SHIPPED || target == CANCELLED;
                case SHIPPED -> target == DELIVERED;
                case DELIVERED, CANCELLED -> false;
            };
        }
    }

    // 建構子
    public Order() {
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING;
    }

    public Order(String id, String customerId) {
        this();
        this.id = id;
        this.customerId = customerId;
    }

    // 便利方法：新增訂單項
    // 同時設定兩側（items 與 item.order）：外鍵由 OrderItem.order 決定，只加入集合不會寫入正確的 order_id
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);

        // 重新計算訂單總金額
        recalculateTotalAmount();
    }

    // 便利方法：移除訂單項
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);

        // 重新計算訂單總金額
        recalculateTotalAmount();
    }

    // 便利方法：計算訂單總金額
    // 以 BigDecimal 累加，避免 double 的浮點誤差；修改訂單項數量後也需要呼叫以保持總額正確
    public void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
