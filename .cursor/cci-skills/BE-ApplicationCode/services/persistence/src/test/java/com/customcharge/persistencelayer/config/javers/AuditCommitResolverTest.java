package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceChargeMapping;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditCommitResolverTest {

    private final AuditCommitResolver resolver = new AuditCommitResolver();

    @Test
    void account_stampsAccountIdAndEntityCode() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);
        Entity entity = new Entity();
        entity.setCode("dxb");
        ReflectionTestUtils.setField(account, "entity", entity);

        Optional<Map<String, String>> props =
                resolver.resolve(account, Account.class.getAnnotation(AuditableEntity.class));

        assertThat(props).isPresent();
        assertThat(props.get())
                .containsEntry(AuditCommitProperties.ACCOUNT_ID, accountId.toString())
                .containsEntry(AuditCommitProperties.ENTITY_CODE, "DXB");
    }

    @Test
    void rateSheetItemRate_stampsAccountId() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);
        RateSheet sheet = new RateSheet();
        sheet.setAccount(account);
        RateSheetItem item = new RateSheetItem();
        item.setRateSheet(sheet);
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateSheetItem(item);

        Optional<Map<String, String>> props =
                resolver.resolve(rate, RateSheetItemRate.class.getAnnotation(AuditableEntity.class));

        assertThat(props).isPresent();
        assertThat(props.get()).containsEntry(AuditCommitProperties.ACCOUNT_ID, accountId.toString());
    }

    @Test
    void serviceChargeMapping_stampsEntityCode() {
        ServiceChargeMapping mapping = new ServiceChargeMapping();
        mapping.setFromEntity("dxb");

        Optional<Map<String, String>> props =
                resolver.resolve(mapping, ServiceChargeMapping.class.getAnnotation(AuditableEntity.class));

        assertThat(props).isPresent();
        assertThat(props.get()).containsEntry(AuditCommitProperties.ENTITY_CODE, "DXB");
    }
}
