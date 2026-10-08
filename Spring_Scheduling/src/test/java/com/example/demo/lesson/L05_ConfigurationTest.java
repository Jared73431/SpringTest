package com.example.demo.lesson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.PostgresContainerTestBase;

/**
 * 第 5 課：排程的設定外部化（以本模組實際的 SessionCleanupTask 為例）。
 *
 * <pre>
 * @Scheduled(cron = "${app.cleanup.cron}", zone = "${app.cleanup.zone}")
 * </pre>
 *
 * <ul>
 * <li>cron 與時區寫在設定檔，不同環境可以不同；設成 - 就停用這個排程</li>
 * <li>執行緒池大小：spring.task.scheduling.pool.size（第 3 課）</li>
 * <li>/actuator/scheduledtasks 列出目前所有排程，部署後可以確認設定是否生效</li>
 * </ul>
 *
 * 修正前的 3 個示範任務只會印字，已由這個實際的清理排程取代。
 */
class L05_ConfigurationTest {

	@Nested
	@SpringBootTest
	@AutoConfigureMockMvc
	class Default extends PostgresContainerTestBase {

		@Autowired
		private ScheduledTaskHolder scheduledTaskHolder;

		@Autowired
		private ThreadPoolTaskScheduler taskScheduler;

		@Autowired
		private MockMvc mockMvc;

		@Test
		void cleanupTask_shouldBeRegisteredWithCronFromConfiguration() {
			assertThat(scheduledTaskHolder.getScheduledTasks()).singleElement()
					.satisfies(task -> assertThat(((CronTask) task.getTask()).getExpression()).isEqualTo("0 0 3 * * *"));
		}

		// 修正前使用預設的 1 條執行緒：所有排程共用，一個慢任務會拖住其他排程（第 3 課）
		@Test
		void taskScheduler_shouldHaveConfiguredPoolSize() {
			assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(4);
			assertThat(taskScheduler.getThreadNamePrefix()).isEqualTo("scheduling-");
		}

		@Test
		void actuator_shouldListScheduledTasks() throws Exception {
			mockMvc.perform(get("/actuator/scheduledtasks"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.cron[0].expression").value("0 0 3 * * *"));
		}
	}

	// cron 設成 - 就停用這個排程（例如在某個環境不需要執行）
	@Nested
	@SpringBootTest(properties = "app.cleanup.cron=-")
	class Disabled extends PostgresContainerTestBase {

		@Autowired
		private ScheduledTaskHolder scheduledTaskHolder;

		@Test
		void cleanupTask_shouldNotBeRegistered_whenCronIsDash() {
			assertThat(scheduledTaskHolder.getScheduledTasks()).isEmpty();
		}
	}
}
