package com.customcharge.persistencelayer.util.aws;

public class DirectS3UploadStrategy implements S3UploadStrategy {

    private final S3Helper s3Helper;

    public DirectS3UploadStrategy(S3Helper s3Helper) {
        this.s3Helper = s3Helper;
    }

    @Override
    public void upload(String bucket, String key, byte[] data) {
        s3Helper.save(bucket, key, data);
    }
}