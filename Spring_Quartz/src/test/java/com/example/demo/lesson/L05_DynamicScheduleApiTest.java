package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.demo.PostgresContainerTestBase;
import com.example.demo.notification.NotificationService;

/**
 * 第 5 課：動態排程 API —— 執行中新增、修改、暫停、刪除排程，不需要重新部署。
 *
 * <p>
 * 每個提醒是一個 JobDetail（帶著自己的資料）+ 一個 CronTrigger，存在資料庫中。「每家門市一個排程，打烊時間改了就重新排程」
 * 就是這個做法。
 *
 * <pre>
 * POST   /api/reminders                  新增（201 + Location）
 * GET    /api/reminders                  全部
 * GET    /api/reminders/{name}           單一提醒（狀態、下次 / 上次觸發時間）
 * PUT    /api/reminders/{name}/schedule  修改時間（rescheduleJob）
 * POST   /api/reminders/{name}/pause     暫停（pauseJob）
 * POST   /api/reminders/{name}/resume    恢復（resumeJob）
 * POST   /api/reminders/{name}/run       立刻執行一次（triggerJob）
 * DELETE /api/reminders/{name}           刪除（deleteJob）
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class L05_DynamicScheduleApiTest extends PostgresContainerTestBase {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Scheduler scheduler;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void deleteAllReminders() throws Exception {
		scheduler.deleteJobs(java.util.List.copyOf(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("reminders"))));
		notificationService.clear();
	}

	private ResultActions create(String name, String cron) throws Exception {
		return mockMvc.perform(post("/api/reminders").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"%s","cron":"%s","recipient":"taipei-101@example.com","message":"打烊後盤點"}
				""".formatted(name, cron)));
	}

	@Test
	void create_shouldReturn201AndStoreScheduleInDatabase() throws Exception {
		create("store-101-closing", "0 30 22 * * ?")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/reminders/store-101-closing"))
				.andExpect(jsonPath("$.cron").value("0 30 22 * * ?"))
				.andExpect(jsonPath("$.timeZone").value("Asia/Taipei"))
				.andExpect(jsonPath("$.state").value("NORMAL"))
				.andExpect(jsonPath("$.nextFireTime", notNullValue()));

		// JDBC JobStore：排程存在 qrtz_* 資料表中
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM qrtz_job_details WHERE job_name = 'store-101-closing'", Integer.class)).isEqualTo(1);
	}

	// 修正前名稱寫死成 myJob：第二次新增失敗，卻仍然回傳 ok
	@Test
	void create_shouldReturn409_whenNameExists() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");

		create("store-101-closing", "0 0 23 * * ?").andExpect(status().isConflict());
	}

	// Quartz 的 cron 規定「日」和「星期」其中一個要寫 ?（和 Spring @Scheduled 的 cron 不同）
	@Test
	void create_shouldReturn400_whenCronIsNotQuartzSyntax() throws Exception {
		create("store-101-closing", "0 30 22 * * *")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("cron 格式不正確：0 30 22 * * *"));
	}

	@Test
	void create_shouldReturn400_whenNameHasInvalidCharacters() throws Exception {
		create("Store 101", "0 30 22 * * ?").andExpect(status().isBadRequest());
	}

	@Test
	void find_shouldReturn404_whenMissing() throws Exception {
		mockMvc.perform(get("/api/reminders/missing")).andExpect(status().isNotFound());
	}

	@Test
	void findAll_shouldListReminders() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");
		create("store-102-closing", "0 0 23 * * ?");

		mockMvc.perform(get("/api/reminders"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("store-101-closing"));
	}

	// 門市改了打烊時間：替換 Trigger，Job 與它的資料不變
	@Test
	void reschedule_shouldReplaceTheTrigger() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");

		mockMvc.perform(put("/api/reminders/store-101-closing/schedule").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cron":"0 0 23 * * ?","timeZone":"Asia/Tokyo"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cron").value("0 0 23 * * ?"))
				.andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"))
				.andExpect(jsonPath("$.recipient").value("taipei-101@example.com"));
	}

	// 修正前用 /quartz/shutdown：永久關閉整個 Scheduler，之後 /quartz/start 回傳 500
	@Test
	void pauseAndResume_shouldChangeTheState() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");

		mockMvc.perform(post("/api/reminders/store-101-closing/pause")).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/reminders/store-101-closing")).andExpect(jsonPath("$.state").value("PAUSED"));

		mockMvc.perform(post("/api/reminders/store-101-closing/resume")).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/reminders/store-101-closing")).andExpect(jsonPath("$.state").value("NORMAL"));
	}

	// 立刻執行一次：Job 由 Spring 建立，注入了 NotificationService，JobDataMap 的資料透過 setter 注入
	@Test
	void runNow_shouldExecuteTheJobWithItsData() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");

		mockMvc.perform(post("/api/reminders/store-101-closing/run")).andExpect(status().isAccepted());

		await().atMost(Duration.ofSeconds(10)).until(() -> !notificationService.sent().isEmpty());
		assertThat(notificationService.sent()).singleElement().satisfies(notification -> {
			assertThat(notification.recipient()).isEqualTo("taipei-101@example.com");
			assertThat(notification.message()).isEqualTo("打烊後盤點");
		});
	}

	@Test
	void delete_shouldRemoveJobAndTrigger() throws Exception {
		create("store-101-closing", "0 30 22 * * ?");

		mockMvc.perform(delete("/api/reminders/store-101-closing")).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/reminders/store-101-closing")).andExpect(status().isNotFound());
		assertThat(scheduler.checkExists(JobKey.jobKey("store-101-closing", "reminders"))).isFalse();
	}

	// 程式中宣告成 Bean 的固定排程，也由 Spring Boot 註冊到同一個 Scheduler；/actuator/quartz 可以查看
	@Test
	void beanDefinedJob_shouldBeRegisteredAndVisibleInActuator() throws Exception {
		assertThat(scheduler.checkExists(JobKey.jobKey("daily-summary", "system"))).isTrue();

		mockMvc.perform(get("/actuator/quartz/jobs/system/daily-summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.className").value("com.example.demo.summary.DailySummaryJob"));
	}
}
