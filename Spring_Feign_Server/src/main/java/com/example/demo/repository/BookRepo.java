package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Book;

/**
 * 書籍的 Repository。只要繼承 JpaRepository&lt;Entity, 主鍵型別&gt;，Spring Data JPA 就會在執行期
 * 自動產生實作，提供 save、findById、findAll、existsById、deleteById 等 CRUD 方法，不需要自己寫 SQL。
 */
@Repository
public interface BookRepo extends JpaRepository<Book, Integer> {

}
