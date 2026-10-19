package com.customcharge.dashboard.rbac;

import com.customcharge.dashboard.dto.RolePermissionDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the {@code ccfe_route_access} JSON payload consumed by the Custom Charges Next.js proxy.
 * Payload is derived from granted permission codes only — not from any configured role code/name.
 */
public final class RouteAccessCookieFactory {

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "VIEW", "CREATE", "EDIT", "DELETE", "EXPORT");

    private RouteAccessCookieFactory() {}

    public static String buildCookieJson(List<RolePermissionDTO> permissions, ObjectMapper objectMapper) {
        Map<String, Object> payload = buildPayload(permissions);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize route access cookie payload", ex);
        }
    }

    static Map<String, Object> buildPayload(List<RolePermissionDTO> permissions) {
        Map<String, List<String>> moduleGrants = new LinkedHashMap<>();
        if (permissions != null) {
            for (RolePermissionDTO permission : permissions) {
                if (permission == null) {
                    continue;
                }
                String moduleKey = resolveModuleKey(permission);
                if (moduleKey.isBlank()) {
                    continue;
                }
                List<String> actions = extractActions(permission);
                moduleGrants.put(moduleKey, actions);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ready", true);
        payload.put("modules", new ArrayList<>(moduleGrants.keySet()));
        payload.put("moduleGrants", moduleGrants);
        payload.put("superAdmin", false);
        return payload;
    }

    private static String resolveModuleKey(RolePermissionDTO permission) {
        Set<String> codes = permission.getPermissionCodes();
        if (codes != null && !codes.isEmpty()) {
            for (String code : codes) {
                String prefix = extractPrefixFromFullPermissionCode(code);
                if (prefix != null && !prefix.isBlank()) {
                    return prefix;
                }
            }
        }
        String moduleName = permission.getModuleName();
        if (moduleName == null || moduleName.isBlank()) {
            return "";
        }
        return inferPermissionCodePrefixFromModuleName(moduleName);
    }

    private static List<String> extractActions(RolePermissionDTO permission) {
        Set<String> codes = permission.getPermissionCodes();
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        for (String code : codes) {
            String action = extractActionFromFullPermissionCode(code);
            if (action != null) {
                actions.add(action);
            }
        }
        return new ArrayList<>(actions);
    }

    static String extractPrefixFromFullPermissionCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);
        int last = upper.lastIndexOf('_');
        if (last <= 0 || last >= upper.length() - 1) {
            return null;
        }
        String suffix = upper.substring(last + 1);
        if (!ALLOWED_ACTIONS.contains(suffix)) {
            return null;
        }
        return upper.substring(0, last);
    }

    static String extractActionFromFullPermissionCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String upper = code.trim().toUpperCase(Locale.ROOT);
        int last = upper.lastIndexOf('_');
        if (last <= 0 || last >= upper.length() - 1) {
            return null;
        }
        String suffix = upper.substring(last + 1);
        return ALLOWED_ACTIONS.contains(suffix) ? suffix : null;
    }

    static String inferPermissionCodePrefixFromModuleName(String moduleName) {
        return moduleName.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("-+", "_");
    }
}
