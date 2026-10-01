package com.example.demo.entity;

import java.math.BigDecimal;

import com.example.demo.entity.compoundKey.OrderItemPK;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 訂單項：Order 與 Product 多對多關聯的「中間 Entity」，額外記錄數量與下單當時的單價。
 * 主鍵是 (order_id, product_id) 的複合主鍵 {@link OrderItemPK}，
 * 以 @EmbeddedId + @MapsId 讓主鍵欄位與兩個外鍵共用同一組資料表欄位。
 */
@Entity
@Getter
@Setter
@Table(name = "order_item")
public class OrderItem {

    // @EmbeddedId：主鍵是一個 @Embeddable 物件，而不是單一欄位
    @EmbeddedId
    private OrderItemPK id;

    // @MapsId：此關聯的外鍵值同時作為 id 中對應屬性的值，
    // 因此 order_id 只會有一個欄位，不會出現「主鍵欄位」和「外鍵欄位」重複對應的衝突。
    // @ManyToOne 預設 FetchType.EAGER，載入訂單項時會一併載入 Order 與 Product
    @ManyToOne
    @MapsId("orderId") // 映射到複合主鍵的orderId欄位
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    @ManyToOne
    @MapsId("productId") // 映射到複合主鍵的productId欄位
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 建構子
    public OrderItem() {}

    // 單價在建立時從商品複製一份（價格快照），日後商品調價不會影響已成立的訂單金額
    public OrderItem(Order order, Product product, Integer quantity) {
        this.order = order;
        this.product = product;
        this.id = new OrderItemPK(order.getId(), product.getId());
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    // 便利方法：計算小計金額
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(new BigDecimal(quantity));
    }
}
