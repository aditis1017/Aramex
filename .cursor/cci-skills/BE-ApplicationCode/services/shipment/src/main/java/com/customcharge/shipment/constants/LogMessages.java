package com.customcharge.shipment.constants;

public final class LogMessages {

    public static final String SHIPMENT_MESSAGE_RECEIVED = "event=shipment_message_received hawb={}";
    public static final String SHIPMENT_FULLNESS_EVALUATED = "event=shipment_fullness_evaluated hawb={} isFullShipment={}";
    public static final String SHIPMENT_INCOMPLETE_SKIPPED = "event=shipment_incomplete_skipped hawb={}";
    public static final String SHIPMENT_DUPLICATE_SKIPPED = "event=shipment_duplicate_skipped hawb={}";
    public static final String SHIPMENT_DUPLICATE_CHARGES_BACKFILL =
            "event=shipment_duplicate_charges_backfill hawb={}";
    public static final String SHIPMENT_INGEST_ROLLBACK =
            "event=shipment_ingest_rollback hawb={} shipmentId={}";
    public static final String SHIPMENT_FRDM_FLAG_SET = "event=shipment_frdm_flag_set hawb={} frdmFlag={}";
    public static final String SHIPMENT_AR_ACCOUNT_LOOKUP =
            "event=shipment_ar_account_lookup hawb={} arAccountFound={} accountNumber={} entityCode={}";
    public static final String SHIPMENT_TYPE_FROM_AR_ACCOUNT =
            "event=shipment_type_from_ar_account hawb={} shipmentType={}";
    public static final String SHIPMENT_B2B_KEYWORD_EVALUATED =
            "event=shipment_b2b_keyword_evaluated hawb={} consigneeName={} b2bShipment={}";
    public static final String SHIPMENT_B2B_KEYWORD_SKIPPED =
            "event=shipment_b2b_keyword_skipped hawb={} businessType={}";
    public static final String SHIPMENT_BUSINESS_TYPE_SET =
            "event=shipment_business_type_set hawb={} shipmentBusinessType={} messageBusinessType={}";
    public static final String SHIPMENT_VALIDATION_FAILED = "event=shipment_validation_failed reason={}";
    public static final String SHIPMENT_PROCESSING_FAILED = "event=shipment_processing_failed hawb={}";
    public static final String SHIPMENT_PROCESSING_UNEXPECTED_ERROR = "event=shipment_processing_unexpected_error";
    public static final String SHIPMENT_PROCESSING_TIMING =
            "event=shipment_processing_timing hawb={} processType={} success={} durationMs={}";
    public static final String SHIPMENT_PHASE_TIMING =
            "event=shipment_phase_timing hawb={} phase={} durationMs={}";
    public static final String SHIPMENT_PHASE_SUMMARY =
            "event=shipment_phase_summary hawb={} phases={} totalMs={}";
    public static final String SHIPMENT_LISTENER_CONSUMPTION_COUNTER =
            "event=shipment_listener_consumption_counter hawb={} podCount={}";
    public static final String SHIPMENT_LISTENER_QUEUE_DEPTH =
            "event=shipment_listener_queue_depth queue={} messageCount={}";
    public static final String SHIPMENT_LISTENER_MESSAGE_ARRIVED =
            "event=shipment_listener_message_arrived receivedCount={} queue={} queueMessageCount={} sincePreviousMessageMs={}";
    public static final String SHIPMENT_LISTENER_RECEIVE_TIMING =
            "event=shipment_listener_receive_timing receivedCount={} durationMs={}";
    public static final String SHIPMENT_RUNTIME_CONFIGURATION =
            "event=shipment_runtime_configuration queue={} rabbitHost={} rabbitVhost={} "
                    + "listenerConcurrency={} listenerMaxConcurrency={} listenerPrefetch={} "
                    + "listenerDefaultRequeueRejected={} datasourceMaxPoolSize={} datasourceMinIdle={}";
    public static final String SHIPMENT_RUNTIME_LISTENER_CONTAINER =
            "event=shipment_runtime_listener_container queues={} configuredConcurrency={} "
                    + "configuredMaxConcurrency={} activeConsumerCount={} configuredPrefetch={} running={}";
    public static final String SHIPMENT_LISTENER_MATRIX_TRACE =
            "event=shipment_listener_matrix instanceId={} queue={} rabbitHost={} rabbitVhost={} "
                    + "datasourceMaxPoolSize={} datasourceMinIdle={} listenerQueues={} "
                    + "configuredMinConcurrency={} configuredMaxConcurrency={} annotationConcurrency={} "
                    + "configuredPrefetch={} configuredDefaultRequeueRejected={} activeConsumerCount={} running={}";
    public static final String SHIPMENT_MESSAGES_CONSUMED_PER_MINUTE =
            "event=shipment_messages_consumed_per_minute instanceId={} podCount={} allPodsCount={}";

    public static final String CHARGE_CREATION_SKIPPED_NULL = "event=charge_creation_skipped reason=shipment_null";
    public static final String CHARGE_CREATION_SKIPPED_NO_CODES =
            "event=charge_creation_skipped shipmentTypeCode={} paymentType={}";

    public static final String CHARGE_LINE_ITEMS_PREPARED =
            "event=charge_line_items_prepared count={} chargeId={} chargeCode={}";
    public static final String CHARGE_LINE_ITEMS_RECALCULATED_COPIED =
            "event=charge_line_items_recalculated_copied count={} chargeId={} chargeCode={} hawb={}";
    public static final String CHARGE_LINE_ITEMS_RECALCULATED_CIF =
            "event=charge_line_items_recalculated_cif count={} chargeId={} chargeCode={} hawb={}";
    public static final String CHARGE_LINE_ITEMS_SKIPPED_NO_ENTITY =
            "event=charge_line_items_skipped reason=missing_billing_entity chargeCode={}";
    public static final String CHARGE_LINE_ITEMS_SKIPPED_NO_ACCOUNT =
            "event=charge_line_items_skipped reason=no_billing_account chargeCode={} entity={}";

    public static final String SH267_SKIPPED_ALREADY_EXEMPTED =
            "event=sh267_skipped reason=already_exempted hawb={}";
    public static final String CIF_RECALCULATION_SKIPPED_NO_CHARGES =
            "event=cif_recalculation_skipped reason=no_charges hawb={}";
    public static final String SH267_RECALCULATION_SKIPPED =
            "event=sh267_recalculation_skipped hawb={} mode={}";
    public static final String SH267_SKIPPED_UNPARSEABLE_COMMENT1 =
            "event=sh267_skipped reason=unparseable_comment1 hawb={}";
    public static final String SH267_SKIPPED_MISSING_HAWB = "event=sh267_skipped reason=missing_hawb";
    public static final String SH267_SKIPPED_SHIPMENT_NOT_FOUND =
            "event=sh267_skipped reason=shipment_not_found hawb={}";
    public static final String SH267_SKIPPED_SHIPMENT_ALREADY_EXEMPTED =
            "event=sh267_skipped reason=shipment_already_exempted hawb={}";
    public static final String SH267_PROCESSED = "event=sh267_processed hawb={}";
    public static final String SH267_STORED_WITHOUT_RECALCULATION =
            "event=sh267_stored_without_recalculation hawb={} amount={} currency={}";
    public static final String SH267_CONVERTED_CIF =
            "event=sh267_converted_cif fromAmount={} fromCurrency={} toAmount={} toCurrency={} hawb={}";
    public static final String CIF_LINE_ITEMS_RECALCULATED =
            "event=cif_line_items_recalculated chargeId={} chargeCode={} hawb={}";
    public static final String CIF_LV_LINE_ITEMS_RECALCULATED =
            "event=cif_lv_line_items_recalculated chargeId={} chargeCode={} hawb={}";

    public static final String RATE_SHEET_SKIPPED_NO_ENTITY =
            "event=rate_sheet_skipped reason=missing_billing_entity chargeType={}";
    public static final String ENTITY_SETTINGS_ACCOUNT_NOT_FOUND =
            "event=entity_settings_account_not_found entity={} chargeType={}";
    public static final String RATE_SHEET_NO_BILLABLE_ITEMS =
            "event=rate_sheet_no_billable_items rateSheetId={} shipmentType={}";

    public static final String BILLING_ENTITY_NOT_FOUND =
            "event=billing_entity_not_found code={} chargeCode={}";

    public static final String FX_CONVERSION_SKIPPED =
            "event=fx_conversion_skipped chargeCode={} hawb={} fromCurrency={} toCurrency={}";
    public static final String EXCHANGE_RATE_SERVICE_UNAVAILABLE =
            "event=exchange_rate_service_unavailable fromCurrency={} toCurrency={}";

    public static final String CHARGE_CURRENCY_NOT_RESOLVED =
            "event=charge_currency_not_resolved chargeCode={} hawb={}";
    public static final String ACCOUNT_CURRENCY_NO_ENTITY =
            "event=account_currency_no_entity accountId={}";
    public static final String ACCOUNT_CURRENCY_NO_ACTIVE_MAPPING =
            "event=account_currency_no_active_mapping entity={} accountId={}";

    public static final String XML_RECEIVED_FROM_QUEUE = "event=shipment_xml_received payload={}";
    public static final String XML_CONVERSION_FAILED = "event=shipment_xml_conversion_failed";

    public static final String SHIPMENT_SQS_FORWARDED =
            "event=shipment_sqs_forwarded queue={} payloadBytes={}";
    public static final String SHIPMENT_SQS_FORWARD_FAILED =
            "event=shipment_sqs_forward_failed queue={}";
    public static final String SHIPMENT_SQS_INGEST_RECEIVED =
            "event=shipment_sqs_ingest_received payloadBytes={}";
    public static final String SHIPMENT_SQS_INGEST_POISON =
            "event=shipment_sqs_ingest_poison reason={}";

    public static final String DROOLS_RULE_EXECUTION_FAILED =
            "event=drools_rule_execution_failed destination={} context={} reason={}";
    public static final String DROOLS_RULE_EXECUTION_TIMING =
            "event=drools_rule_execution_timing hawb={} context={} ruleRef={} durationMs={}";
    public static final String DROOLS_RULES_BATCH_TIMING =
            "event=drools_rules_batch_timing hawb={} context={} rulesExecuted={} resolved={} durationMs={}";
    public static final String DROOLS_RULE_MATCHED =
            "event=drools_rule_matched hawb={} ruleKind={} resolvedType={} ruleId={} priority={} domainCode={}";

    public static final String REDIS_LOOKUP_FAILED = "event=redis_lookup_failed key={} reason={}";

    private LogMessages() {
    }
}
