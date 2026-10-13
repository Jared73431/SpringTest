package com.example.demo.report;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * 方法層級的權限（第 3 課）：權限檢查寫在「做事的方法」上，不論從哪個 Controller、哪個路徑呼叫都會檢查。
 */
@Service
public class ReportService {

	@PreAuthorize("hasRole('ADMIN')")
	public String salesReport() {
		return "Sales report: 42 books sold";
	}
}
