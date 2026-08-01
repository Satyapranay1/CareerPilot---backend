package com.example.careerpilot.storage;

import com.example.careerpilot.exception.InvalidResumeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final Path rootDirectory;
    private final long maxFileSize;

    public LocalFileStorageService(
            @Value("${careerpilot.resume.storage-directory}")
            String storageDirectory,

            @Value("${careerpilot.resume.max-file-size}")
            long maxFileSize
    ) {
        this.rootDirectory = Path.of(storageDirectory)
                .toAbsolutePath()
                .normalize();

        this.maxFileSize = maxFileSize;

        createRootDirectory();
    }

    @Override
    public String store(MultipartFile file, Long userId) {

        validate(file, userId);

        try {
            Path userDirectory = rootDirectory
                    .resolve(String.valueOf(userId))
                    .normalize();

            ensureInsideRoot(userDirectory);

            Files.createDirectories(userDirectory);

            String generatedFilename =
                    UUID.randomUUID() + ".pdf";

            Path destination = userDirectory
                    .resolve(generatedFilename)
                    .normalize();

            ensureInsideRoot(destination);

            try (var inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            /*
             * Store only:
             *
             * 42/uuid.pdf
             *
             * rather than the absolute filesystem path.
             */
            return rootDirectory
                    .relativize(destination)
                    .toString()
                    .replace('\\', '/');

        } catch (IOException exception) {
            throw new InvalidResumeException(
                    "Unable to store the resume PDF.",
                    exception
            );
        }
    }

    @Override
    public Resource retrieve(String storedFilePath) {

        Path filePath = resolveStoredPath(storedFilePath);

        try {
            Resource resource = new UrlResource(
                    filePath.toUri()
            );

            if (!resource.exists()
                    || !resource.isReadable()
                    || !Files.isRegularFile(filePath)) {

                throw new InvalidResumeException(
                        "Stored resume PDF could not be found."
                );
            }

            return resource;

        } catch (IOException exception) {
            throw new InvalidResumeException(
                    "Unable to retrieve the resume PDF.",
                    exception
            );
        }
    }

    @Override
    public void delete(String storedFilePath) {

        Path filePath = resolveStoredPath(storedFilePath);

        try {
            Files.deleteIfExists(filePath);

            removeEmptyUserDirectory(filePath.getParent());

        } catch (IOException exception) {
            throw new InvalidResumeException(
                    "Unable to delete the resume PDF.",
                    exception
            );
        }
    }

    private void validate(
            MultipartFile file,
            Long userId
    ) {

        if (userId == null || userId <= 0) {
            throw new InvalidResumeException(
                    "Invalid authenticated user."
            );
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidResumeException(
                    "Resume file is required and cannot be empty."
            );
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidResumeException(
                    "Resume file exceeds the maximum allowed size."
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.isBlank()) {

            throw new InvalidResumeException(
                    "Resume filename is missing."
            );
        }

        String lowercaseFilename =
                originalFilename.toLowerCase(Locale.ROOT);

        if (!lowercaseFilename.endsWith(".pdf")) {
            throw new InvalidResumeException(
                    "Only PDF resumes are supported."
            );
        }

        String contentType = file.getContentType();

        if (contentType != null
                && !contentType.equalsIgnoreCase(PDF_CONTENT_TYPE)
                && !contentType.equalsIgnoreCase(
                "application/octet-stream"
        )) {

            throw new InvalidResumeException(
                    "Only PDF resumes are supported."
            );
        }
    }

    private Path resolveStoredPath(String storedFilePath) {

        if (storedFilePath == null
                || storedFilePath.isBlank()) {

            throw new InvalidResumeException(
                    "Stored resume path is invalid."
            );
        }

        Path resolved = rootDirectory
                .resolve(storedFilePath)
                .normalize();

        ensureInsideRoot(resolved);

        return resolved;
    }

    private void ensureInsideRoot(Path path) {

        if (!path.startsWith(rootDirectory)) {
            throw new InvalidResumeException(
                    "Invalid resume storage path."
            );
        }
    }

    private void createRootDirectory() {

        try {
            Files.createDirectories(rootDirectory);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to initialize resume storage directory.",
                    exception
            );
        }
    }

    private void removeEmptyUserDirectory(Path directory) {

        if (directory == null
                || directory.equals(rootDirectory)) {
            return;
        }

        ensureInsideRoot(directory);

        try (var entries = Files.list(directory)) {

            if (entries.findAny().isEmpty()) {
                Files.deleteIfExists(directory);
            }

        } catch (IOException ignored) {
            /*
             * The actual PDF deletion already succeeded.
             * Failure to clean an empty directory should not
             * fail the entire delete operation.
             */
        }
    }
}