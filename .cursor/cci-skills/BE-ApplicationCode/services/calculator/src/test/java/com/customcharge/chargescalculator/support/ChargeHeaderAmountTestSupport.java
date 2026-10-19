package com.customcharge.chargescalculator.support;

import com.customcharge.persistencelayer.calculatorhelper.ChargeHeaderAmountSupport;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.chargescalculator.service.charge.ChargeLineSupport;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public final class ChargeHeaderAmountTestSupport {

    private ChargeHeaderAmountTestSupport() {
    }

    public static void stubApplyChargeHeaderAmounts(ChargeLineSupport chargeLineSupport) {
        lenient().doAnswer(invocation -> {
            Charge charge = invocation.getArgument(0);
            List<ChargeLineItem> lines = invocation.getArgument(1);
            ChargeHeaderAmountSupport.applyToCharge(charge, lines, amount -> amount);
            return null;
        }).when(chargeLineSupport).applyChargeHeaderAmounts(any(), any());
    }
}
