package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRedisSyncJobStatusResponse {

    private String jobId;
    private String status;
    private int processed;
    private int succeeded;
    private int failed;
    private String message;
    private Instant startedAt;
    private Instant completedAt;
}
