package com.customcharge.chargescalculator.constants;

/**
 * Business rule tokens and user-facing error fragments for calculator processing.
 */
public final class BatchProcessingConstants {

    private BatchProcessingConstants() {
    }

    public static final int CHUNK_SIZE = 20;

    public static final String STATUS_DUE = "DUE";
    public static final String STATUS_ACTUALIZED = "ACTUALIZED";
    public static final String STATUS_EXEMPTED = "Exempted";
    public static final String CATEGORY_TYPE_EXEMPTED = "Exempted";
    public static final String PRODUCT_TYPE_LV = "LV";
    public static final String SHIPMENT_TYPE_DDU = "DDU";
    public static final String SHIPMENT_TYPE_DDO = "DDO";
    public static final String SHIPMENT_TYPE_DDP = "DDP";
    public static final String PROCESS_FOR_ESTIMATE = "Estimates_Template";
    public static final String PROCESS_FOR_ACTUAL = "Actual_Template";

    public static final int DEFAULT_MAX_ACTUAL_UPLOAD_COUNT = 3;

    public static final String ERROR_ACTUAL_UPLOAD_LIMIT_EXCEEDED_TEMPLATE =
            "Maximum actual charge calculation uploads reached for this shipment (limit: %d)";

    public static String formatActualUploadLimitExceeded(int limit) {
        return ERROR_ACTUAL_UPLOAD_LIMIT_EXCEEDED_TEMPLATE.formatted(limit);
    }

    public static String missingPaymentReferenceMessage() {
        return paymentReferenceLabel() + " is required for actual charge processing";
    }

    public static final String PAYMENT_TYPE_PREPAID = "P";
    public static final String PAYMENT_TYPE_THIRD_PARTY = "3";

    public static final String CHARGE_CODE_ECD = "ECD";
    public static final String CHARGE_CODE_ECS = "ECS";
    public static final String CHARGE_CODE_EC3 = "EC3";
    public static final String CHARGE_CODE_DTC = "DTC";
    public static final String PRODUCT_GROUP_DOM = "DOM";
    public static final String CHARGE_CODE_DTA = "DTA";
    public static final String CHARGE_CODE_DTS = "DTS";
    public static final String CHARGE_CODE_DT3 = "DT3";

    public static final String GLOBAL_ENTITY_CODE = "Global";

    public static final String REVENUE_TYPE_CODE = "C";
    public static final String CALCULATED_CHARGES_KEY_DTA_REVENUE = "dtaRevenueAmount";

    public static final String PAYMENT_REFERENCE = "saddad";

    public static String paymentReferenceLabel() {
        return "Payment Reference(" + PAYMENT_REFERENCE+")";
    }

    public static final String SOURCE_KEY_CURRENCY = "currency";
    public static final String DEFAULT_CURRENCY = "SAR";
    public static final String SOURCE_KEY_DEMARRAGE = "demarrage";
    public static final String SOURCE_KEY_DECLARATION_NUMBER = "declaration number";
    public static final String SOURCE_KEY_CONSOL_GROUP = "consol group";
    public static final String SOURCE_KEY_PAYING_ENTITY = "paying entity";
    public static final String SOURCE_KEY_AP_ACCOUNT = "ap account";

    public static final String ERROR_KEY = "errorMessage";
    public static final String ERROR_CODE_KEY = "errorCode";
    public static final String ERROR_SHIPMENT_NOT_FOUND = "Shipment not found for awb";
    public static final String ERROR_GATEWAY_TRACKING_FAILED = "Gateway tracking request failed";
    public static final String ERROR_INVALID_STATUS = "Shipment is not in DUE status";
    public static final String ERROR_MISSING_SHIPMENT_TYPE = "Missing shipment type for charge calculation";
    public static final String ERROR_UNSUPPORTED_SHIPMENT_TYPE = "Unsupported shipment type for charge calculation";
    public static final String ERROR_UNSUPPORTED_PROCESS_FOR = "Unsupported processFor for charge calculation";
    public static final String ERROR_INVALID_RECORD = "Invalid record payload";
    public static final String ERROR_DDO_ALREADY_CONSUMED =
            "DDO shipment already consumed; ECD and DTC charges already exist for this shipment";
    public static final String ERROR_EXEMPTED_ALREADY_CONSUMED =
            "Exempted category shipment already consumed; charges already exist for this shipment";
    public static final String ERROR_ACTUAL_PAYMENT_REFERENCE_ALREADY_EXISTS =
            "Actual charge line items already exist for this " + paymentReferenceLabel() + " and charge code";
    public static final String ERROR_ACTUAL_DECLARATION_NUMBER_ALREADY_EXISTS =
            "Remove this row from process because of duplicate Declaration Number In System";
    public static final String ERROR_ACTUAL_RECORD_EXISTS_FOR_AWB =
            "Actual charges already exist for AWB %s. Estimate re-calculation is not allowed.";

    public static String formatActualRecordExistsForAwb(String awb) {
        return ERROR_ACTUAL_RECORD_EXISTS_FOR_AWB.formatted(awb == null ? "" : awb.strip());
    }

    public static String formatEntityDestinationMismatch(String selectedEntity, String awbDestinationEntity) {
        return ("Selected entity (%s) does not match the AWB destination (%s). "
                + "Please select the entity matching the destination country and re-upload the file")
                .formatted(
                        selectedEntity == null || selectedEntity.isBlank() ? "unknown" : selectedEntity.strip(),
                        awbDestinationEntity == null || awbDestinationEntity.isBlank()
                                ? "unknown"
                                : awbDestinationEntity.strip());
    }

    public static final String ERROR_PROCESSING_FAILURE = "Failed to process record";
}
