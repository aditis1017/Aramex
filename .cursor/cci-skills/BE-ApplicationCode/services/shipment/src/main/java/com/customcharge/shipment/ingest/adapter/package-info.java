/**
 * Rabbit → SQS ingest adapter (standalone boundary).
 * <p>
 * Listens on the client shipment Rabbit queue and publishes raw XML to SQS only.
 * Intended for extraction into a separate deployable/repository; the shipment worker
 * ({@code com.customcharge.shipment.ingest.worker}) consumes the same SQS queue.
 */
package com.customcharge.shipment.ingest.adapter;
