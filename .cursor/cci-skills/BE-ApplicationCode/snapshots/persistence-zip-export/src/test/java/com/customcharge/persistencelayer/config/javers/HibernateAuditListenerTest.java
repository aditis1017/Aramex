package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.audit.AuditAuthorContext;
import com.customcharge.persistencelayer.audit.AuditAuthorResolver;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import org.hibernate.event.spi.PostInsertEvent;
import org.javers.core.Javers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HibernateAuditListenerTest {

    @Mock
    private Javers javers;
    @Mock
    private AuditCommitResolver auditCommitResolver;
    @Mock
    private ObjectProvider<JaversAuditAsyncCommitter> asyncCommitterProvider;

    private JaversProperties javersProperties;
    private HibernateAuditListener listener;

    @AfterEach
    void tearDown() {
        AuditAuthorContext.clear();
    }

    @Test
    void onPostInsert_shouldCommitSynchronouslyWhenAsyncDisabled() {
        listener = createListener(false);
        AuditAuthorContext.set("batch-uploader@aramex.com");
        Charge charge = auditableCharge();

        when(auditCommitResolver.resolve(eq(charge), any(AuditableEntity.class)))
                .thenReturn(Optional.of(Map.of("awb", "AWB-1")));

        PostInsertEvent event = mock(PostInsertEvent.class);
        when(event.getEntity()).thenReturn(charge);

        listener.onPostInsert(event);

        ArgumentCaptor<String> authorCaptor = ArgumentCaptor.forClass(String.class);
        verify(javers).commit(authorCaptor.capture(), eq(charge), eq(Map.of("awb", "AWB-1")));
        assertThat(authorCaptor.getValue()).isEqualTo("batch-uploader@aramex.com");
        verify(asyncCommitterProvider, never()).getIfAvailable();
    }

    @Test
    void onPostInsert_shouldDelegateToAsyncCommitterWhenAsyncEnabled() {
        listener = createListener(true);
        JaversAuditAsyncCommitter asyncCommitter = mock(JaversAuditAsyncCommitter.class);
        when(asyncCommitterProvider.getIfAvailable()).thenReturn(asyncCommitter);

        Charge charge = auditableCharge();
        PostInsertEvent event = mock(PostInsertEvent.class);
        when(event.getEntity()).thenReturn(charge);

        listener.onPostInsert(event);

        verify(asyncCommitter).scheduleCommit(charge);
        verify(javers, never()).commit(any(), any(), any());
    }

    @Test
    void requiresPostCommitHandling_shouldFollowAsyncFlag() {
        HibernateAuditListener syncListener = createListener(false);
        HibernateAuditListener asyncListener = createListener(true);

        assertThat(syncListener.requiresPostCommitHandling(null)).isFalse();
        assertThat(asyncListener.requiresPostCommitHandling(null)).isTrue();
    }

    private HibernateAuditListener createListener(boolean asyncEnabled) {
        javersProperties = new JaversProperties();
        javersProperties.getAsync().setEnabled(asyncEnabled);
        return new HibernateAuditListener(javers, auditCommitResolver, javersProperties, asyncCommitterProvider);
    }

    private static Charge auditableCharge() {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHawbNumber("AWB-1");
        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", UUID.randomUUID());
        charge.setShipment(shipment);
        charge.setChargeCode("DTC");
        return charge;
    }
}
