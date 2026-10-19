package com.customcharge.dashboard.constants;

/**
 * Structured log message templates for the dashboard module.
 */
public final class DashboardLogMessages {

    public static final String ACCOUNT_CACHE_READ_FAILED =
            "Account cache read failed; falling back to database [reason={}]";
    public static final String AR_ACCOUNT_CACHE_THIRD_PARTY_OVERWRITE =
            "AR account cache missing CCIPH id; reloading from database [key={}]";
    public static final String ACCOUNT_CACHE_WRITE_FAILED =
            "Account cache write failed [accountId={}, entity={}, nature={}, chargeType={}]";
    public static final String ACCOUNT_CACHE_WRITE_SUCCESS =
            "Account cache updated [key={}]";
    public static final String ACCOUNT_CACHE_DEBUG =
            "Account cache operation [key={}, payloadSize={}]";

    public static final String SAMPLE_ACCOUNT_EXISTS =
            "Sample account already present; refreshed dependencies [number={}, entity={}, nature={}, chargeType={}]";
    public static final String ACCOUNT_SYNC_FAILED =
            "Failed to sync account id={} nature={} entity={} chargeType={}: {}";

    private DashboardLogMessages() {
    }
}
