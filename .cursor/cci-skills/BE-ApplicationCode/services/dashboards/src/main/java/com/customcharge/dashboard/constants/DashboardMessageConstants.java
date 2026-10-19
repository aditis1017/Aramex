package com.customcharge.dashboard.constants;

/**
 * Standard API response and user-facing message fragments.
 */
public final class DashboardMessageConstants {

    public static final String SUCCESS = "success";
    public static final String SAVED_SUCCESSFULLY = "Saved successfully";

    public static final String ACCOUNT_NOT_FOUND = "Account not found";
    public static final String RATE_SHEET_NOT_FOUND = "RateSheet not found";
    public static final String SERVICE_CHARGE_NOT_FOUND = "Service charge not found";
    public static final String DELETED_SUCCESSFULLY = "Deleted successfully";
    public static final String UPDATED_SUCCESSFULLY = "Updated successfully";

    public static final String SHIPMENT_NOT_FOUND_FOR_TRACKING_PREFIX =
            "Shipment not found for tracking number: ";
    public static final String NO_CHARGES_FOR_TRACKING_PREFIX =
            "No charges found for tracking number: ";
    public static final String CHARGE_TYPE_NOT_FOUND_FOR_TRACKING_PREFIX =
            "Charge type ";
    public static final String CHARGE_TYPE_NOT_FOUND_FOR_TRACKING_MIDDLE =
            " not found for tracking number: ";

    private DashboardMessageConstants() {
    }
}
