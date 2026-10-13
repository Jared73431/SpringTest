package com.example.demo.file;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上傳流程：檢查 → 存檔 → 記錄到資料庫 → 通知所有人。
 */
@Service
public class FileService {

	public static final String NOTIFICATION_TOPIC = "/topic/files";

	private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{N}_-]{1,20}");

	private final FileStorage storage;

	private final SharedFileRepository repository;

	private final SimpMessagingTemplate messagingTemplate;

	public FileService(FileStorage storage, SharedFileRepository repository, SimpMessagingTemplate messagingTemplate) {
		this.storage = storage;
		this.repository = repository;
		this.messagingTemplate = messagingTemplate;
	}

	public SharedFile.Info upload(MultipartFile file, String uploadedBy) throws IOException {
		if (uploadedBy == null || !VALID_NAME.matcher(uploadedBy).matches()) {
			throw new IllegalArgumentException("Invalid uploader name");
		}
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}
		// 只取檔名的最後一段（有些瀏覽器會送完整路徑）；它只用來顯示與下載時的檔名
		String originalName = StringUtils.getFilename(StringUtils.cleanPath(String.valueOf(file.getOriginalFilename())));

		String storedName = storage.store(file.getInputStream());
		SharedFile saved;
		try {
			saved = repository.save(originalName, storedName, file.getSize(), uploadedBy);
		}
		catch (RuntimeException e) {
			// 資料庫寫入失敗：刪掉剛存的檔案，不留下沒有紀錄的檔案
			storage.delete(storedName);
			throw e;
		}

		var info = saved.info();
		// 通知所有開著頁面的人：檔案走 HTTP，「有新檔案」這件事走 WebSocket
		messagingTemplate.convertAndSend(NOTIFICATION_TOPIC, info);
		return info;
	}

	public List<SharedFile.Info> recent() {
		return repository.findRecent().stream().map(SharedFile::info).toList();
	}

	public SharedFile find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new FileNotFoundException(id));
	}

	public static class FileNotFoundException extends RuntimeException {

		public FileNotFoundException(UUID id) {
			super("No file " + id);
		}
	}
}
