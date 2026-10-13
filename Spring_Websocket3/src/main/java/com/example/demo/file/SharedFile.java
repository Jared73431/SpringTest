package com.example.demo.file;

import java.time.Instant;
import java.util.UUID;

/**
 * shared_file 表的一列。storedName 是磁碟上的檔名（UUID），不對外公開。
 */
public record SharedFile(UUID id, String originalName, String storedName, long size, String uploadedBy,
		Instant uploadedAt) {

	/** 對外的資料：不包含磁碟上的檔名 */
	public record Info(UUID id, String name, long size, String uploadedBy, Instant uploadedAt, String downloadUrl) {
	}

	public Info info() {
		return new Info(id, originalName, size, uploadedBy, uploadedAt, "/api/files/" + id + "/content");
	}
}
