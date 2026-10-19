package com.customcharge.dashboard.importfile.processor;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ImportProcessorRegistry {

    private final Map<String, ImportProcessor> byScreenUpper;

    public ImportProcessorRegistry(List<ImportProcessor> processors) {
        this.byScreenUpper = processors.stream()
                .collect(Collectors.toMap(
                        p -> p.screenName().strip().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (a, b) -> {
                            throw new InternalServerException(
                                    String.format(ImportConstants.MESSAGE_MULTIPLE_PROCESSORS_REGISTERED, a.screenName()));
                        }));
    }

    public Optional<ImportProcessor> find(String screenName) {
        if (screenName == null || screenName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byScreenUpper.get(screenName.strip().toUpperCase(Locale.ROOT)));
    }

    public ImportProcessor require(String screenName) {
        return find(screenName).orElseThrow(
                () -> new ResourceNotFoundException(
                        String.format(ImportConstants.MESSAGE_NO_IMPORT_HANDLER, screenName)));
    }
}
