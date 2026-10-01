package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.entity.StudentPO;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 學生的 DTO，同時作為 Request 與 Response；選課關係只以 courseIds 表示，避免與 CourseDTO 互相巢狀。
 * 提供 fromEntity() / toEntity() / updateEntity() 集中處理 Entity 與 DTO 的轉換。
 */
@Data
@NoArgsConstructor
// NON_EMPTY：null、空字串、空集合的欄位不輸出到 JSON（例如沒有選課時省略 courseIds）
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StudentDTO {

    private long id;

    @NotBlank(message = "學生姓名不可為空")
    @Size(max = 255, message = "學生姓名最多 255 字")
    private String name;
    private Set<Long> courseIds = new HashSet<>();

    // From entity to DTO with course IDs only
    // 存取 LAZY 的 courses 集合，需在交易內呼叫或事先以 @EntityGraph 載入
    public static StudentDTO fromEntity(StudentPO student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setCourseIds(student.getCourses().stream()
                .map(course -> course.getId())
                .collect(Collectors.toSet()));
        return dto;
    }

    // Convert DTO to entity (for create/update operations)
    public StudentPO toEntity() {
        StudentPO student = new StudentPO();
        student.setName(this.name);
        return student;
    }

    // Update existing entity with DTO values (for update operations)
    public void updateEntity(StudentPO student) {
        student.setName(this.name);
    }
}
