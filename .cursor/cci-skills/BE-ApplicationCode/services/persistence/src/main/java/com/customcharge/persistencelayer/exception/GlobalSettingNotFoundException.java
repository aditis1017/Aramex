package com.customcharge.persistencelayer.exception;

import java.util.UUID;

public class GlobalSettingNotFoundException extends AbstractApiException {

    public GlobalSettingNotFoundException(UUID id) {
        super(ErrorCode.GLOBAL_SETTING_NOT_FOUND,
                "No global setting exists for id " + id);
    }
}
