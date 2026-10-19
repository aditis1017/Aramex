package com.customcharge.dashboard.constants;

import java.util.List;

/**
 * Service code validation limits and sample seed templates.
 */
public final class ServiceCodesConstants {

    public static final int MAX_SERVICE_CODE_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 255;
    public static final int MAX_TAX_CODE_LENGTH = 50;
    public static final int MAX_CHARGE_TYPE_LENGTH = 50;

    public static final String SAMPLE_SERVICE_CODE_DUTY = "DUTY";
    public static final String SAMPLE_SERVICE_CODE_REVENUE = "REVENUE";
    public static final String SAMPLE_SERVICE_CODE_VAT = "VAT";
    public static final String SAMPLE_SERVICE_CODE_OF = "OF";
    public static final String SAMPLE_TAX_CODE_VAT0 = "VAT0";
    public static final String SAMPLE_DESCRIPTION_DUTY = "Duty";
    public static final String SAMPLE_DESCRIPTION_REVENUE = "Revenue";
    public static final String SAMPLE_DESCRIPTION_VAT = "VAT";
    public static final String SAMPLE_DESCRIPTION_OFFICIAL_RECEIPT = "Official Receipt";

    public static final List<SampleServiceCodeTemplate> SAMPLE_REVENUE_SERVICE_CODES = List.of(
            new SampleServiceCodeTemplate(
                    SAMPLE_SERVICE_CODE_DUTY, SAMPLE_DESCRIPTION_DUTY, SAMPLE_TAX_CODE_VAT0),
            new SampleServiceCodeTemplate(
                    SAMPLE_SERVICE_CODE_REVENUE, SAMPLE_DESCRIPTION_REVENUE, SAMPLE_TAX_CODE_VAT0));
    public static final List<SampleServiceCodeTemplate> SAMPLE_COST_SERVICE_CODES = List.of(
            new SampleServiceCodeTemplate(
                    SAMPLE_SERVICE_CODE_DUTY, SAMPLE_DESCRIPTION_DUTY, SAMPLE_TAX_CODE_VAT0),
            new SampleServiceCodeTemplate(
                    SAMPLE_SERVICE_CODE_VAT, SAMPLE_DESCRIPTION_VAT, SAMPLE_TAX_CODE_VAT0),
            new SampleServiceCodeTemplate(
                    SAMPLE_SERVICE_CODE_OF, SAMPLE_DESCRIPTION_OFFICIAL_RECEIPT, SAMPLE_TAX_CODE_VAT0));

    public record SampleServiceCodeTemplate(
            String serviceCode,
            String serviceDescription,
            String taxCode) {
    }

    private ServiceCodesConstants() {
    }
}
