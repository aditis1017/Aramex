package com.customcharge.persistencelayer.security.entra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Maps Entra {@code groups} claim values to the {@code users.groups} column (JSON array of strings).
 */
public final class EntraTokenGroupsFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EntraTokenGroupsFormatter() {
    }

    /**
     * @return JSON array string, or {@code null} when there are no non-blank group entries
     */
    public static String toUserGroupsColumn(List<String> tokenGroups) {
        return toUserGroupsColumn(tokenGroups, UnaryOperator.identity());
    }

    /**
     * @param groupClaimToStored maps each trimmed {@code groups} claim fragment to the value stored in the database
     * @return JSON array string, or {@code null} when there are no non-blank group entries after resolution
     */
    public static String toUserGroupsColumn(List<String> tokenGroups, UnaryOperator<String> groupClaimToStored) {
        Objects.requireNonNull(groupClaimToStored, "groupClaimToStored");
        if (tokenGroups == null || tokenGroups.isEmpty()) {
            return null;
        }
        List<String> cleaned =
                tokenGroups.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(groupClaimToStored::apply)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(cleaned);
        } catch (JsonProcessingException e) {
            return String.join(",", cleaned);
        }
    }
}
