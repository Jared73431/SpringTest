package com.example.demo.book;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

	private final JdbcClient jdbcClient;

	public BookRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public Book insert(String title, String author) {
		return jdbcClient.sql("INSERT INTO book (title, author) VALUES (:title, :author) RETURNING *")
				.param("title", title)
				.param("author", author)
				.query(BookRepository::toBook)
				.single();
	}

	public Optional<Book> findById(long id) {
		return jdbcClient.sql("SELECT * FROM book WHERE id = :id").param("id", id).query(BookRepository::toBook).optional();
	}

	public List<Book> searchByTitle(String keyword) {
		return jdbcClient.sql("SELECT * FROM book WHERE title ILIKE :pattern ORDER BY id")
				.param("pattern", "%" + keyword + "%")
				.query(BookRepository::toBook)
				.list();
	}

	public List<Book> findAll() {
		return jdbcClient.sql("SELECT * FROM book ORDER BY id").query(BookRepository::toBook).list();
	}

	private static Book toBook(ResultSet rs, int rowNum) throws SQLException {
		return new Book(rs.getLong("id"), rs.getString("title"), rs.getString("author"),
				rs.getTimestamp("created_at").toInstant());
	}
}
