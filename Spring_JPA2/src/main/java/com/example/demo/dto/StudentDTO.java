package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.entity.StudentPO;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StudentDTO {

    private long id;
    private String name;
    private Set<Long> courseIds = new HashSet<>();

    // From entity to DTO with course IDs only
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
