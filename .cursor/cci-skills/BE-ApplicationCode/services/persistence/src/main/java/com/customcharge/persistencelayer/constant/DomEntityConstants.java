package com.customcharge.persistencelayer.constant;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

public final class DomEntityConstants {

    public static final String DOM_ENTITY_CATEGORY_CODE = "DOM_ENTITY";
    public static final String DEFAULT_DOM_ENTITY_CODE = "RUH";

    private DomEntityConstants() {
    }

    public static Optional<String> buildDomEntityValueCode(String consigneeCountryCode) {
        if (!StringUtils.hasText(consigneeCountryCode)) {
            return Optional.empty();
        }
        return Optional.of(
                DOM_ENTITY_CATEGORY_CODE + "_" + consigneeCountryCode.strip().toUpperCase(Locale.ROOT));
    }

    public static String formatDomEntityValidationMessage(String entityCode) {
        String entity = entityCode == null || entityCode.isBlank() ? DEFAULT_DOM_ENTITY_CODE : entityCode.strip();
        return "This is a Domestic shipment which is alerted under "
                + entity
                + " entity. Please upload this shipment under "
                + entity
                + ".";
    }
}
