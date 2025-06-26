package com.example.demo.batch.dto;

import java.io.Serializable;

/**
 * Book 數據傳輸對象 - 用於批次處理的中間數據格式
 */
public record BookDTO(Integer id,
                      Integer isbn,
                      String title,
                      String author,
                      Integer year,
                      String publisher,
                      Double cost,
                      String category, // 新增分類字段
                      Boolean isProcessed // 處理狀態標記
)implements Serializable {

    /**
     * 從 Book entity 創建 BookDTO
     */
    public static BookDTO fromEntity(com.example.demo.batch.entity.Book book) {
        return new BookDTO(
                book.getId(),
                book.getISBN(),
                book.getTitle(),
                book.getAuthor(),
                book.getYear(),
                book.getPublisher(),
                book.getCost(),
                null, // 待處理時分類
                false // 初始未處理狀態
        );
    }

    /**
     * 轉換為 Book entity
     */
    public com.example.demo.batch.entity.Book toEntity() {
        com.example.demo.batch.entity.Book book = new com.example.demo.batch.entity.Book();
        book.setId(this.id);
        book.setISBN(this.isbn);
        book.setTitle(this.title);
        book.setAuthor(this.author);
        book.setYear(this.year);
        book.setPublisher(this.publisher);
        book.setCost(this.cost);
        return book;
    }

}
