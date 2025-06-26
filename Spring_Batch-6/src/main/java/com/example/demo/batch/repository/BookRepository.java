package com.example.demo.batch.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.batch.entity.Book;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * 查找指定年份之後出版的書籍
     */
    @Query("SELECT b FROM Book b WHERE b.year >= :year ORDER BY b.year, b.id")
    List<Book> findBooksAfterYear(@Param("year") Integer year);

    /**
     * 查找價格範圍內的書籍
     */
    @Query("SELECT b FROM Book b WHERE b.cost BETWEEN :minCost AND :maxCost ORDER BY b.cost")
    List<Book> findBooksByPriceRange(@Param("minCost") Double minCost, @Param("maxCost") Double maxCost);

    /**
     * 查找特定作者的書籍
     */
    @Query("SELECT b FROM Book b WHERE b.author LIKE %:author% ORDER BY b.author, b.title")
    List<Book> findBooksByAuthor(@Param("author") String author);

    /**
     * 統計總書籍數量
     */
    @Query("SELECT COUNT(b) FROM Book b")
    Long countAllBooks();
}
