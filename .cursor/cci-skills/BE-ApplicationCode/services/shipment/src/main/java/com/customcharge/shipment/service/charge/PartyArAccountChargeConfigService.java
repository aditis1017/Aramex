package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolated write transaction for party AR charge-config creation during ingest.
 */
@Service
@RequiredArgsConstructor
public class PartyArAccountChargeConfigService {

    private final AccountRepository accountRepository;
    private final EntityRepository entityRepository;

    @Transactional
    public Account createChargeConfigIfNeeded(ARAccount arMaster, Entity entity, String chargeType) {
        return ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, arMaster, entity, chargeType);
    }
}
