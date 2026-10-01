package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Order;

/**
 * 訂單的 Repository，主鍵型別為 String（UUID）。
 * 大部分查詢使用方法名稱推導（derived query），無法表達的條件才使用 @Query JPQL。
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // 查找某個客戶的所有訂單
    List<Order> findByCustomerId(String customerId);

    // 查找某個狀態的訂單
    List<Order> findByStatus(Order.OrderStatus status);

    // 查找某個日期範圍的訂單
    List<Order> findByOrderDateBetween(Date startDate, Date endDate);

    // 查找超過某個金額的訂單
    List<Order> findByTotalAmountGreaterThan(BigDecimal amount);

    // 自定義查詢：查找包含特定商品的訂單
    // JOIN 一對多的 items 時，一張訂單有多個符合的訂單項就會重複出現，因此加上 DISTINCT
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.id = :productId")
    List<Order> findOrdersContainingProduct(@Param("productId") String productId);
}
