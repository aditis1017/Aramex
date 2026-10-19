package com.customcharge.dashboard.constants;

import java.util.Map;

/**
 * Payment type display labels, filter tokens, and database codes (C/P).
 */
public final class PaymentTypeConstants {

    public static final String DISPLAY_CASH = "CASH";
    public static final String DISPLAY_ACCOUNT = "ACCOUNT";

    public static final String FILTER_CASH = "cash";
    public static final String FILTER_ACCOUNT = "account";

    public static final String DB_CASH = "C";
    public static final String DB_PREPAID = "P";

    public static final Map<String, String> DISPLAY_TO_DB = Map.of(
            FILTER_CASH, DB_CASH,
            FILTER_ACCOUNT, DB_PREPAID
    );

    private PaymentTypeConstants() {
    }
}
