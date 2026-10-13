package com.example.demo.file;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SharedFileRepository {

	private final JdbcClient jdbcClient;

	public SharedFileRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	public SharedFile save(String originalName, String storedName, long size, String uploadedBy) {
		return jdbcClient.sql("""
				INSERT INTO shared_file (id, original_name, stored_name, size, uploaded_by)
				VALUES (:id, :originalName, :storedName, :size, :uploadedBy)
				RETURNING *
				""")
				.param("id", UUID.randomUUID())
				.param("originalName", originalName)
				.param("storedName", storedName)
				.param("size", size)
				.param("uploadedBy", uploadedBy)
				.query(SharedFileRepository::map)
				.single();
	}

	public Optional<SharedFile> findById(UUID id) {
		return jdbcClient.sql("SELECT * FROM shared_file WHERE id = :id").param("id", id)
				.query(SharedFileRepository::map).optional();
	}

	/** 最近上傳的 50 個，新的在前 */
	public List<SharedFile> findRecent() {
		return jdbcClient.sql("SELECT * FROM shared_file ORDER BY uploaded_at DESC LIMIT 50")
				.query(SharedFileRepository::map).list();
	}

	private static SharedFile map(ResultSet rs, int rowNum) throws SQLException {
		return new SharedFile(rs.getObject("id", UUID.class), rs.getString("original_name"),
				rs.getString("stored_name"), rs.getLong("size"), rs.getString("uploaded_by"),
				rs.getTimestamp("uploaded_at").toInstant());
	}
}
