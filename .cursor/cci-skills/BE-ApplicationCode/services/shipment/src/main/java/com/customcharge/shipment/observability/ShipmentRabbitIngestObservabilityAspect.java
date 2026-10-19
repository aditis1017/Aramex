package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.converter.ShipmentXmlConverter;
import com.customcharge.shipment.mapper.XmlShipmentDtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * Records Rabbit queue receive metrics and Rabbit-only SHIPMENT_ENTERED tracking logs.
 */
@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentRabbitIngestObservabilityAspect {

    AsyncShipmentIngestMetrics ingestMetrics;
    ShipmentTrackingLogger trackingLogger;
    ShipmentXmlConverter shipmentXmlConverter;
    XmlShipmentDtoMapper xmlShipmentDtoMapper;

    @Before("execution(* com.customcharge.shipment.listener.ShipmentDirectRabbitIngestListener.onMessage(..)) && args(message)")
    public void beforeDirectRabbitIngest(Message message) {
        recordRabbitIngest(message, "direct");
    }

    @Before("execution(* com.customcharge.shipment.ingest.adapter.rabbit.RabbitToSqsIngestAdapterListener.onMessage(..)) && args(message)")
    public void beforeAdapterRabbitIngest(Message message) {
        recordRabbitIngest(message, "adapter");
    }

    private void recordRabbitIngest(Message message, String path) {
        ingestMetrics.recordRabbitReceived(path);
        try {
            JsonNode xmlNode = shipmentXmlConverter.fromMessage(message);
            ShipmentDto dto = xmlShipmentDtoMapper.map(xmlNode);
            String hawb = dto.getHawb() != null ? dto.getHawb() : "unknown";
            String destinationEntity = ShipmentDestinationEntitySupport.resolve(dto);
            ingestMetrics.recordMessageEntered(destinationEntity);
            trackingLogger.logEntered(hawb, destinationEntity, ShipmentTrackingLogger.CHANNEL_RABBIT);
        } catch (RuntimeException ignored) {
            trackingLogger.logEntered("unknown", "unknown", ShipmentTrackingLogger.CHANNEL_RABBIT);
        }
    }
}
