package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.converter.ShipmentXmlConverter;
import com.customcharge.shipment.enums.PICode;
import com.customcharge.shipment.mapper.XmlShipmentDtoMapper;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.cif.ShipmentCifUpdateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_LISTENER_CONSUMPTION_COUNTER;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_PROCESSING_FAILED;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_PROCESSING_TIMING;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_PROCESSING_UNEXPECTED_ERROR;
import static com.customcharge.shipment.constants.LogMessages.SHIPMENT_VALIDATION_FAILED;
import static com.customcharge.shipment.constants.QueueMessages.PROCESSING_FAILED;

/**
 * Core shipment ingest processing shared by Rabbit (direct) and SQS consumers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentMessageProcessor {

    ShipmentService shipmentService;
    ShipmentCifUpdateService shipmentCifUpdateService;
    XmlShipmentDtoMapper xmlShipmentDtoMapper;
    ShipmentCompletenessChecker shipmentCompletenessChecker;
    ShipmentMessageConsumptionTracker consumptionTracker;
    ShipmentProcessingPhaseTimer phaseTimer;
    ShipmentXmlConverter shipmentXmlConverter;

    public void processFromRabbitMessage(Message message) {
        JsonNode xmlNode = shipmentXmlConverter.fromMessage(message);
        processXmlTree(xmlNode, IngestChannel.RABBIT);
    }

    public void processFromXmlPayload(String payloadXml) {
        JsonNode xmlNode = shipmentXmlConverter.fromXmlString(payloadXml);
        processXmlTree(xmlNode, IngestChannel.SQS);
    }

    private void processXmlTree(JsonNode xmlNode, IngestChannel channel) {
        ShipmentDto message = xmlShipmentDtoMapper.map(xmlNode);
        processShipmentDto(message, channel);
    }

    public void processShipmentDto(ShipmentDto message, IngestChannel channel) {
        long startedAtNanos = System.nanoTime();
        String hawb = "unknown";
        String processType = "shipment";
        boolean success = false;
        try {
            hawb = message.getHawb() != null ? message.getHawb() : "unknown";

            if (PICode.SH267.getName().equalsIgnoreCase(message.getPiNumber())) {
                processType = "SH267";
                shipmentCifUpdateService.handle(message);
                success = true;
                consumptionTracker.recordConsumed();
                return;
            }

            if (PICode.SH022.getName().equalsIgnoreCase(message.getPiNumber())) {
                processType = "SH022";
                phaseTimer.run(hawb, "mawbRules", () -> shipmentService.handleSh022(message));
                success = true;
                consumptionTracker.recordConsumed();
                return;
            }

            phaseTimer.run(hawb, "fullnessRules", () ->
                    shipmentCompletenessChecker.applyShipmentFilterRules(message));

            shipmentService.processShipment(message);
            success = true;
            consumptionTracker.recordConsumed();
        } catch (IncompleteShipmentException ex) {
            throw channel == IngestChannel.RABBIT
                    ? new AmqpRejectAndDontRequeueException(ex.getMessage())
                    : ex;
        } catch (IllegalArgumentException ex) {
            log.warn(SHIPMENT_VALIDATION_FAILED, ex.getMessage());
            throw channel == IngestChannel.RABBIT
                    ? new AmqpRejectAndDontRequeueException(ex.getMessage())
                    : ex;
        } catch (AmqpRejectAndDontRequeueException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error(SHIPMENT_PROCESSING_FAILED, hawb, ex);
            throw channel == IngestChannel.RABBIT
                    ? new AmqpRejectAndDontRequeueException(PROCESSING_FAILED)
                    : ex;
        } catch (Exception ex) {
            log.error(SHIPMENT_PROCESSING_UNEXPECTED_ERROR, ex);
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
            log.debug(SHIPMENT_PROCESSING_TIMING, hawb, processType, success, durationMs);
            phaseTimer.logSummary(hawb);
            phaseTimer.clear();
            if (success) {
                log.info(SHIPMENT_LISTENER_CONSUMPTION_COUNTER, hawb, consumptionTracker.currentMinutePodCount());
            }
        }
    }

    public enum IngestChannel {
        RABBIT,
        SQS
    }
}
