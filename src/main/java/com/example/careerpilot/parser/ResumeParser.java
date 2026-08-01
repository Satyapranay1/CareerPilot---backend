package com.example.careerpilot.parser;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeParser {

    String parse(MultipartFile file);
}