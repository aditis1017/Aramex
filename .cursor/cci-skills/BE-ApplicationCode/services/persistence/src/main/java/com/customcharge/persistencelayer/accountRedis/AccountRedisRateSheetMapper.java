package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;

import java.util.ArrayList;
import java.util.List;

final class AccountRedisRateSheetMapper {

    private AccountRedisRateSheetMapper() {}

    static ArAccountRedisRateSheetItem toRedis(RateSheetItem item) {
        ArAccountRedisRateSheetItem redis = new ArAccountRedisRateSheetItem();
        redis.setId(item.getId());
        redis.setDescription(item.getDescription());
        redis.setServiceCode(item.getServiceCode());
        redis.setShipmentType(item.getShipmentType());
        redis.setBusinessType(item.getBusinessType());
        redis.setCategoryType(item.getCategoryType());
        if (item.getRates() != null) {
            redis.setRates(item.getRates().stream().map(AccountRedisRateSheetMapper::toRedisRate).toList());
        }
        return redis;
    }

    private static ArAccountRedisRateSheetItemRate toRedisRate(RateSheetItemRate rate) {
        ArAccountRedisRateSheetItemRate redis = new ArAccountRedisRateSheetItemRate();
        redis.setId(rate.getId());
        if (rate.getRateType() != null) {
            redis.setRateType(rate.getRateType().getDisplayName());
        }
        if (rate.getPercentageOf() != null) {
            redis.setPercentageOf(rate.getPercentageOf().getDisplayName());
        }
        redis.setValue(rate.getValue());
        return redis;
    }

    static List<RateSheetItem> toEntities(List<ArAccountRedisRateSheetItem> redisItems, RateSheet rateSheet) {
        if (redisItems == null || redisItems.isEmpty()) {
            return List.of();
        }
        List<RateSheetItem> items = new ArrayList<>(redisItems.size());
        for (ArAccountRedisRateSheetItem redis : redisItems) {
            items.add(toEntity(redis, rateSheet));
        }
        return items;
    }

    private static RateSheetItem toEntity(ArAccountRedisRateSheetItem redis, RateSheet rateSheet) {
        RateSheetItem item = new RateSheetItem();
        AccountRedisEntityIdSupport.setId(item, redis.getId());
        item.setRateSheet(rateSheet);
        item.setDescription(redis.getDescription());
        item.setServiceCode(redis.getServiceCode());
        item.setShipmentType(redis.getShipmentType());
        item.setBusinessType(redis.getBusinessType());
        item.setCategoryType(redis.getCategoryType());
        item.setIsDeleted(false);
        List<RateSheetItemRate> rates = new ArrayList<>();
        if (redis.getRates() != null) {
            for (ArAccountRedisRateSheetItemRate redisRate : redis.getRates()) {
                rates.add(toEntityRate(redisRate, item));
            }
        }
        item.setRates(rates);
        return item;
    }

    private static RateSheetItemRate toEntityRate(ArAccountRedisRateSheetItemRate redis, RateSheetItem item) {
        RateSheetItemRate rate = new RateSheetItemRate();
        AccountRedisEntityIdSupport.setId(rate, redis.getId());
        rate.setRateSheetItem(item);
        if (redis.getRateType() != null && !redis.getRateType().isBlank()) {
            rate.setRateType(RateType.fromDisplayName(redis.getRateType()));
        }
        if (redis.getPercentageOf() != null && !redis.getPercentageOf().isBlank()) {
            rate.setPercentageOf(PercentageOf.fromDisplayName(redis.getPercentageOf()));
        }
        rate.setValue(redis.getValue());
        return rate;
    }
}
