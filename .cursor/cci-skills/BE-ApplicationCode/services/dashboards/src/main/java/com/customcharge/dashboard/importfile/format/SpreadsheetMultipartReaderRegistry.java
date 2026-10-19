package com.customcharge.dashboard.importfile.format;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SpreadsheetMultipartReaderRegistry {

    private final List<SpreadsheetMultipartReader> readers;

    public SpreadsheetMultipartReaderRegistry(List<SpreadsheetMultipartReader> readers) {
        this.readers = List.copyOf(readers);
    }

    public SpreadsheetReadOutcome read(MultipartFile file, List<String> requiredColumns, boolean trimCellValues) {
        List<SpreadsheetMultipartReader> matching = readers.stream()
                .filter(r -> r.supports(file))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            throw new ValidationException(ImportConstants.MESSAGE_UNSUPPORTED_FILE_TYPE);
        }
        if (matching.size() > 1) {
            throw new InternalServerException(ImportConstants.MESSAGE_MULTIPLE_READERS_MATCHED);
        }
        return matching.get(0).read(file, requiredColumns, trimCellValues);
    }
}
