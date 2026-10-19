package com.customcharge.dashboard.importfile.config;

import com.customcharge.dashboard.constants.ImportConfigConstants;
import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.dashboard.importfile.exception.ImportInternalException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class ImportScreenCatalog implements InitializingBean {

    private Map<String, List<String>> screens = Map.of();

    public Optional<List<String>> requiredColumnsFor(String screenName) {
        if (screenName == null || screenName.isBlank()) {
            return Optional.empty();
        }
        String key = screenName.strip().toUpperCase(Locale.ROOT);
        List<String> cols = screens.get(key);
        if (cols == null || cols.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(cols));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Resource resource = new ClassPathResource(ImportConfigConstants.CLASSPATH_LOCATION);
        if (!resource.exists()) {
            throw new InternalServerException(
                    ValidationConstants.IMPORT_CONFIG_MISSING_PREFIX
                            + ImportConfigConstants.CLASSPATH_LOCATION
                            + ValidationConstants.IMPORT_CONFIG_MISSING_SUFFIX);
        }
        try (InputStream in = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new InternalServerException(
                        ValidationConstants.IMPORT_CONFIG_SCREENS_MAPPING_PREFIX
                                + ImportConfigConstants.CLASSPATH_LOCATION
                                + ValidationConstants.IMPORT_CONFIG_SCREENS_MAPPING_SUFFIX);
            }
            Object screensNode = root.get(ImportConfigConstants.SCREENS_KEY);
            if (!(screensNode instanceof Map<?, ?> screenMap)) {
                throw new InternalServerException(
                        ValidationConstants.IMPORT_CONFIG_SCREENS_MAPPING_PREFIX
                                + ImportConfigConstants.CLASSPATH_LOCATION
                                + ValidationConstants.IMPORT_CONFIG_SCREENS_OBJECT_SUFFIX);
            }
            Map<String, List<String>> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : screenMap.entrySet()) {
                String name = e.getKey().toString().strip().toUpperCase(Locale.ROOT);
                if (!(e.getValue() instanceof List<?> rawList)) {
                    throw new InternalServerException(
                            ValidationConstants.IMPORT_CONFIG_SCREEN_LIST_PREFIX
                                    + name
                                    + ValidationConstants.IMPORT_CONFIG_SCREEN_LIST_SUFFIX);
                }
                List<String> columns = new ArrayList<>();
                for (Object o : rawList) {
                    if (o == null) {
                        continue;
                    }
                    String col = o.toString().strip();
                    if (!col.isEmpty()) {
                        columns.add(col);
                    }
                }
                if (columns.isEmpty()) {
                    throw new InternalServerException(
                            ValidationConstants.IMPORT_CONFIG_SCREEN_LIST_PREFIX
                                    + name
                                    + ValidationConstants.IMPORT_CONFIG_SCREEN_COLUMNS_SUFFIX);
                }
                map.put(name, List.copyOf(columns));
            }
            if (map.isEmpty()) {
                throw new InternalServerException(ValidationConstants.IMPORT_CONFIG_SCREENS_EMPTY);
            }
            this.screens = Map.copyOf(map);
        } catch (InternalServerException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportInternalException(ValidationConstants.IMPORT_CONFIG_LOAD_FAILED, e);
        }
    }
}
