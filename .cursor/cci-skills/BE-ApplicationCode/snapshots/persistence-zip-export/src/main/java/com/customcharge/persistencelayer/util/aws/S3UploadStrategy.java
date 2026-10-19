package com.customcharge.persistencelayer.util.aws;

public interface S3UploadStrategy {
    void upload(String bucket, String key, byte[] data);
}