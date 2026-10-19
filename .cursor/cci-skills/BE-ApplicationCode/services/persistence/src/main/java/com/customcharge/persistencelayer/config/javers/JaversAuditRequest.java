package com.customcharge.persistencelayer.config.javers;

import java.util.Map;

record JaversAuditRequest(String author, Object entity, Map<String, String> commitProperties) {
}
