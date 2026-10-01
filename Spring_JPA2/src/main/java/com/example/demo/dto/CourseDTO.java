package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 課程的資料傳輸物件（DTO），同時作為 API 的 Request 與 Response。
 * 不直接回傳 CoursePO，可避免雙向關聯造成 JSON 無限遞迴與 LAZY 載入例外，也讓 API 格式不受資料表結構綁定。
 * Bean Validation 註解（@NotBlank、@Size…）搭配 Controller 的 @Valid 在進入 Service 前檢查輸入。
 */
@Data
public class CourseDTO {
    private long id;

    @NotBlank(message = "課程名稱不可為空")
    @Size(max = 255, message = "課程名稱最多 255 字")
    private String name;

    @PositiveOrZero(message = "學分不可為負數")
    private int point;
    // 預設只回傳學生 ID（輕量）；需要學生詳細資料時才使用 fromEntityWithStudents 填入 students
    // READ_ONLY：只出現在回應中，建立 / 修改課程時即使帶入也會被忽略；
    // 選課請使用 POST /api/courses/{id}/students/{studentId} 或 /students/batch
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<Long> studentIds;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<StudentDTO> students = new HashSet<>();

    // From entity to DTO with student IDs only
    // 會存取 LAZY 的 students 集合，必須在交易內呼叫，或事先以 @EntityGraph 載入，否則可能拋出 LazyInitializationException
    public static CourseDTO fromEntity(CoursePO course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setName(course.getName());
        dto.setPoint(course.getPoint());
        dto.setStudentIds(course.getStudents().stream()
                .map(StudentPO::getId)
                .collect(Collectors.toSet()));
        return dto;
    }

    // From entity to DTO with student details
    public static CourseDTO fromEntityWithStudents(CoursePO course) {
        CourseDTO dto = fromEntity(course);
        dto.setStudents(course.getStudents().stream()
                .map(StudentDTO::fromEntity)
                .collect(Collectors.toSet()));
        return dto;
    }

    // Convert DTO to entity (for create/update operations)
    // 刻意不複製 id 與 studentIds：id 由資料庫產生，學生關聯不在此建立（由 Service 的加入 / 移除學生方法另外處理）
    public CoursePO toEntity() {
        CoursePO course = new CoursePO();
        course.setName(this.name);
        course.setPoint(this.point);
        return course;
    }

    // Update existing entity with DTO values (for update operations)
    // 直接修改從資料庫查出的 managed Entity，交易提交時 Hibernate 的 dirty checking 會自動產生 UPDATE
    public void updateEntity(CoursePO course) {
        course.setName(this.name);
        course.setPoint(this.point);
    }

}
