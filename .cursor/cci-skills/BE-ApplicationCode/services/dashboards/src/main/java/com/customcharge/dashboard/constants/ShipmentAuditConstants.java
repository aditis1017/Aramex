package com.customcharge.dashboard.constants;

import java.util.Set;

/**
 * Shipment audit metadata field names and validation messages.
 */
public final class ShipmentAuditConstants {

    public static final String HAWB_NUMBER_REQUIRED = "hawbNumber must be provided";

    public static final String METADATA_FIELD_ID = "id";
    public static final String METADATA_FIELD_CREATED_BY = "createdBy";
    public static final String METADATA_FIELD_UPDATED_BY = "updatedBy";
    public static final String METADATA_FIELD_CREATED_TIME = "createdTime";
    public static final String METADATA_FIELD_UPDATED_TIME = "updatedTime";
    public static final String METADATA_FIELD_ENTITY = "entity";

    public static final Set<String> AUDIT_METADATA_FIELDS_TO_STRIP = Set.of(
            METADATA_FIELD_ID,
            METADATA_FIELD_CREATED_BY,
            METADATA_FIELD_UPDATED_BY,
            METADATA_FIELD_CREATED_TIME,
            METADATA_FIELD_UPDATED_TIME,
            METADATA_FIELD_ENTITY);

    private ShipmentAuditConstants() {
    }
}
