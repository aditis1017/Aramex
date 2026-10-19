# Ingest adapter (extractable module)

`com.customcharge.shipment.ingest.adapter` is the **Rabbit → SQS wrapper only**. It must not call `ShipmentService`, Drools, or JPA.

| Piece | Role |
|-------|------|
| `rabbit.RabbitToSqsIngestAdapterListener` | `@RabbitListener` on `queue.client-shipment` |
| `publish.ShipmentInboundSqsPublisher` | Envelope JSON → SQS |
| `model.ShipmentInboundEnvelope` | Wire contract shared with worker |

Enable with `shipment.ingest.adapter.enabled=true` (alias env: `SHIPMENT_INGEST_ADAPTER_ENABLED` or legacy `SHIPMENT_SQS_BUFFER_ENABLED`).

When moving to a separate repo, copy this package + `IngestAdapterProperties`, persistence AWS SQS/S3 config, and Rabbit listener factory settings. The worker stays in shipment (`ingest.worker`) until split further.
