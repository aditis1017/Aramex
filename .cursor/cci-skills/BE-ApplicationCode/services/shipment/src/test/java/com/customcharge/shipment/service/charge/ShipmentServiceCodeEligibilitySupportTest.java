package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentServiceCodeEligibilitySupportTest {

    @Test
    void isArAccount_shouldDetectArNature() {
        Account account = new Account();
        account.setAccountNature(AccountNature.AR);

        assertThat(ShipmentServiceCodeEligibilitySupport.isArAccount(account)).isTrue();
        assertThat(ShipmentServiceCodeEligibilitySupport.isArAccount(new Account())).isFalse();
        assertThat(ShipmentServiceCodeEligibilitySupport.isArAccount(null)).isFalse();
    }

    @Test
    void isMasterEligibleForAccount_shouldRejectDeletedMaster() {
        ServiceCodes master = new ServiceCodes();
        master.setIsDeleted(true);
        master.setIsActive(true);

        assertThat(ShipmentServiceCodeEligibilitySupport.isMasterEligibleForAccount(new Account(), master))
                .isFalse();
    }

    @Test
    void isMasterEligibleForAccount_shouldAcceptActiveMaster() {
        ServiceCodes master = new ServiceCodes();
        master.setIsDeleted(false);
        master.setIsActive(true);

        assertThat(ShipmentServiceCodeEligibilitySupport.isMasterEligibleForAccount(new Account(), master))
                .isTrue();
    }
}
