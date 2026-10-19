package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.service.charge.ChargeLineSupport;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetAmountBreakdown;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetCoreAmountResolver;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Shipment;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DduEstimateLineAmountResolver {

    RateSheetCoreAmountResolver rateSheetCoreAmountResolver;

    public boolean isRateSheetCoreLine(String chargeCode, ServiceChargeResponse serviceCharge) {
        return isRateSheetChargeCode(normalizeChargeCode(chargeCode))
                && ChargeLineSupport.isRevenueServiceLine(serviceCharge);
    }

    public RateSheetAmountBreakdown resolveRateSheetBreakdown(
            Account account,
            Shipment shipment,
            ServiceChargeResponse serviceCharge) {
        return rateSheetCoreAmountResolver.resolveBreakdown(account, shipment, serviceCharge);
    }

    private static boolean isRateSheetChargeCode(String code) {
        return BatchProcessingConstants.CHARGE_CODE_DTC.equals(code)
                || BatchProcessingConstants.CHARGE_CODE_DTA.equals(code)
                || BatchProcessingConstants.CHARGE_CODE_DTS.equals(code)
                || BatchProcessingConstants.CHARGE_CODE_DT3.equals(code);
    }

    private static String normalizeChargeCode(String chargeCode) {
        return chargeCode == null ? "" : chargeCode.strip().toUpperCase(Locale.ROOT);
    }
}

