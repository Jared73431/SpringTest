package com.example.demo.file;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 檔案的上傳、列表、下載都是一般的 HTTP。
 *
 * <pre>
 * POST /api/files                上傳（multipart：file、uploadedBy）→ 201 + Location，並通知 /topic/files
 * GET  /api/files                最近上傳的 50 個
 * GET  /api/files/{id}           檔案的資料
 * GET  /api/files/{id}/content   下載
 * </pre>
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

	/**
	 * 下載時的 Content-Type 由<b>伺服器</b>依副檔名決定，只認得這幾種，其他一律 application/octet-stream。
	 * 修正前直接相信上傳時客戶端宣稱的 Content-Type，那是可以偽造的。
	 */
	private static final Map<String, MediaType> KNOWN_TYPES = Map.of(
			"png", MediaType.IMAGE_PNG,
			"jpg", MediaType.IMAGE_JPEG,
			"jpeg", MediaType.IMAGE_JPEG,
			"gif", MediaType.IMAGE_GIF,
			"pdf", MediaType.APPLICATION_PDF,
			"txt", MediaType.TEXT_PLAIN);

	private final FileService fileService;

	private final FileStorage storage;

	public FileController(FileService fileService, FileStorage storage) {
		this.fileService = fileService;
		this.storage = storage;
	}

	@PostMapping
	public ResponseEntity<SharedFile.Info> upload(@RequestParam MultipartFile file, @RequestParam String uploadedBy)
			throws IOException {
		var info = fileService.upload(file, uploadedBy);
		return ResponseEntity.created(URI.create("/api/files/" + info.id())).body(info);
	}

	@GetMapping
	public List<SharedFile.Info> recent() {
		return fileService.recent();
	}

	@GetMapping("/{id}")
	public SharedFile.Info info(@PathVariable UUID id) {
		return fileService.find(id).info();
	}

	/**
	 * 一律當成附件下載（attachment）：瀏覽器存檔而不是直接打開，就算有人上傳了 HTML，也不會在我們的網站上執行。
	 * nosniff：要求瀏覽器照我們給的 Content-Type，不要自己猜。
	 */
	@GetMapping("/{id}/content")
	public ResponseEntity<Resource> download(@PathVariable UUID id) {
		var file = fileService.find(id);
		return ResponseEntity.ok()
				.contentType(contentType(file.originalName()))
				.contentLength(file.size())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.originalName(), StandardCharsets.UTF_8).build()
								.toString())
				.header("X-Content-Type-Options", "nosniff")
				.body(new FileSystemResource(storage.resolve(file.storedName())));
	}

	private static MediaType contentType(String fileName) {
		String extension = StringUtils.getFilenameExtension(fileName);
		return extension == null ? MediaType.APPLICATION_OCTET_STREAM
				: KNOWN_TYPES.getOrDefault(extension.toLowerCase(Locale.ROOT), MediaType.APPLICATION_OCTET_STREAM);
	}
}
