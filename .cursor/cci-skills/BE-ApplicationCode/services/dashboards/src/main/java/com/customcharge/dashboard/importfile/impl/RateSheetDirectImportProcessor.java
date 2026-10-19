package com.customcharge.dashboard.importfile.impl;

import com.customcharge.dashboard.constants.DashboardMessageConstants;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.dashboard.importfile.format.SpreadsheetMultipartReaderRegistry;
import com.customcharge.dashboard.importfile.format.SpreadsheetReadOutcome;
import com.customcharge.dashboard.importfile.support.ImportColumnUtils;
import com.customcharge.dashboard.importfile.processor.ImportContext;
import com.customcharge.dashboard.importfile.processor.ImportProcessor;
import com.customcharge.dashboard.importfile.processor.ImportResult;
import com.customcharge.dashboard.service.RateSheetItemDuplicateValidator;
import com.customcharge.dashboard.service.RateSheetItemFieldValidation;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class RateSheetDirectImportProcessor implements ImportProcessor {

    private final SpreadsheetMultipartReaderRegistry spreadsheetReaderRegistry;
    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;
    private final RateSheetItemFieldValidation fieldValidation;
    private final RateSheetItemDuplicateValidator duplicateValidator;
    private final AccountService accountService;

    public RateSheetDirectImportProcessor(
            SpreadsheetMultipartReaderRegistry spreadsheetReaderRegistry,
            RateSheetRepository rateSheetRepository,
            RateSheetItemRepository rateSheetItemRepository,
            RateSheetItemFieldValidation fieldValidation,
            RateSheetItemDuplicateValidator duplicateValidator,
            AccountService accountService) {
        this.spreadsheetReaderRegistry = spreadsheetReaderRegistry;
        this.rateSheetRepository = rateSheetRepository;
        this.rateSheetItemRepository = rateSheetItemRepository;
        this.fieldValidation = fieldValidation;
        this.duplicateValidator = duplicateValidator;
        this.accountService = accountService;
    }

    @Override
    public String screenName() {
        return ImportConstants.SCREEN_RATE_SHEET;
    }

    @Override
    public ImportResult process(ImportContext context) {
        MultipartFile file = context.file();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().strip();
        if (name.isEmpty()) {
            throw new ValidationException(ImportConstants.MESSAGE_FILENAME_EXTENSION_REQUIRED);
        }

        UUID rateSheetId = requireRateSheetId(context.params());
        RateSheet rateSheet = rateSheetRepository.findById(rateSheetId)
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.RATE_SHEET_NOT_FOUND));
        Account account = requireAccount(rateSheet);

        List<String> requiredColumns = context.requiredColumns();
        validateRateSheetImportConfig(requiredColumns);

        SpreadsheetReadOutcome outcome = spreadsheetReaderRegistry.read(file, requiredColumns, true);
        List<RateSheetSpreadsheetMapper.ParsedRateSheetImportItem> parsedItems =
                RateSheetSpreadsheetMapper.mapRows(outcome.rows(), requiredColumns);

        List<RateSheetSpreadsheetMapper.ParsedRateSheetImportItem> validatedItems =
                validateParsedItems(parsedItems, account);

        List<RateSheetItemDuplicateValidator.ItemKey> importKeys = validatedItems.stream()
                .map(item -> new RateSheetItemDuplicateValidator.ItemKey(
                        item.serviceCode(),
                        item.shipmentType(),
                        item.businessType(),
                        item.categoryType()))
                .toList();
        duplicateValidator.assertImportNotDuplicate(rateSheetId, importKeys);

        List<RateSheetItem> items = new ArrayList<>(validatedItems.size());
        for (RateSheetSpreadsheetMapper.ParsedRateSheetImportItem parsed : validatedItems) {
            RateSheetItem item = new RateSheetItem();
            item.setRateSheet(rateSheet);
            mapItemFields(item, parsed);
            for (RateSheetSpreadsheetMapper.ParsedRateEntry rateEntry : parsed.rates()) {
                item.getRates().add(buildRate(item, rateEntry));
            }
            items.add(item);
        }
        rateSheetItemRepository.saveAll(items);
        syncAccountRedisForAccount(account);

        return new ImportResult(outcome.dataRowCount());
    }

    private void syncAccountRedisForAccount(Account account) {
        if (account == null || account.getAccountNature() == null || !StringUtils.hasText(account.getEntityCode())) {
            return;
        }
        if (AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            accountService.syncAccountToRedis(null, AccountNature.ENTITY_SETTINGS, account.getEntityCode().trim(), null);
            return;
        }
        if (AccountNature.AR.equals(account.getAccountNature()) && StringUtils.hasText(account.getAccountNumber())) {
            accountService.syncAccountToRedis(
                    account.getAccountNumber().trim(), AccountNature.AR, account.getEntityCode().trim(), null);
        }
    }

    private List<RateSheetSpreadsheetMapper.ParsedRateSheetImportItem> validateParsedItems(
            List<RateSheetSpreadsheetMapper.ParsedRateSheetImportItem> parsedItems,
            Account account) {
        List<RateSheetSpreadsheetMapper.ParsedRateSheetImportItem> validated = new ArrayList<>(parsedItems.size());
        for (int i = 0; i < parsedItems.size(); i++) {
            RateSheetSpreadsheetMapper.ParsedRateSheetImportItem parsed = parsedItems.get(i);
            int rowNumber = i + ImportConstants.DATA_ROW_NUMBER_OFFSET;
            try {
                RateSheetItemFieldValidation.ValidatedFields fields = fieldValidation.normalizeFields(
                        account.getId().toString(),
                        account.getEntityCode(),
                        account.getChargeType(),
                        parsed.serviceCode(),
                        parsed.shipmentType(),
                        parsed.businessType(),
                        parsed.categoryType(),
                        rowNumber);
                validated.add(new RateSheetSpreadsheetMapper.ParsedRateSheetImportItem(
                        parsed.description(),
                        fields.serviceCode(),
                        fields.shipmentType(),
                        fields.businessType(),
                        fields.categoryType(),
                        parsed.rates()));
            } catch (BadRequestException ex) {
                throw new ValidationException(ex.getMessage());
            }
        }
        return validated;
    }

    private Account requireAccount(RateSheet rateSheet) {
        if (rateSheet.getAccount() == null) {
            throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NOT_FOUND_FOR_RATE_SHEET);
        }
        return rateSheet.getAccount();
    }

    private void validateRateSheetImportConfig(List<String> requiredColumns) {
        if (requiredColumns == null || requiredColumns.isEmpty()) {
            throw new BadRequestException(ImportConstants.MESSAGE_RATE_SHEET_CONFIG_NO_COLUMNS);
        }
        Set<String> normalized = new HashSet<>();
        for (String col : requiredColumns) {
            if (col != null && !col.isBlank()) {
                normalized.add(ImportColumnUtils.normalizeHeader(col));
            }
        }
        for (String must : ImportConstants.RATE_SHEET_MINIMUM_CONFIGURED_COLUMNS) {
            if (!normalized.contains(ImportColumnUtils.normalizeHeader(must))) {
                throw new BadRequestException(
                        String.format(ImportConstants.MESSAGE_RATE_SHEET_CONFIG_MISSING_COLUMN, must));
            }
        }
    }

    private UUID requireRateSheetId(Map<String, Object> params) {
        if (params == null || !params.containsKey(ImportConstants.PARAM_RATE_SHEET_ID)) {
            throw new BadRequestException(ImportConstants.MESSAGE_RATE_SHEET_ID_REQUIRED);
        }
        Object raw = params.get(ImportConstants.PARAM_RATE_SHEET_ID);
        if (raw == null) {
            throw new BadRequestException(ImportConstants.MESSAGE_RATE_SHEET_ID_REQUIRED);
        }
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        String text = raw.toString().strip();
        if (text.isEmpty()) {
            throw new BadRequestException(ImportConstants.MESSAGE_RATE_SHEET_ID_REQUIRED);
        }
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ImportConstants.MESSAGE_RATE_SHEET_ID_INVALID_UUID);
        }
    }

    private void mapItemFields(RateSheetItem item, RateSheetSpreadsheetMapper.ParsedRateSheetImportItem parsed) {
        item.setServiceCode(parsed.serviceCode());
        item.setDescription(parsed.description());
        item.setShipmentType(parsed.shipmentType());
        item.setBusinessType(parsed.businessType());
        item.setCategoryType(parsed.categoryType());
    }

    private RateSheetItemRate buildRate(RateSheetItem item, RateSheetSpreadsheetMapper.ParsedRateEntry entry) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateSheetItem(item);
        rate.setRateType(RateType.fromDisplayName(entry.rateType()));
        rate.setValue(entry.value());
        if (RateType.PERCENTAGE.getDisplayName().equalsIgnoreCase(entry.rateType())) {
            rate.setPercentageOf(PercentageOf.fromDisplayName(entry.percentageOf()));
        }
        return rate;
    }
}
