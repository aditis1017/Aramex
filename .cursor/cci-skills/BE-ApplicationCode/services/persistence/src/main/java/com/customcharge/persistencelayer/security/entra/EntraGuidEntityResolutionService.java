package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.EntraGuidEntityMap;
import com.customcharge.persistencelayer.repository.EntraGuidEntityMapRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads {@link EntraGuidEntityMap} rows into memory and resolves Entra {@code groups} claim object ids to
 * {@code entity_code} values for storage on the {@code users.groups} column.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class EntraGuidEntityResolutionService {

    private static final Logger log = LoggerFactory.getLogger(EntraGuidEntityResolutionService.class);

    private static final Pattern TOKEN_GROUP_AS_OBJECT_ID =
            Pattern.compile("^\\{?([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})}?$");

    private final EntraGuidEntityMapRepository entraGuidEntityMapRepository;

    private volatile Map<String, String> guidToEntityCode = Map.of();

    public EntraGuidEntityResolutionService(EntraGuidEntityMapRepository entraGuidEntityMapRepository) {
        this.entraGuidEntityMapRepository = entraGuidEntityMapRepository;
    }

    @PostConstruct
    public void reloadCache() {
        Map<String, String> m = new HashMap<>();
        for (EntraGuidEntityMap row : entraGuidEntityMapRepository.findAll()) {
            if (row.getGuid() == null || row.getGuid().isBlank()) {
                continue;
            }
            String key = row.getGuid().trim().toLowerCase(Locale.ROOT);
            if (row.getEntityCode() == null || row.getEntityCode().isBlank()) {
                continue;
            }
            m.putIfAbsent(key, row.getEntityCode().trim());
        }
        guidToEntityCode = Collections.unmodifiableMap(m);
        log.info("Entra guid->entity map loaded entries={}", m.size());
    }

    /**
     * If {@code trimmedTokenGroupClaim} is a known Entra group object id, returns the mapped {@code entity_code};
     * otherwise returns the input unchanged.
     */
    public String resolveForPersistence(String trimmedTokenGroupClaim) {
        String objectIdKey = normalizeObjectIdKey(trimmedTokenGroupClaim);
        if (objectIdKey == null) {
            return trimmedTokenGroupClaim;
        }
        return guidToEntityCode.getOrDefault(objectIdKey, trimmedTokenGroupClaim);
    }

    /**
     * @return lower-case object id without braces, or {@code null} when the claim is not a lone Entra object id
     */
    static String normalizeObjectIdKey(String trimmedTokenGroupClaim) {
        if (trimmedTokenGroupClaim == null || trimmedTokenGroupClaim.isEmpty()) {
            return null;
        }
        Matcher mat = TOKEN_GROUP_AS_OBJECT_ID.matcher(trimmedTokenGroupClaim.trim());
        if (!mat.matches()) {
            return null;
        }
        return mat.group(1).toLowerCase(Locale.ROOT);
    }
}
