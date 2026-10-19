package com.customcharge.dashboard.importfile.impl;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.format.SpreadsheetMultipartReaderRegistry;
import com.customcharge.dashboard.importfile.format.SpreadsheetReadOutcome;
import com.customcharge.dashboard.importfile.processor.ImportContext;
import com.customcharge.dashboard.importfile.processor.ImportProcessor;
import com.customcharge.dashboard.importfile.processor.ImportResult;
import com.customcharge.dashboard.keywordrules.dto.ParsedKeywordRuleRow;
import com.customcharge.persistencelayer.dao.KeywordRuleDao;
import com.customcharge.persistencelayer.entities.KeywordRule;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Component
public class B2BConditionDirectImportProcessor implements ImportProcessor {

    private final SpreadsheetMultipartReaderRegistry spreadsheetReaderRegistry;
    private final KeywordRuleDao keywordRuleDao;

    public B2BConditionDirectImportProcessor(SpreadsheetMultipartReaderRegistry spreadsheetReaderRegistry, KeywordRuleDao keywordRuleDao) {
        this.spreadsheetReaderRegistry = spreadsheetReaderRegistry;
        this.keywordRuleDao = keywordRuleDao;
    }

    @Override
    public String screenName() {
        return ImportConstants.SCREEN_B2B_CONDITION;
    }

    @Override
    @CacheEvict(cacheNames = DashboardCacheNames.KEYWORD_RULES, allEntries = true)
    public ImportResult process(ImportContext context) {
        MultipartFile file = context.file();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().strip();
        if (name.isEmpty()) {
            throw new ValidationException(ImportConstants.MESSAGE_FILENAME_EXTENSION_REQUIRED);
        }
        SpreadsheetReadOutcome outcome = spreadsheetReaderRegistry.read(file, context.requiredColumns(), false);

        List<ParsedKeywordRuleRow> parsedRows = KeywordRuleSpreadsheetMapper.mapRows(outcome.rows());
        List<ParsedKeywordRuleRow> deduplicatedRows = deduplicate(parsedRows);

        List<KeywordRule> rulesToPersist = buildRules(deduplicatedRows);
        keywordRuleDao.deactivateAllActiveRules();
        keywordRuleDao.saveAll(rulesToPersist);

        return new ImportResult(outcome.dataRowCount());
    }

    private List<KeywordRule> buildRules(List<ParsedKeywordRuleRow> rows) {
        return rows.stream().map(row -> {
            KeywordRule keywordRule = new KeywordRule();
            keywordRule.setKeyword(row.keyword());
            keywordRule.setMatchType(row.matchType());
            keywordRule.setActive(row.isActive());
            return keywordRule;
        }).toList();
    }

    private List<ParsedKeywordRuleRow> deduplicate(List<ParsedKeywordRuleRow> parsedRows) {
        Map<String, ParsedKeywordRuleRow> uniqueRows = new LinkedHashMap<>();
        for (ParsedKeywordRuleRow row : parsedRows) {
            String dedupeKey = row.keyword() + ImportConstants.DEDUPE_KEY_SEPARATOR
                    + row.matchType().name().toUpperCase(Locale.ROOT);
            uniqueRows.putIfAbsent(dedupeKey, row);
        }
        return new ArrayList<>(uniqueRows.values());
    }
}
