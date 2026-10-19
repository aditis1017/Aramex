package com.customcharge.persistencelayer.util.aws;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class S3StorageService {

    private final S3Helper s3Helper;

    private final S3UploadStrategy directS3UploadStrategy;
    private final S3UploadStrategy lambdaS3UploadStrategy;

    public void saveDirect(String bucket, String key, byte[] data) {
        directS3UploadStrategy.upload(bucket, key, data);
    }

    public void saveViaLambda(String bucket, String key, byte[] data) {
        lambdaS3UploadStrategy.upload(bucket, key, data);
    }

    public Optional<byte[]> get(String bucket, String key) {
        return s3Helper.get(bucket, key);
    }
}