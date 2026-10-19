package com.customcharge.persistencelayer.util.aws;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@RequiredArgsConstructor
public class LambdaS3UploadStrategy implements S3UploadStrategy {

    private final LambdaClient lambdaClient;

    @Override
    public void upload(String bucket, String key, byte[] data) {

        String payload = """
        {
          "bucket":"%s",
          "key":"%s",
          "data":"%s"
        }
        """.formatted(bucket, key, java.util.Base64.getEncoder().encodeToString(data));

        InvokeRequest request = InvokeRequest.builder()
                .functionName("s3-upload-lambda")
                .payload(SdkBytes.fromUtf8String(payload))
                .build();

        lambdaClient.invoke(request);
    }
}