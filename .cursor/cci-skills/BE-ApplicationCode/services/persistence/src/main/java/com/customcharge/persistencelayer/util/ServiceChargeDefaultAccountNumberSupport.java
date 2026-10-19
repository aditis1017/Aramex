package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import org.springframework.util.StringUtils;

public final class ServiceChargeDefaultAccountNumberSupport {

    private ServiceChargeDefaultAccountNumberSupport() {
    }

    /**
     * Service-charge default wins when set; otherwise falls back to the account default.
     */
    public static String resolve(Account account, ServiceCharge charge) {
        if (charge != null && StringUtils.hasText(charge.getDefaultAccountNumber())) {
            return charge.getDefaultAccountNumber().trim();
        }
        Account effectiveAccount = account != null ? account : charge != null ? charge.getAccount() : null;
        if (effectiveAccount != null && StringUtils.hasText(effectiveAccount.getDefaultAccountNumber())) {
            return effectiveAccount.getDefaultAccountNumber().trim();
        }
        return null;
    }
}
