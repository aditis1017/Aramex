package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.service.FileFetchService;
import com.customcharge.persistencelayer.util.aws.S3Helper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class S3DocumentFetchService implements FileFetchService {

    private static final String S3_SCHEME = "s3://";
    private final S3Helper s3Helper;

    public S3DocumentFetchService(S3Helper s3Helper) {
        this.s3Helper = s3Helper;
    }

    @Override
    public InputStream fetch(String documentPath) {
        if (documentPath == null || documentPath.isBlank()) {
            throw new IllegalArgumentException("documentPath must not be blank");
        }

        if (documentPath.startsWith(S3_SCHEME)) {
            String withoutScheme = documentPath.substring(S3_SCHEME.length());
            int separatorIndex = withoutScheme.indexOf('/');
            if (separatorIndex < 1 || separatorIndex == withoutScheme.length() - 1) {
                throw new IllegalArgumentException("Invalid S3 path format: " + documentPath);
            }
            String bucket = withoutScheme.substring(0, separatorIndex);
            String key = withoutScheme.substring(separatorIndex + 1);
            return s3Helper.getAsStream(bucket, key)
                    .orElseThrow(() -> new IllegalStateException("S3 object not found for path: " + documentPath));
        }

        return s3Helper.getAsStream(documentPath)
                .orElseThrow(() -> new IllegalStateException("S3 object not found for key: " + documentPath));
    }
}
