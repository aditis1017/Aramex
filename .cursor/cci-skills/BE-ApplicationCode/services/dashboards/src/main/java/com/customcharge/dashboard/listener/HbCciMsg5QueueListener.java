package com.customcharge.dashboard.listener;

import com.customcharge.dashboard.constants.MessagingConstants;
import com.customcharge.dashboard.service.HbCciMsg5ShipmentSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HbCciMsg5QueueListener {

    private static final Logger log = LoggerFactory.getLogger(HbCciMsg5QueueListener.class);

    private final HbCciMsg5ShipmentSaveService saveService;

    public HbCciMsg5QueueListener(HbCciMsg5ShipmentSaveService saveService) {
        this.saveService = saveService;
    }

//    @RabbitListener(queues = "${" + MessagingConstants.QUEUE_HB_CCI_MSG5_PROPERTY + "}")
//    public void listen(String message) {
//        log.info("Received message from RabbitMQ: {}", message);
//        saveService.saveRawPayload(message);
//    }
}

