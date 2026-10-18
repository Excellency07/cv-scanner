package com.backend.cv_scanner.service;

import com.backend.cv_scanner.exception.CVBatchUploadException;
import com.backend.cv_scanner.exception.CVFileTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.cv_scanner.dao.entity.CVBatch;
import com.backend.cv_scanner.dao.entity.CVFile;
import com.backend.cv_scanner.dao.repository.jpa.CVBatchRepository;
import com.backend.cv_scanner.dao.repository.jpa.CVFileRepository;
import com.backend.cv_scanner.model.dto.CVBatchUploadResponse;
import com.backend.cv_scanner.model.enums.CVBatchStatus;
import com.backend.cv_scanner.model.enums.CVFileStatus;
import com.backend.cv_scanner.service.storage.StorageService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class CVFileUploadService {

    private static final Logger log = LoggerFactory.getLogger(CVFileUploadService.class);

    private static final int MAX_ENTRIES = 1000;

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final long MAX_TOTAL_SIZE = 500L * 1024 * 1024;

    private final CVBatchRepository cvBatchRepository;

    private final CVFileRepository cvFileRepository;

    private final StorageService storageService;

    public CVBatchUploadResponse upload(MultipartFile zipFile) {

        String originalFilename = validateZip(zipFile);

        CVBatch batch = new CVBatch(originalFilename);
        batch.setStatus(CVBatchStatus.UPLOADING);

        batch = cvBatchRepository.save(batch);

        int uploadedFiles = 0;
        int failedFiles = 0;
        int entriesProcessed = 0;
        long totalExpandedSize = 0;

        try (
                InputStream inputStream = zipFile.getInputStream();
                ZipInputStream zipInputStream =
                        new ZipInputStream(inputStream)
        ) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                try {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    entriesProcessed++;

                    if (entriesProcessed > MAX_ENTRIES) {
                        throw new CVBatchUploadException(
                                "ZIP contains too many entries"
                        );
                    }

                    String filename = getSafeFilename(entry.getName());

                    if (!isSupportedFile(filename)) {
                        continue;
                    }

                    Path tempFile = Files.createTempFile("cv-", ".tmp");
                    try {
                        long fileSize;

                        try {
                            fileSize = copyEntryToTempFile(
                                    zipInputStream,
                                    tempFile,
                                    MAX_FILE_SIZE
                            );
                        } catch (CVFileTooLargeException exception) {

                            failedFiles++;

                            log.warn(
                                    "Skipping oversized CV: {}",
                                    filename
                            );

                            continue;
                        }

                        totalExpandedSize += fileSize;

                        if (totalExpandedSize > MAX_TOTAL_SIZE) {
                            throw new CVBatchUploadException(
                                    "Expanded ZIP exceeds the allowed size"
                            );
                        }

                        try {
                            uploadCv(
                                    batch,
                                    tempFile,
                                    filename,
                                    fileSize
                            );

                            uploadedFiles++;

                        } catch (RuntimeException exception) {

                            failedFiles++;

                            log.error(
                                    "Failed to process CV: {}",
                                    filename,
                                    exception
                            );
                        }

                    } finally {
                        Files.deleteIfExists(tempFile);
                    }

                } finally {
                    zipInputStream.closeEntry();
                }
            }

        } catch (CVBatchUploadException exception) {

            markBatchFailed(batch, uploadedFiles, failedFiles);

            throw exception;

        } catch (IOException exception) {

            markBatchFailed(batch, uploadedFiles, failedFiles);

            throw new RuntimeException(
                    "Failed to read uploaded ZIP file",
                    exception
            );
        }

        batch.setTotal(uploadedFiles + failedFiles);
        batch.setFailedFiles(failedFiles);

        if (uploadedFiles == 0 && failedFiles > 0) {

            batch.setStatus(CVBatchStatus.FAILED);

        } else if (failedFiles > 0) {

            batch.setStatus(CVBatchStatus.COMPLETED_WITH_ERRORS);

        } else {

            batch.setStatus(CVBatchStatus.PENDING);
        }

        cvBatchRepository.save(batch);

        return new CVBatchUploadResponse(
                batch.getId(),
                batch.getOriginalFilename(),
                uploadedFiles,
                failedFiles,
                batch.getStatus().name()
        );
    }

    private void markBatchFailed(
            CVBatch batch,
            int uploadedFiles,
            int failedFiles
    ) {
        batch.setTotal(uploadedFiles + failedFiles);
        batch.setFailedFiles(failedFiles);
        batch.setStatus(CVBatchStatus.FAILED);

        cvBatchRepository.save(batch);
    }

    private void uploadCv(
            CVBatch batch,
            Path tempFile,
            String filename,
            long fileSize
    ) {
        String storageName =
                "cvs/" + UUID.randomUUID() + getExtension(filename);

        String contentType = getContentType(filename);

        boolean uploadedToStorage = false;

        try {
            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                storageService.upload(
                        storageName,
                        inputStream,
                        fileSize,
                        contentType
                );
            }

            uploadedToStorage = true;

            CVFile cvFile = new CVFile(
                    batch,
                    filename,
                    storageName,
                    fileSize
            );

            cvFile.setStatus(CVFileStatus.PENDING);

            cvFileRepository.save(cvFile);

        } catch (Exception e) {

            if (uploadedToStorage) {
                try {
                    storageService.delete(storageName);
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            throw new RuntimeException("Failed to upload CV: " + filename, e);
        }
    }

    private String validateZip(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "ZIP file must not be empty"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {

            throw new IllegalArgumentException(
                    "Only ZIP files are supported"
            );
        }

        return filename;
    }

    private String getSafeFilename(String entryName) {

        String normalized = entryName.replace('\\', '/');

        int lastSlash = normalized.lastIndexOf('/');

        return normalized.substring(lastSlash + 1);
    }

    private boolean isSupportedFile(String filename) {

        String lower = filename.toLowerCase(Locale.ROOT);

        return lower.endsWith(".pdf") || lower.endsWith(".docx");
    }

    private String getExtension(String filename) {

        int dotIndex = filename.lastIndexOf('.');

        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String getContentType(String filename) {

        String lower = filename.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }

        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private long copyEntryToTempFile(
            InputStream inputStream,
            Path target,
            long maxSize
    ) throws IOException {

        byte[] buffer = new byte[8192];
        long totalBytes = 0;

        try (OutputStream outputStream = Files.newOutputStream(target)) {

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                totalBytes += bytesRead;

                if (totalBytes > maxSize) {
                    throw new CVFileTooLargeException(
                            "CV file exceeds the maximum allowed size"
                    );
                }

                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return totalBytes;
    }
}