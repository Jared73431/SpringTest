package com.example.demo.batch.model;

/**
 * 寫進 person 表的資料（Processor 的輸出）。
 *
 * <p>
 * 修正前是 JPA Entity（Lombok @Data）：只為了 insert 而引入 JPA，表由 ddl-auto 建立。改用 JdbcBatchItemWriter 後，
 * 一個 record 就夠了。
 */
public record Person(String firstName, String lastName) {
}
