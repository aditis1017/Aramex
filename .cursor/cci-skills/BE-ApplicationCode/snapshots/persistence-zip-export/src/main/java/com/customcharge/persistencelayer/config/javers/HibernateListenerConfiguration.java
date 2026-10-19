package com.customcharge.persistencelayer.config.javers;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JaversProperties.class)
@ConditionalOnProperty(prefix = "mvp.persistence.javers", name = "enabled", havingValue = "true")
public class HibernateListenerConfiguration {

    private final HibernateAuditListener listener;
    private final EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void registerListeners() {

        SessionFactoryImpl sessionFactory =
                entityManagerFactory.unwrap(SessionFactoryImpl.class);

        EventListenerRegistry registry =
                sessionFactory.getServiceRegistry()
                        .getService(EventListenerRegistry.class);

        registry.appendListeners(EventType.POST_INSERT, listener);
        registry.appendListeners(EventType.POST_UPDATE, listener);
        registry.appendListeners(EventType.POST_DELETE, listener);
    }
}