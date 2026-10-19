package com.customcharge.shipment.ingest.adapter.constants;

public final class IngestAdapterLogMessages {

    public static final String ADAPTER_QUEUE_DEPTH =
            "event=shipment_ingest_adapter_queue_depth queue={} messageCount={}";
    public static final String ADAPTER_SQS_FORWARDED =
            "event=shipment_ingest_adapter_sqs_forwarded queue={} payloadBytes={}";
    public static final String ADAPTER_SQS_FORWARD_FAILED =
            "event=shipment_ingest_adapter_sqs_forward_failed queue={}";

    private IngestAdapterLogMessages() {
    }
}
