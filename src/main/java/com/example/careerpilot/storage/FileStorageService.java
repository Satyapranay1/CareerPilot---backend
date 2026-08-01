package com.example.careerpilot.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, Long userId);

    Resource retrieve(String storedFilePath);

    void delete(String storedFilePath);
}