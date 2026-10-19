package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.audit.AuditAuthorResolver;
import com.customcharge.persistencelayer.observability.JaversAuditMetrics;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.javers.core.Javers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
public class JaversAuditAsyncCommitter {

    private final Javers javers;
    private final AuditCommitResolver auditCommitResolver;
    private final Executor auditExecutor;
    private final JaversAuditMetrics javersAuditMetrics;

    public JaversAuditAsyncCommitter(
            Javers javers,
            AuditCommitResolver auditCommitResolver,
            Executor auditExecutor,
            JaversAuditMetrics javersAuditMetrics) {
        this.javers = javers;
        this.auditCommitResolver = auditCommitResolver;
        this.auditExecutor = auditExecutor;
        this.javersAuditMetrics = javersAuditMetrics;
    }

    public void scheduleCommit(Object entity) {
        Optional<JaversAuditRequest> request = prepareRequest(entity);
        if (request.isEmpty()) {
            return;
        }
        JaversAuditRequest auditRequest = request.get();
        auditExecutor.execute(() -> commitSafely(auditRequest));
    }

    Optional<JaversAuditRequest> prepareRequest(Object entity) {
        if (entity == null || entity.getClass().getName().startsWith("org.javers")) {
            return Optional.empty();
        }
        Class<?> entityClass = Hibernate.getClass(entity);
        if (!entityClass.isAnnotationPresent(AuditableEntity.class)) {
            return Optional.empty();
        }
        AuditableEntity annotation = entityClass.getAnnotation(AuditableEntity.class);
        Optional<Map<String, String>> commitProperties = auditCommitResolver.resolve(entity, annotation);
        if (commitProperties.isEmpty()) {
            log.warn(
                    "Skipping Javers commit for {}: commit properties could not be resolved",
                    entityClass.getSimpleName());
            return Optional.empty();
        }
        return Optional.of(new JaversAuditRequest(
                AuditAuthorResolver.resolve(),
                entity,
                commitProperties.get()));
    }

    private void commitSafely(JaversAuditRequest request) {
        try {
            javers.commit(request.author(), request.entity(), request.commitProperties());
        } catch (RuntimeException ex) {
            javersAuditMetrics.recordFailedCommit();
            log.error(
                    "event=javers_async_commit_failed entityClass={} author={}",
                    Hibernate.getClass(request.entity()).getSimpleName(),
                    request.author(),
                    ex);
        }
    }
}
