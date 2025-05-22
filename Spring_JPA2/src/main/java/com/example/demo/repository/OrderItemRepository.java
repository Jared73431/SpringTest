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

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemPK> {

    // 查找某个订单的所有订单项
    List<OrderItem> findByOrderId(String orderId);

    // 查找包含某个商品的所有订单项
    List<OrderItem> findByProductId(String productId);

    // 查找数量大于指定值的订单项
    List<OrderItem> findByQuantityGreaterThan(Integer quantity);

    // 自定义查询：查找某个时间段内售出数量最多的商品
    @Query("SELECT i.product.id, SUM(i.quantity) as total FROM OrderItem i " +
            "JOIN i.order o WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "GROUP BY i.product.id ORDER BY total DESC")
    List<Object[]> findBestSellingProducts(@Param("startDate") Date startDate,
                                           @Param("endDate") Date endDate,
                                           Pageable pageable);
}
