package com.customcharge.persistencelayer.audit.service;

import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.core.metamodel.object.SnapshotType;
import org.javers.core.metamodel.property.Property;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersistenceAuditQueryEngine {

    private static final int SNAPSHOT_QUERY_LIMIT = 10_000;

    private final Javers javers;
    private final ObjectMapper objectMapper;
    private final JaversAuditCommitLookup javersAuditCommitLookup;

    public PersistenceAuditQueryEngine(
            Javers javers,
            ObjectMapper objectMapper,
            JaversAuditCommitLookup javersAuditCommitLookup) {
        this.javers = javers;
        this.objectMapper = objectMapper;
        this.javersAuditCommitLookup = javersAuditCommitLookup;
    }

    public Page<AuditCommitBlock> getAuditHistory(AuditQueryRequest request) {
        validateRequest(request);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        long totalCommits = javersAuditCommitLookup.countMatchingCommits(request);
        if (totalCommits == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<BigDecimal> pageCommitIds = javersAuditCommitLookup.findCommitIds(
                request, pageable.getOffset(), pageable.getPageSize());
        if (pageCommitIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, totalCommits);
        }

        List<CdoSnapshot> snapshots = fetchSnapshotsForCommits(request, pageCommitIds);
        Map<String, CdoSnapshot> versionIndex = buildVersionIndex(snapshots);
        populateMissingPriorSnapshots(snapshots, versionIndex);

        Map<String, List<CdoSnapshot>> byCommit = groupByCommitId(snapshots);
        List<AuditCommitBlock> content = new ArrayList<>();
        for (BigDecimal commitId : pageCommitIds) {
            String commitKey = commitIdKey(commitId);
            List<CdoSnapshot> snaps = byCommit.getOrDefault(commitKey, List.of());
            if (snaps.isEmpty()) {
                continue;
            }
            content.add(buildCommitBlock(commitKey, snaps, versionIndex));
        }

        return new PageImpl<>(content, pageable, totalCommits);
    }

    public long countAuditExportRows(AuditQueryRequest request) {
        validateRequest(request);
        return 1L + javersAuditCommitLookup.countMatchingSnapshots(request);
    }

    private List<CdoSnapshot> fetchSnapshotsForCommits(
            AuditQueryRequest request, List<BigDecimal> commitIds) {
        QueryBuilder builder = snapshotQueryBuilder(request);
        builder = builder.withCommitIds(commitIds);
        return javers.findSnapshots(builder.limit(SNAPSHOT_QUERY_LIMIT).build());
    }

    private QueryBuilder snapshotQueryBuilder(AuditQueryRequest request) {
        Optional<String> managedTypeFqcn = request.getManagedType()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim);
        if (managedTypeFqcn.isPresent()) {
            Class<?> entityClass;
            try {
                entityClass = Class.forName(managedTypeFqcn.get());
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        "Managed Type must be a loadable fully-qualified class name: " + managedTypeFqcn.get(), e);
            }
            return QueryBuilder.byClass(entityClass);
        }
        return QueryBuilder.anyDomainObject();
    }

    private Map<String, List<CdoSnapshot>> groupByCommitId(List<CdoSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> s.getCommitMetadata() != null && s.getCommitMetadata().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getCommitMetadata().getId().toString()));
    }

    private static String commitIdKey(BigDecimal commitId) {
        return CommitId.valueOf(commitId).toString();
    }

    private void populateMissingPriorSnapshots(
            List<CdoSnapshot> snapshots, Map<String, CdoSnapshot> versionIndex) {
        Set<String> requested = new HashSet<>();
        for (CdoSnapshot snap : snapshots) {
            if (snap.getType() == SnapshotType.INITIAL || snap.getGlobalId() == null || snap.getVersion() <= 1) {
                continue;
            }
            long prev = snap.getVersion() - 1;
            String key = versionKey(snap.getGlobalId(), prev);
            if (versionIndex.containsKey(key) || requested.contains(key)) {
                continue;
            }
            CdoSnapshot prior = loadSnapshotVersion(snap.getGlobalId(), prev);
            if (prior != null) {
                versionIndex.put(key, prior);
            }
            requested.add(key);
        }
    }

    private static Instant resolveCommitInstant(CommitMetadata meta) {
        if (meta == null) {
            return null;
        }
        Instant instant = meta.getCommitDateInstant();
        if (instant != null) {
            return instant;
        }
        return legacyCommitDateToInstant(meta.getCommitDate());
    }

    private static Instant legacyCommitDateToInstant(Object commitDate) {
        if (commitDate == null) {
            return null;
        }
        if (commitDate instanceof Instant i) {
            return i;
        }
        if (commitDate instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
        if (commitDate instanceof java.util.Date d) {
            return d.toInstant();
        }
        return null;
    }

    private Map<String, CdoSnapshot> buildVersionIndex(List<CdoSnapshot> snapshots) {
        Map<String, CdoSnapshot> index = new HashMap<>();
        for (CdoSnapshot s : snapshots) {
            if (s.getGlobalId() != null) {
                index.put(versionKey(s.getGlobalId(), s.getVersion()), s);
            }
        }
        return index;
    }

    private static String versionKey(GlobalId globalId, long version) {
        return globalId.value() + ":" + version;
    }

    private AuditCommitBlock buildCommitBlock(
            String commitId,
            List<CdoSnapshot> snapshots,
            Map<String, CdoSnapshot> versionIndex) {

        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId(commitId);
        CdoSnapshot first = snapshots.get(0);
        block.setManagedType(managedTypeSimpleName(first));
        CommitMetadata meta = first.getCommitMetadata();
        if (meta != null) {
            block.setAuthor(meta.getAuthor());
            block.setCommitDate(resolveCommitInstant(meta));
        }

        for (CdoSnapshot snap : snapshots) {
            block.getStateChangeData().add(buildStateChangeEntry(snap, versionIndex));
        }
        return block;
    }

    private AuditStateChangeEntry buildStateChangeEntry(
            CdoSnapshot snap, Map<String, CdoSnapshot> versionIndex) {
        JsonNode currentState = snapshotToJson(snap);
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        entry.setType(snap.getType() != null ? snap.getType().name() : null);
        entry.setCurrentState(currentState);
        entry.setChangeParameter(changeParameterFromSnapshot(snap, currentState));
        entry.setPreState(resolvePreState(snap, versionIndex));
        return entry;
    }

    private static String managedTypeSimpleName(CdoSnapshot snap) {
        GlobalId gid = snap.getGlobalId();
        if (!(gid instanceof InstanceId instanceId)) {
            return null;
        }
        String fqcn = instanceId.getTypeName();
        if (fqcn == null || fqcn.isEmpty()) {
            return null;
        }
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }

    private JsonNode resolvePreState(CdoSnapshot snap, Map<String, CdoSnapshot> versionIndex) {
        if (snap.getType() == SnapshotType.INITIAL) {
            return null;
        }
        if (snap.getGlobalId() == null || snap.getVersion() <= 1) {
            return null;
        }
        long prev = snap.getVersion() - 1;
        CdoSnapshot prior = versionIndex.get(versionKey(snap.getGlobalId(), prev));
        return prior == null ? null : snapshotToJson(prior);
    }

    private CdoSnapshot loadSnapshotVersion(GlobalId globalId, long version) {
        if (!(globalId instanceof InstanceId instanceId)) {
            return null;
        }
        try {
            Class<?> entityClass = Class.forName(instanceId.getTypeName());
            JqlQuery q = QueryBuilder.byInstanceId(instanceId.getCdoId(), entityClass)
                    .withVersion(version)
                    .build();
            List<CdoSnapshot> list = javers.findSnapshots(q);
            return list.isEmpty() ? null : list.get(0);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private ObjectNode changeParameterFromSnapshot(CdoSnapshot snap, JsonNode fullSnapshot) {
        ObjectNode changeParameter = objectMapper.createObjectNode();
        for (String name : Optional.ofNullable(snap.getChanged()).orElse(List.of())) {
            if (fullSnapshot.has(name)) {
                changeParameter.set(name, fullSnapshot.get(name));
            } else {
                changeParameter.set(name, objectMapper.valueToTree(snap.getPropertyValue(name)));
            }
        }
        return changeParameter;
    }

    private JsonNode snapshotToJson(CdoSnapshot snap) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            for (Property p : snap.getManagedType().getProperties()) {
                String name = p.getName();
                node.set(name, objectMapper.valueToTree(snap.getPropertyValue(name)));
            }
            return node;
        } catch (RuntimeException ex) {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private void validateRequest(AuditQueryRequest request) {
        Objects.requireNonNull(request, "AuditQueryRequest must not be null");
        Objects.requireNonNull(request.getFromDate(), "fromDate (UTC instant) must not be null");
        Objects.requireNonNull(request.getToDate(), "toDate (UTC instant) must not be null");
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new IllegalArgumentException("Start instant must be before or equal to end instant");
        }
        if (request.getPage() < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (request.getSize() <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
    }

}
