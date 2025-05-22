package com.example.demo.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.entity.CoursePO;
import com.example.demo.entity.StudentPO;

import lombok.Data;

@Data
public class CourseDTO {
    private long id;
    private String name;
    private int point;
    private Set<Long> studentIds;
    private Set<StudentDTO> students = new HashSet<>();

    // From entity to DTO with student IDs only
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
    public CoursePO toEntity() {
        CoursePO course = new CoursePO();
        course.setName(this.name);
        course.setPoint(this.point);
        return course;
    }

    // Update existing entity with DTO values (for update operations)
    public void updateEntity(CoursePO course) {
        course.setName(this.name);
        course.setPoint(this.point);
    }

}
