package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;

import java.math.BigDecimal;

/**
 * Resolves tax value and tax code for service-charge fetch: account charge first, then
 * {@code fetchTax} on the master with entity-level tax, otherwise out of scope.
 */
public final class ServiceChargeTaxResolutionSupport {

    private static final String TAX_CODE_PREFIX = "TAX-";
    private static final String OUT_OF_SCOPE_TAX_CODE = "OOS";

    private ServiceChargeTaxResolutionSupport() {
    }

    public record ResolvedTax(Double taxValue, String taxCode) {
    }

    public static ResolvedTax resolve(ServiceCharge charge, ServiceCodes master, Double entityTaxValue) {
        Double chargeTaxValue = charge != null ? charge.getTaxValue() : null;
        if (chargeTaxValue != null) {
            return new ResolvedTax(chargeTaxValue, formatTaxCode(chargeTaxValue));
        }
        if (master != null && Boolean.TRUE.equals(master.getFetchTax()) && entityTaxValue != null) {
            return new ResolvedTax(entityTaxValue, formatTaxCode(entityTaxValue));
        }
        return new ResolvedTax(0.0, OUT_OF_SCOPE_TAX_CODE);
    }

    static String formatTaxCode(Double taxValue) {
        if (taxValue == null || taxValue <= 0) {
            return OUT_OF_SCOPE_TAX_CODE;
        }
        return TAX_CODE_PREFIX + formatTaxValue(taxValue);
    }

    private static String formatTaxValue(Double taxValue) {
        BigDecimal decimal = BigDecimal.valueOf(taxValue).stripTrailingZeros();
        return decimal.scale() <= 0 ? decimal.toBigInteger().toString() : decimal.toPlainString();
    }
}
