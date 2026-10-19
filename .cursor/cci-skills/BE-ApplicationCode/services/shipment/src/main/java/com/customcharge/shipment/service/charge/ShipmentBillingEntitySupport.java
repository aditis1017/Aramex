package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.customcharge.shipment.constants.LogMessages.BILLING_ENTITY_NOT_FOUND;

/**
 * Resolves and applies the billing {@link com.customcharge.persistencelayer.entities.Entity} on a charge.
 * ECS and DTS use {@link Shipment#getOriginEntity()}; EC3 and DT3 use third-party entity; other types use destination entity.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentBillingEntitySupport {

    EntityRepository entityRepository;

    public void applyToCharge(Charge charge, Shipment shipment) {
        if (charge == null || shipment == null || !StringUtils.hasText(charge.getChargeCode())) {
            return;
        }

        if (charge.getEntity() != null) {
            return;
        }

        String billingEntityCode = ShipmentChargeTypeSupport.resolveBillingEntityCode(
                shipment, charge.getChargeCode());
        if (!StringUtils.hasText(billingEntityCode)) {
            return;
        }

        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null) {
            context.findEntity(billingEntityCode)
                    .ifPresentOrElse(
                            charge::setEntity,
                            () -> log.debug(
                                    BILLING_ENTITY_NOT_FOUND,
                                    billingEntityCode,
                                    charge.getChargeCode()));
            return;
        }

        entityRepository.findByCodeIgnoreCase(billingEntityCode.strip())
                .ifPresentOrElse(
                        charge::setEntity,
                        () -> log.debug(
                                BILLING_ENTITY_NOT_FOUND,
                                billingEntityCode,
                                charge.getChargeCode()));
    }
}
