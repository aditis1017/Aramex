package com.customcharge.shipment.ingest.worker.constants;

public final class IngestWorkerLogMessages {

    public static final String WORKER_SQS_RECEIVED =
            "event=shipment_ingest_worker_sqs_received payloadBytes={}";
    public static final String WORKER_SQS_POISON =
            "event=shipment_ingest_worker_sqs_poison reason={}";

    private IngestWorkerLogMessages() {
    }
}
