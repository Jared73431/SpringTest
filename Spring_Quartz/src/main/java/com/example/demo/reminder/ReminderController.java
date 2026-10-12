package com.example.demo.reminder;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 修正前：/quartz/add、/remove、/start、/shutdown 全部是 GET（瀏覽器預先載入或爬蟲造訪就會觸發），
 * 名稱與 cron 寫死，失敗也回傳 "ok"。
 */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

	private final ReminderService reminderService;

	public ReminderController(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@PostMapping
	public ResponseEntity<ReminderResponse> create(@Valid @RequestBody ReminderRequest request) {
		ReminderResponse created = reminderService.create(request);
		return ResponseEntity.created(URI.create("/api/reminders/" + created.name())).body(created);
	}

	@GetMapping
	public List<ReminderResponse> findAll() {
		return reminderService.findAll();
	}

	@GetMapping("/{name}")
	public ReminderResponse find(@PathVariable String name) {
		return reminderService.find(name);
	}

	@PutMapping("/{name}/schedule")
	public ReminderResponse reschedule(@PathVariable String name, @Valid @RequestBody RescheduleRequest request) {
		return reminderService.reschedule(name, request);
	}

	@PostMapping("/{name}/pause")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void pause(@PathVariable String name) {
		reminderService.pause(name);
	}

	@PostMapping("/{name}/resume")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resume(@PathVariable String name) {
		reminderService.resume(name);
	}

	// 202：已排入執行，實際由 Quartz 的執行緒非同步執行
	@PostMapping("/{name}/run")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void runNow(@PathVariable String name) {
		reminderService.runNow(name);
	}

	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String name) {
		reminderService.delete(name);
	}
}
