package com.example.demo.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.OrderItem;
import com.example.demo.entity.compoundKey.OrderItemPK;

/**
 * 訂單項的 Repository。因為 OrderItem 使用複合主鍵，JpaRepository 的 ID 型別是 {@link OrderItemPK}。
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemPK> {

    // 查找某個訂單的所有訂單項
    // 屬性路徑 order.id：Spring Data 會把 OrderId 解析成「order 關聯的 id 屬性」
    List<OrderItem> findByOrderId(String orderId);

    // 查找包含某個商品的所有訂單項
    List<OrderItem> findByProductId(String productId);

    // 查找數量大於指定值的訂單項
    List<OrderItem> findByQuantityGreaterThan(Integer quantity);

    // 自定義查詢：查找某個時間段內售出數量最多的商品
    // 只 SELECT 部分欄位，所以回傳 Object[]（[0] = 商品 ID、[1] = 總數量），使用時需自行轉型；
    // 也可改用介面投影或 record 讓型別更明確。Pageable 用來限制筆數（取前 N 名）
    @Query("SELECT i.product.id, SUM(i.quantity) as total FROM OrderItem i " +
            "JOIN i.order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY i.product.id ORDER BY total DESC")
    List<Object[]> findBestSellingProducts(@Param("startDate") Date startDate,
                                           @Param("endDate") Date endDate,
                                           Pageable pageable);
}
