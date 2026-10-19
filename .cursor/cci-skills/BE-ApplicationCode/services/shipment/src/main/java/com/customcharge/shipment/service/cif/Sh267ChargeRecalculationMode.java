package com.customcharge.shipment.service.cif;

/**
 * Determines which charge recalculation path SH267 should follow after category re-evaluation.
 */
public enum Sh267ChargeRecalculationMode {

    /** Shipment was already exempted before SH267; no processing. */
    SKIP_ALREADY_EXEMPTED,

    /** Drools newly resolved exempted category. */
    NEWLY_EXEMPTED,

    /** Non-actualized shipment, category unchanged: CIF-percentage lines only. */
    CATEGORY_UNCHANGED_NORMAL,

    /** Actualized shipment, category unchanged: CIF-percentage ECD estimate lines only. */
    ACTUALIZED_CIF_ONLY,

    /** Actualized shipment, LV to HV: full ECD estimate recalc and revenue contracted-rate refresh. */
    ACTUALIZED_LV_TO_HV,

    /** Non-actualized LV to HV: zero ECD and reapply full rate sheet. */
    LV_TO_HV_NORMAL
}
