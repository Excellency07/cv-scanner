package com.backend.cv_scanner.service.storage;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    void upload(String storageName, InputStream inputStream, long size, String contentType) throws IOException;
    InputStream download(String storageName, String originalFilename) throws IOException;
    void delete(String storageName) throws IOException;
}
