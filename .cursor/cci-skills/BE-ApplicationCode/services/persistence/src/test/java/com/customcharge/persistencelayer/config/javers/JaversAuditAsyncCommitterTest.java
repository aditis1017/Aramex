package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.observability.JaversAuditMetrics;
import org.javers.core.Javers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JaversAuditAsyncCommitterTest {

    @Mock
    private Javers javers;
    @Mock
    private AuditCommitResolver auditCommitResolver;
    @Mock
    private JaversAuditMetrics javersAuditMetrics;

    @Test
    void scheduleCommit_shouldRunJaversCommitOnExecutor() {
        Executor directExecutor = Runnable::run;
        JaversAuditAsyncCommitter committer = new JaversAuditAsyncCommitter(
                javers, auditCommitResolver, directExecutor, javersAuditMetrics);
        Charge charge = auditableCharge();

        when(auditCommitResolver.resolve(eq(charge), any(AuditableEntity.class)))
                .thenReturn(Optional.of(Map.of("awb", "AWB-1")));

        committer.scheduleCommit(charge);

        verify(javers).commit(eq("SYSTEM"), eq(charge), eq(Map.of("awb", "AWB-1")));
    }

    @Test
    void scheduleCommit_shouldRecordMetricWhenCommitFails() {
        JaversAuditAsyncCommitter committer = new JaversAuditAsyncCommitter(
                javers, auditCommitResolver, Runnable::run, javersAuditMetrics);
        Charge charge = auditableCharge();

        when(auditCommitResolver.resolve(eq(charge), any(AuditableEntity.class)))
                .thenReturn(Optional.of(Map.of("awb", "AWB-1")));
        when(javers.commit(any(), any(), any())).thenThrow(new RuntimeException("boom"));

        committer.scheduleCommit(charge);

        verify(javersAuditMetrics).recordFailedCommit();
    }

    @Test
    void prepareRequest_shouldReturnEmptyForNonAuditableEntity() {
        JaversAuditAsyncCommitter committer = new JaversAuditAsyncCommitter(
                javers, auditCommitResolver, Runnable::run, javersAuditMetrics);

        Optional<JaversAuditRequest> request = committer.prepareRequest(new Object());

        assertThat(request).isEmpty();
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
