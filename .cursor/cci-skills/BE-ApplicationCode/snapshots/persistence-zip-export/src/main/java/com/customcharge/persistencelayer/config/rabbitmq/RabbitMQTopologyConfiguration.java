package com.customcharge.persistencelayer.config.rabbitmq;

import com.customcharge.persistencelayer.messaging.MessagePublisher;
import com.customcharge.persistencelayer.messaging.RabbitMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnProperty(prefix = "mvp.persistence.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQTopologyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTopologyConfiguration.class);

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        admin.setIgnoreDeclarationExceptions(false);
        return admin;
    }

    @Bean("rabbitPublisher")
    @ConditionalOnBean(AmqpTemplate.class)
    @ConditionalOnMissingBean(name = "rabbitPublisher")
    public MessagePublisher rabbitMessagePublisher(AmqpTemplate amqpTemplate,
                                                   RabbitMQProperties properties) {
        return new RabbitMessagePublisher(amqpTemplate, properties);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void declareTopologyOnStartup(ContextRefreshedEvent event) {
        ApplicationContext ctx = event.getApplicationContext();
        if (!ctx.containsBean("rabbitAdmin")) {
            return;
        }
        RabbitAdmin admin = ctx.getBean(RabbitAdmin.class);
        RabbitMQProperties properties = ctx.getBean(RabbitMQProperties.class);
        properties.loadFromJsonIfPresent();
        DirectExchange exchange = new DirectExchange(
                properties.getExchangeName(),
                properties.isDurable(),
                false
        );
        admin.declareExchange(exchange);
        log.info("Declared RabbitMQ exchange: {}", properties.getExchangeName());
        for (RabbitMQProperties.QueueBinding qb : properties.getQueues()) {
            if (qb.getName() == null || qb.getName().isBlank()) {
                continue;
            }
            Queue queue = new Queue(qb.getName(), properties.isDurable(), false, false);
            admin.declareQueue(queue);
            if (qb.getRoutingKey() != null && !qb.getRoutingKey().isBlank()) {
                Binding binding = BindingBuilder.bind(queue).to(exchange).with(qb.getRoutingKey());
                admin.declareBinding(binding);
                log.info("Declared queue '{}' bound to '{}' with routing key '{}'",
                        qb.getName(), properties.getExchangeName(), qb.getRoutingKey());
            }
        }
    }
}
