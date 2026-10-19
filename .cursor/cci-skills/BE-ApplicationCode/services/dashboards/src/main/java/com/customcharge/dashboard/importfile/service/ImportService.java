package com.customcharge.dashboard.importfile.service;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.config.ImportScreenCatalog;
import com.customcharge.dashboard.importfile.dto.ImportResponse;
import com.customcharge.dashboard.importfile.processor.ImportContext;
import com.customcharge.dashboard.importfile.processor.ImportProcessor;
import com.customcharge.dashboard.importfile.processor.ImportProcessorRegistry;
import com.customcharge.dashboard.importfile.processor.ImportResult;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImportService {

    private final ImportScreenCatalog screenCatalog;
    private final ImportProcessorRegistry processorRegistry;

    public ImportService(ImportScreenCatalog screenCatalog, ImportProcessorRegistry processorRegistry) {
        this.screenCatalog = screenCatalog;
        this.processorRegistry = processorRegistry;
    }

    @Transactional(readOnly = false)
    public ImportResponse importFile(String screenName, MultipartFile file, Map<String, Object> params) {
        if (file == null) {
            throw new BadRequestException(ImportConstants.MESSAGE_FILE_REQUIRED);
        }
        if (file.isEmpty()) {
            throw new ValidationException(ImportConstants.MESSAGE_FILE_EMPTY);
        }
        String screen = screenName == null ? "" : screenName.strip();
        if (screen.isEmpty()) {
            throw new BadRequestException(ImportConstants.MESSAGE_SCREEN_NAME_REQUIRED);
        }
        String screenKey = screen.toUpperCase(Locale.ROOT).replace('-', '_');
        List<String> required = screenCatalog.requiredColumnsFor(screenKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ImportConstants.MESSAGE_IMPORT_SCREEN_NOT_CONFIGURED, screenName)));

        ImportProcessor processor = processorRegistry.require(screenKey);

        ImportContext ctx = new ImportContext(file, screenKey, required, params);

        ImportResult result = processor.process(ctx);
        return new ImportResponse(screenKey, result.rowsProcessed(), ImportConstants.MESSAGE_IMPORT_COMPLETED);
    }
}
