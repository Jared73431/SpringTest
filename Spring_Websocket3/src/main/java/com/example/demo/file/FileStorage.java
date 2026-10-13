package com.example.demo.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 把檔案內容存在磁碟上的一個目錄。
 *
 * <p>
 * 磁碟上的檔名一律是<b>新產生的 UUID</b>，不使用使用者提供的檔名：檔名可能是 "../../etc/passwd" 這類路徑（路徑穿越），
 * 也可能和別人的檔案重複。原本的檔名只存在資料庫，下載時再放回 Content-Disposition。
 */
@Component
public class FileStorage {

	private final Path root;

	public FileStorage(@Value("${app.storage.directory}") String directory) throws IOException {
		this.root = Path.of(directory).toAbsolutePath().normalize();
		Files.createDirectories(root);
	}

	/** 存檔，回傳磁碟上的檔名 */
	public String store(InputStream content) throws IOException {
		String storedName = UUID.randomUUID().toString();
		Files.copy(content, resolve(storedName));
		return storedName;
	}

	public Path resolve(String storedName) {
		Path path = root.resolve(storedName).normalize();
		// 多一層保險：解析後的路徑必須還在存放目錄裡
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Invalid file name");
		}
		return path;
	}

	public void delete(String storedName) {
		try {
			Files.deleteIfExists(resolve(storedName));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
