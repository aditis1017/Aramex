package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisReadListener;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import org.springframework.stereotype.Component;

@Component
public class ShipmentAccountRedisReadListener implements AccountRedisReadListener {

    @Override
    public boolean skipReads() {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        return context != null && context.isAccountRedisUnavailable();
    }

    @Override
    public void onReadFailure(String redisKey, Exception ex) {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            context.markAccountRedisUnavailable();
        }
    }
}
