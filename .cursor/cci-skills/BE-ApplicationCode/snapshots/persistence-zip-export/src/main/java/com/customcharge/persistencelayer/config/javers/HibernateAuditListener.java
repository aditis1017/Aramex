package com.customcharge.persistencelayer.config.javers;

import com.customcharge.persistencelayer.audit.AuditAuthorResolver;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.javers.core.Javers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@EnableConfigurationProperties(JaversProperties.class)
@ConditionalOnProperty(prefix = "mvp.persistence.javers", name = "enabled", havingValue = "true")
public class HibernateAuditListener
        implements PostCommitInsertEventListener,
        PostCommitUpdateEventListener,
        PostCommitDeleteEventListener {

    private final Javers javers;
    private final AuditCommitResolver auditCommitResolver;
    private final JaversProperties javersProperties;
    private final ObjectProvider<JaversAuditAsyncCommitter> asyncCommitterProvider;

    public HibernateAuditListener(
            Javers javers,
            AuditCommitResolver auditCommitResolver,
            JaversProperties javersProperties,
            ObjectProvider<JaversAuditAsyncCommitter> asyncCommitterProvider) {
        this.javers = javers;
        this.auditCommitResolver = auditCommitResolver;
        this.javersProperties = javersProperties;
        this.asyncCommitterProvider = asyncCommitterProvider;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        handleChange(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (event.getDirtyProperties() == null || event.getDirtyProperties().length == 0) {
            return;
        }
        handleChange(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        handleChange(event.getEntity());
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        // no-op
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
        // no-op
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
        // no-op
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return javersProperties.getAsync().isEnabled();
    }

    private void handleChange(Object entity) {
        if (javersProperties.getAsync().isEnabled()) {
            JaversAuditAsyncCommitter asyncCommitter = asyncCommitterProvider.getIfAvailable();
            if (asyncCommitter == null) {
                log.warn("Javers async auditing enabled but JaversAuditAsyncCommitter is unavailable");
                commitSynchronously(entity);
                return;
            }
            asyncCommitter.scheduleCommit(entity);
            return;
        }
        commitSynchronously(entity);
    }

    private void commitSynchronously(Object entity) {
        if (entity == null || entity.getClass().getName().startsWith("org.javers")) {
            return;
        }
        Class<?> entityClass = Hibernate.getClass(entity);
        if (!entityClass.isAnnotationPresent(AuditableEntity.class)) {
            return;
        }
        AuditableEntity annotation = entityClass.getAnnotation(AuditableEntity.class);
        Optional<Map<String, String>> commitProperties = auditCommitResolver.resolve(entity, annotation);
        if (commitProperties.isEmpty()) {
            log.warn(
                    "Skipping Javers commit for {}: commit properties could not be resolved",
                    entityClass.getSimpleName());
            return;
        }
        javers.commit(AuditAuthorResolver.resolve(), entity, commitProperties.get());
    }
}
