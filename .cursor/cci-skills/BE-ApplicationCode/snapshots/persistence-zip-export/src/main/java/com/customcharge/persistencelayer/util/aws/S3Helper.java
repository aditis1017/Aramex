package com.customcharge.persistencelayer.util.aws;

import com.customcharge.persistencelayer.config.aws.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.Optional;

public class S3Helper {

    private static final Logger log = LoggerFactory.getLogger(S3Helper.class);

    /** Hard cap for ranged reads to avoid accidental large buffers. */
    public static final int MAX_RANGE_LENGTH_BYTES = 64 * 1024;

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3Helper(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    public void save(String bucket, String key, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromBytes(data));
    }

    public void save(String key, byte[] data) {
        save(requireDefaultBucket(), key, data);
    }

    public Optional<byte[]> get(String bucket, String key) {
        try {
            var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(FileCopyUtils.copyToByteArray(response));
        } catch (Exception e) {
            log.warn("Failed to get S3 object {}/{}: {}", bucket, key, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<InputStream> getAsStream(String bucket, String key) {
        try {
            var response = s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(response);
        } catch (Exception e) {
            log.warn("Failed to get S3 object as stream {}/{}: {}", bucket, key, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<byte[]> get(String key) {
        return get(requireDefaultBucket(), key);
    }

    public Optional<InputStream> getAsStream(String key) {
        return getAsStream(requireDefaultBucket(), key);
    }

    public void delete(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void delete(String key) {
        delete(requireDefaultBucket(), key);
    }

    public byte[] getObjectRange(String bucket, String key, long startInclusive, long endInclusive) {
        if (startInclusive < 0 || endInclusive < startInclusive) {
            throw new IllegalArgumentException("Invalid range: " + startInclusive + "-" + endInclusive);
        }
        long length = endInclusive - startInclusive + 1;
        if (length > MAX_RANGE_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "Range length " + length + " exceeds max " + MAX_RANGE_LENGTH_BYTES);
        }
        String rangeHeader = "bytes=" + startInclusive + "-" + endInclusive;
        try (var response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).range(rangeHeader).build())) {
            return FileCopyUtils.copyToByteArray(response);
        } catch (Exception e) {
            log.warn("Failed ranged get S3 object {}/{} range {}: {}", bucket, key, rangeHeader, e.getMessage());
            throw new IllegalStateException("S3 ranged read failed for " + bucket + "/" + key, e);
        }
    }

    public Optional<HeadObjectResponse> headObject(String bucket, String key) {
        try {
            return Optional.of(s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            log.warn("HeadObject failed for {}/{}: {}", bucket, key, e.getMessage());
            throw e;
        }
    }

    public boolean objectExists(String bucket, String key) {
        return headObject(bucket, key).isPresent();
    }

    public long getObjectContentLength(String bucket, String key) {
        return headObject(bucket, key).map(HeadObjectResponse::contentLength).orElse(0L);
    }

    public Optional<String> getObjectContentType(String bucket, String key) {
        return headObject(bucket, key).map(HeadObjectResponse::contentType);
    }

    private String requireDefaultBucket() {
        String bucket = s3Properties.getDefaultBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("mvp.persistence.aws.s3.default-bucket must be set when using default bucket methods");
        }
        return bucket;
    }
}
