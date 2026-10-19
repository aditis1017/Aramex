package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.converter.ShipmentXmlConverter;
import com.customcharge.shipment.mapper.XmlShipmentDtoMapper;
import com.customcharge.shipment.service.IncompleteShipmentException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.customcharge.shipment.constants.QueueMessages.INCOMPLETE_SHIPMENT_IGNORED;
import static com.customcharge.shipment.constants.QueueMessages.PROCESSING_FAILED;
import static com.customcharge.shipment.observability.ShipmentTrackingLogger.CHANNEL_RABBIT;
import static com.customcharge.shipment.observability.ShipmentTrackingLogger.CHANNEL_SQS;

/**
 * Records processing failures on the worker path without logging SQS ingest volume.
 */
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentIngestObservabilityAspect {

    AsyncShipmentIngestMetrics ingestMetrics;
    ShipmentTrackingLogger trackingLogger;
    ShipmentXmlConverter shipmentXmlConverter;
    XmlShipmentDtoMapper xmlShipmentDtoMapper;

    @Around("execution(* com.customcharge.shipment.service.ShipmentMessageProcessor.processFromRabbitMessage(..))")
    public Object observeRabbitProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        Message message = (Message) joinPoint.getArgs()[0];
        JsonNode xmlNode = shipmentXmlConverter.fromMessage(message);
        return observeProcessingFailures(xmlNode, joinPoint, CHANNEL_RABBIT);
    }

    @Around("execution(* com.customcharge.shipment.service.ShipmentMessageProcessor.processFromXmlPayload(..))")
    public Object observeSqsProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        String payloadXml = (String) joinPoint.getArgs()[0];
        JsonNode xmlNode = shipmentXmlConverter.fromXmlString(payloadXml);
        return observeProcessingFailures(xmlNode, joinPoint, CHANNEL_SQS);
    }

    private Object observeProcessingFailures(JsonNode xmlNode, ProceedingJoinPoint joinPoint, String ingestChannel)
            throws Throwable {
        ShipmentDto dto;
        try {
            dto = xmlShipmentDtoMapper.map(xmlNode);
        } catch (IllegalArgumentException ex) {
            recordFailure("unknown", "unknown", "validation", ex.getMessage(), ingestChannel);
            throw ex;
        }
        String hawb = dto.getHawb() != null ? dto.getHawb() : "unknown";
        String destinationEntity = ShipmentDestinationEntitySupport.resolve(dto);

        try {
            return joinPoint.proceed();
        } catch (IncompleteShipmentException ex) {
            recordFailure(hawb, destinationEntity, "incomplete", ex.getMessage(), ingestChannel);
            throw ex;
        } catch (IllegalArgumentException ex) {
            recordFailure(hawb, destinationEntity, "validation", ex.getMessage(), ingestChannel);
            throw ex;
        } catch (AmqpRejectAndDontRequeueException ex) {
            recordFailure(hawb, destinationEntity, inferAmqpFailureType(ex), ex.getMessage(), ingestChannel);
            throw ex;
        } catch (RuntimeException ex) {
            recordFailure(hawb, destinationEntity, "processing", ex.getMessage(), ingestChannel);
            throw ex;
        } catch (Exception ex) {
            recordFailure(hawb, destinationEntity, "unexpected", ex.getMessage(), ingestChannel);
            throw ex;
        }
    }

    private void recordFailure(
            String hawb,
            String destinationEntity,
            String failureType,
            String reason,
            String ingestChannel) {
        ingestMetrics.recordEntityFailed(destinationEntity, failureType);
        trackingLogger.logFailedAwb(hawb, reason, destinationEntity, failureType, ingestChannel);
    }

    private static String inferAmqpFailureType(AmqpRejectAndDontRequeueException ex) {
        String message = ex.getMessage();
        if (INCOMPLETE_SHIPMENT_IGNORED.equals(message)) {
            return "incomplete";
        }
        if (PROCESSING_FAILED.equals(message)) {
            return "processing";
        }
        return "validation";
    }
}
