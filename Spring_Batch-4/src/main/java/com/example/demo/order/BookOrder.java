package com.example.demo.order;

/** book_order 表的一列 */
public record BookOrder(Long id, Long bookId, int quantity, String status) {
}
