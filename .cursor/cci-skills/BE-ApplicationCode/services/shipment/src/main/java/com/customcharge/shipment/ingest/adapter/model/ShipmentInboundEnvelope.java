package com.customcharge.shipment.ingest.adapter.model;

/**
 * Wire format from Rabbit adapter to SQS worker ({@code payloadXml} is unchanged Boomi/client XML).
 */
public record ShipmentInboundEnvelope(String payloadXml) {
}
