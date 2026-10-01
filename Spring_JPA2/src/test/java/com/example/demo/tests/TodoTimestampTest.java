package com.example.demo.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.entity.Todo;
import com.example.demo.entity.User;
import com.example.demo.repository.TodoRepository;
import com.example.demo.repository.UserRepository;

/**
 * Todo 的 @CreationTimestamp / @UpdateTimestamp（Hibernate 自動時間戳記）。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TodoTimestampTest {

	@Autowired
	private TodoRepository todoRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void save_shouldUpdateUpdateTimeAndKeepCreateTime_whenTodoModified() throws InterruptedException {
		User user = new User();
		user.setName("Tom");
		userRepository.save(user);
		Todo todo = new Todo();
		todo.setTask("Study");
		todo.setUser(user);
		Todo saved = todoRepository.save(todo);
		Date createTime = saved.getCreateTime();
		Date firstUpdateTime = saved.getUpdateTime();

		Thread.sleep(20);
		saved.setTask("Study more");
		Todo updated = todoRepository.save(saved);

		assertThat(updated.getCreateTime()).isEqualTo(createTime);
		assertThat(updated.getUpdateTime()).isAfter(firstUpdateTime);
	}
}
