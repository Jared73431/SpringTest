package com.example.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.dto.CourseDTO;
import com.example.demo.dto.StudentDTO;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.StudentRepository;

/**
 * 課程 / 學生（多對多）API 的行為測試（透過真實 HTTP 呼叫）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class CourseStudentApiTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private StudentRepository studentRepository;

	@BeforeEach
	void cleanDatabase() {
		studentRepository.deleteAllFromSelectedCourse();
		studentRepository.deleteAll();
		courseRepository.deleteAll();
	}

	private Long createCourse(String name) {
		return restTemplate.postForEntity("/api/courses", Map.of("name", name, "point", 3), CourseDTO.class)
				.getBody().getId();
	}

	// CourseDTO.studentIds 為 READ_ONLY（只在回應中輸出），用戶端反序列化成 CourseDTO 時會被忽略，
	// 因此直接讀取回應 JSON；JSON 數字轉成 Map 時可能是 Integer，統一轉成 Long 比對
	private List<Long> studentIdsOf(ResponseEntity<Map> response) {
		return ((List<?>) response.getBody().get("studentIds")).stream()
				.map(id -> ((Number) id).longValue())
				.toList();
	}

	private Long createStudent(String name) {
		return restTemplate.postForEntity("/api/students", Map.of("name", name), StudentDTO.class)
				.getBody().getId();
	}

	// ===== 課程 / 學生（多對多） =====

	@Test
	void addStudentsToCourse_shouldLinkBothSides_whenAllStudentsExist() {
		Long courseId = createCourse("Java");
		Long amy = createStudent("Amy");
		Long ben = createStudent("Ben");

		ResponseEntity<Map> response = restTemplate.postForEntity("/api/courses/{id}/students/batch",
				List.of(amy, ben), Map.class, courseId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(studentIdsOf(response)).containsExactlyInAnyOrder(amy, ben);
		StudentDTO student = restTemplate.getForObject("/api/students/{id}", StudentDTO.class, amy);
		assertThat(student.getCourseIds()).containsExactly(courseId);
	}

	@Test
	void addStudentsToCourse_shouldReturnBadRequest_whenSomeStudentsNotExist() {
		Long courseId = createCourse("Java");
		Long amy = createStudent("Amy");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/courses/{id}/students/batch",
				List.of(amy, 999999L), String.class, courseId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBody()).contains("999999");
	}

	@Test
	void addStudentToCourse_shouldReturnNotFound_whenStudentNotExists() {
		Long courseId = createCourse("Java");

		ResponseEntity<String> response = restTemplate.postForEntity("/api/courses/{id}/students/{sid}", null,
				String.class, courseId, 999999L);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteCourse_shouldKeepStudentsAndRemoveRelation_whenCourseHasStudents() {
		Long courseId = createCourse("Java");
		Long amy = createStudent("Amy");
		restTemplate.postForEntity("/api/courses/{id}/students/{sid}", null, CourseDTO.class, courseId, amy);

		ResponseEntity<Void> response = restTemplate.exchange("/api/courses/{id}", HttpMethod.DELETE, null,
				Void.class, courseId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		StudentDTO student = restTemplate.getForObject("/api/students/{id}", StudentDTO.class, amy);
		assertThat(student.getName()).isEqualTo("Amy");
		assertThat(student.getCourseIds()).isEmpty();
	}

	@Test
	void getCourseById_shouldReturnNotFound_whenCourseNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/courses/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// 修正前：沒有輸入驗證，不帶 name 也能建立課程
	@Test
	void createCourse_shouldReturnBadRequest_whenNameMissing() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/courses", Map.of(), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).contains("\"name\"");
		assertThat(courseRepository.count()).isZero();
	}

	@Test
	void createStudent_shouldReturnBadRequest_whenNameBlank() {
		ResponseEntity<String> response = restTemplate.postForEntity("/api/students", Map.of("name", " "),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(studentRepository.count()).isZero();
	}

	// 修正前：courseIds 有重複時，查到的課程數少於 ID 數，被誤判為「課程不存在: []」（400）
	@Test
	void addCoursesToStudent_shouldTreatDuplicateIdsAsOne_whenSameCourseGivenTwice() {
		Long courseId = createCourse("Java");
		Long amy = createStudent("Amy");

		ResponseEntity<StudentDTO> response = restTemplate.postForEntity("/api/students/{id}/courses/batch",
				List.of(courseId, courseId), StudentDTO.class, amy);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getCourseIds()).containsExactly(courseId);
	}

	@Test
	void removeCoursesFromStudent_shouldTreatDuplicateIdsAsOne_whenSameCourseGivenTwice() {
		Long courseId = createCourse("Java");
		Long amy = createStudent("Amy");
		restTemplate.postForEntity("/api/students/{id}/courses/{cid}", null, StudentDTO.class, amy, courseId);

		ResponseEntity<StudentDTO> response = restTemplate.exchange("/api/students/{id}/courses/batch",
				HttpMethod.DELETE, new HttpEntity<>(List.of(courseId, courseId)), StudentDTO.class, amy);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getCourseIds()).isEmpty();
	}

	// studentIds / students 只出現在回應中；建立課程時帶入會被忽略，選課請用 /api/courses/{id}/students/...
	@Test
	void createCourse_shouldIgnoreStudentIds_whenGivenInRequest() {
		Long amy = createStudent("Amy");

		ResponseEntity<Map> response = restTemplate.postForEntity("/api/courses",
				Map.of("name", "Java", "point", 3, "studentIds", List.of(amy)), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(studentIdsOf(response)).isEmpty();
		assertThat(restTemplate.getForObject("/api/students/{id}", StudentDTO.class, amy).getCourseIds()).isEmpty();
	}
}
