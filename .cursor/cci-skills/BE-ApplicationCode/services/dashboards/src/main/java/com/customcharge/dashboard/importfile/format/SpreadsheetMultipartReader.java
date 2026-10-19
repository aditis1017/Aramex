package com.customcharge.dashboard.importfile.format;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpreadsheetMultipartReader {

    boolean supports(MultipartFile file);

    SpreadsheetReadOutcome read(MultipartFile file, List<String> requiredColumns, boolean trimCellValues);
}
