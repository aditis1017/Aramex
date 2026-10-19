package com.customcharge.shipment.enums;

public enum PICode {
    SH013("SH013", "Create charges"),
    SH267("SH267", "CIF value update"),
    SH022("SH022", "MAWB linkage update"),
    SH001("SH001","Customs clearance in progress"),
    SH163("SH163","Lost shipment"),
    SH237("SH237","Lost shipment"),
    SH157("SH157","Damaged shipment"),
    SH162("SH162","Damaged shipment"),
    SH069("SH069","Returned shipment"),
    SH041("SH041","Cleared successfully");

    private final String description;
    private final String name;

    PICode(String name ,String description) {
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    public String getName() {
        return name;
    }
}
