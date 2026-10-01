package com.example.demo.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.example.demo.TestcontainersConfiguration;
import com.example.demo.dto.CourseDTO;
import com.example.demo.dto.StudentDTO;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.ImageRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;

/**
 * 課程 / 學生、Todo / User、圖片 API 的行為測試（透過真實 HTTP 呼叫）。
 * 標示 [Potential Bug] 的測試記錄的是「目前的行為」，修正時會一併修改。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OtherApiTest {

	private static final byte[] PNG_BYTES = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 1, 2, 3 };

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ImageRepository imageRepository;

	@BeforeEach
	void cleanDatabase() {
		studentRepository.deleteAllFromSelectedCourse();
		studentRepository.deleteAll();
		courseRepository.deleteAll();
		// User 對 Todo 設定 cascade = ALL，刪除 User 會一併刪除其 Todo
		userRepository.deleteAll();
		imageRepository.deleteAll();
	}

	private Long createCourse(String name) {
		return restTemplate.postForEntity("/api/courses", Map.of("name", name, "point", 3), CourseDTO.class)
				.getBody().getId();
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

		ResponseEntity<CourseDTO> response = restTemplate.postForEntity("/api/courses/{id}/students/batch",
				List.of(amy, ben), CourseDTO.class, courseId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStudentIds()).containsExactlyInAnyOrder(amy, ben);
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

	// [Potential Bug] 沒有輸入驗證，不帶 name 也能建立課程
	@Test
	void createCourse_shouldReturnCreatedWithNullName_whenNameMissing() {
		ResponseEntity<CourseDTO> response = restTemplate.postForEntity("/api/courses", Map.of(), CourseDTO.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody().getName()).isNull();
	}

	// ===== User / Todo（一對多） =====

	@Test
	void getTodosByUserId_shouldReturnUserWithTodos_whenUserHasTodos() {
		restTemplate.postForEntity("/api/saveUser?name={name}", null, Void.class, "Tom");
		Integer userId = userRepository.findAll().get(0).getId();
		restTemplate.postForEntity("/saveTodo?task={task}&Userid={id}", null, Void.class, "Study", userId);

		ResponseEntity<String> response = restTemplate.getForEntity("/api/users/{id}/todos", String.class, userId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"name\":\"Tom\"").contains("\"task\":\"Study\"");
		// User 的 password 欄位會出現在回應中（目前決定先不處理）
		assertThat(response.getBody()).contains("\"password\"");
	}

	// [Potential Bug] 查不到 Todo 時回 200 與 null，而不是 404
	@Test
	void getTodos_shouldReturnOkWithNull_whenTodoNotExists() {
		ResponseEntity<String> response = restTemplate.getForEntity("/todo/999999", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("null");
	}

	// ===== 圖片 =====

	private Long uploadImage() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		HttpHeaders fileHeaders = new HttpHeaders();
		fileHeaders.setContentType(MediaType.IMAGE_PNG);
		body.add("file", new HttpEntity<>(new ByteArrayResource(PNG_BYTES) {
			@Override
			public String getFilename() {
				return "test.png";
			}
		}, fileHeaders));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		ResponseEntity<Map> response = restTemplate.postForEntity("/api/images/upload",
				new HttpEntity<>(body, headers), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		return ((Number) response.getBody().get("imageId")).longValue();
	}

	@Test
	void getImage_shouldReturnBytesWithContentType_whenImageUploaded() {
		Long imageId = uploadImage();

		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/{id}", byte[].class, imageId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
		assertThat(response.getBody()).isEqualTo(PNG_BYTES);
	}

	// 圖片清單會回傳每張圖片的完整內容（base64）
	@Test
	void getAllImages_shouldIncludeImageData_whenImagesExist() {
		uploadImage();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/images", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"name\":\"test.png\"").contains("\"data\":");
	}

	@Test
	void getImage_shouldReturnNotFound_whenImageNotExists() {
		ResponseEntity<byte[]> response = restTemplate.getForEntity("/api/images/999999", byte[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	// [Potential Bug] 頁面上的刪除使用 GET 請求
	@Test
	void deleteImagePage_shouldDeleteImage_whenCalledWithGet() {
		Long imageId = uploadImage();

		restTemplate.getForEntity("/delete/{id}", String.class, imageId);

		assertThat(imageRepository.existsById(imageId)).isFalse();
	}
}
