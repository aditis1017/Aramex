package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.util.ServiceChargeDefaultAccountNumberSupport;
import com.customcharge.persistencelayer.util.ServiceChargeTaxResolutionSupport;

final class AccountRedisServiceChargeMapper {

    private AccountRedisServiceChargeMapper() {}

    static ArAccountRedisServiceCharge toRedis(
            ServiceCodes master, ServiceCharge charge, Double entityTaxValue) {
        ArAccountRedisServiceCharge redis = new ArAccountRedisServiceCharge();
        if (charge != null) {
            redis.setId(charge.getId());
            redis.setDescription(charge.getDescription());
            redis.setActive(charge.getActive());
            redis.setIsBillable(charge.getIsBillable());
            redis.setSplit(charge.getSplit());
            redis.setDefaultAccountNumber(
                    ServiceChargeDefaultAccountNumberSupport.resolve(null, charge));
        }
        if (master != null) {
            redis.setServiceCodeMasterId(master.getId());
            redis.setServiceCode(master.getServiceCode());
            redis.setServiceDescription(master.getServiceDescription());
            redis.setType(master.getType());
            redis.setMasterActive(master.getIsActive());
            redis.setInfoAxsId(master.getInfoAxsId());
            redis.setFetchTax(master.getFetchTax());
            ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                    ServiceChargeTaxResolutionSupport.resolve(charge, master, entityTaxValue);
            redis.setTaxValue(tax.taxValue());
            redis.setTaxCode(tax.taxCode());
        }
        if (redis.getActive() == null) {
            redis.setActive(true);
        }
        if (redis.getIsBillable() == null) {
            redis.setIsBillable(true);
        }
        if (redis.getSplit() == null) {
            redis.setSplit("1");
        }
        if (redis.getDefaultAccountNumber() == null) {
            redis.setDefaultAccountNumber("");
        }
        return redis;
    }

    static ServiceCodes toServiceCodesMaster(ArAccountRedisServiceCharge redis, String entityCode, String chargeType) {
        ServiceCodes master = new ServiceCodes();
        AccountRedisEntityIdSupport.setId(master, redis.getServiceCodeMasterId());
        Entity entity = new Entity();
        entity.setCode(entityCode);
        master.setEntity(entity);
        master.setServiceCode(redis.getServiceCode());
        master.setServiceDescription(redis.getServiceDescription());
        master.setType(redis.getType());
        master.setTaxCode(redis.getTaxCode());
        master.setIsActive(redis.getMasterActive() == null || redis.getMasterActive());
        master.setIsDeleted(false);
        master.setChargeType(chargeType);
        master.setInfoAxsId(redis.getInfoAxsId());
        master.setFetchTax(redis.getFetchTax());
        return master;
    }

    static ServiceCharge toServiceCharge(ArAccountRedisServiceCharge redis, Account account, ServiceCodes master) {
        ServiceCharge charge = new ServiceCharge();
        AccountRedisEntityIdSupport.setId(charge, redis.getId());
        charge.setAccount(account);
        charge.setServiceCodes(master);
        charge.setDescription(redis.getDescription());
        charge.setActive(redis.getActive());
        charge.setIsBillable(redis.getIsBillable());
        charge.setSplit(redis.getSplit());
        charge.setDefaultAccountNumber(redis.getDefaultAccountNumber());
        return charge;
    }
}
