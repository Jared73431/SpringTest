package com.example.demo.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String dir;
    private long maxSize;
    private List<String> allowedTypes;
}
