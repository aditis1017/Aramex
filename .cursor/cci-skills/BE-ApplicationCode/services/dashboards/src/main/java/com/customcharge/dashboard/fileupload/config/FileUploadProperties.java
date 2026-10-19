package com.customcharge.dashboard.fileupload.config;

import com.customcharge.dashboard.constants.FileUploadConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "dashboard.file-upload")
public class FileUploadProperties {
    private int presignTtlSeconds = FileUploadConstants.DEFAULT_PRESIGN_TTL_SECONDS;
    private String incomingUploadKeyPrefix = FileUploadConstants.DEFAULT_INCOMING_UPLOAD_KEY_PREFIX;
}
