package com.customcharge.chargescalculator.support;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.entities.StagedFileUpload;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CalculatorTestDataFactory {

    private CalculatorTestDataFactory() {
    }

    public static UUID fileId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    public static Shipment shipment() {
        return shipment(BatchProcessingConstants.SHIPMENT_TYPE_DDP, BatchProcessingConstants.PAYMENT_TYPE_PREPAID);
    }

    public static Shipment shipment(String shipmentType, String paymentType) {
        Shipment shipment = new Shipment();
        ReflectionTestUtils.setField(shipment, "id", UUID.randomUUID());
        shipment.setHawbNumber("AWB-1001");
        shipment.setShipmentType(shipmentType);
        shipment.setPaymentType(paymentType);
        shipment.setHAWBDestinationEntity("DXB");
        shipment.setOriginEntity("RUH");
        shipment.setThirdPartyEntity("JED");
        shipment.setConsigneeNumber("ACC-001");
        shipment.setActualUploadCount(0);
        return shipment;
    }

    public static ShipmentDto shipmentDto() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("AWB-1001");
        dto.setPaymentType("P");
        dto.setProductGroup("DOM");
        dto.setConsigneeCountryCode("SA");
        dto.setShipperAccountNumber("12345");
        dto.setDestinationEntity("DXB");
        dto.setHawbOriginEntity("RUH");
        dto.setThirdPartyEntity("JED");
        dto.setConsigneeAccountNumber("ACC-001");
        return dto;
    }

    public static ProcessedChunkMessage chunkMessage() {
        return chunkMessage(BatchProcessingConstants.PROCESS_FOR_ESTIMATE, List.of(recordMap()));
    }

    public static ProcessedChunkMessage chunkMessage(String processFor, List<Map<String, String>> records) {
        ProcessedChunkMessage message = new ProcessedChunkMessage();
        message.setFileId(fileId().toString());
        message.setFileName("upload.xlsx");
        message.setFileType("xlsx");
        message.setChunkIndex(0);
        message.setRecordCount(records.size());
        message.setProcessFor(processFor);
        message.setTotalChunks(1);
        message.setRecords(records);
        message.setTraceId("trace-1");
        message.setEntityCode("DXB");
        return message;
    }

    public static Map<String, String> recordMap() {
        Map<String, String> record = new HashMap<>();
        record.put("awb", "AWB-1001");
        record.put("customs", "10.50");
        return record;
    }

    public static Map<String, String> actualRecordMap() {
        Map<String, String> record = recordMap();
        record.put(BatchProcessingConstants.PAYMENT_REFERENCE, "SAD-001");
        return record;
    }

    public static ChargeCalculationContext chargeContext() {
        return chargeContext(shipment());
    }

    public static ChargeCalculationContext chargeContext(String processFor, Map<String, String> source) {
        return chargeContext(shipment(), processFor, source);
    }

    public static ChargeCalculationContext chargeContext(Shipment shipment) {
        return chargeContext(shipment, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, recordMap());
    }

    public static ChargeCalculationContext chargeContext(Shipment shipment, String processFor, Map<String, String> source) {
        return new ChargeCalculationContext(
                shipment,
                source,
                fileId().toString(),
                processFor,
                0);
    }

    public static Account account(UUID id, String chargeType) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType(chargeType);
        return account;
    }

    public static com.customcharge.persistencelayer.entities.Entity billingEntity(String code) {
        com.customcharge.persistencelayer.entities.Entity entity =
                new com.customcharge.persistencelayer.entities.Entity();
        ReflectionTestUtils.setField(entity, "id", 1L);
        entity.setCode(code);
        return entity;
    }

    public static ServiceChargeResponse serviceCharge(
            String serviceCode,
            String description,
            String type,
            BigDecimal taxValue,
            Boolean isBillable) {
        return new ServiceChargeResponse(
                UUID.randomUUID(),
                serviceCode,
                description,
                description,
                type,
                true,
                taxValue == null ? null : taxValue.doubleValue(),
                null,
                true,
                null,
                isBillable,
                null,
                null,
                null,
                null,
                null);
    }

    public static Charge charge(UUID id, Shipment shipment, String chargeCode) {
        Charge charge = new Charge();
        ReflectionTestUtils.setField(charge, "id", id);
        charge.setShipment(shipment);
        charge.setChargeCode(chargeCode);
        charge.setEstimateAmount(BigDecimal.ZERO);
        return charge;
    }

    public static ChargeLineItem chargeLineItem(Charge charge, Account account, String serviceCode, BigDecimal amount) {
        ChargeLineItem line = new ChargeLineItem();
        ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
        line.setCharge(charge);
        line.setAccount(account);
        line.setServiceCode(serviceCode);
        line.setAmount(amount);
        line.setFileReferenceId(fileId());
        return line;
    }

    public static CustomChargeFileError fileError(List<Map<String, String>> errorList) {
        CustomChargeFileError error = new CustomChargeFileError();
        ReflectionTestUtils.setField(error, "id", UUID.randomUUID());
        error.setFileId(fileId());
        error.setFileName("upload.xlsx");
        error.setFileType("xlsx");
        error.setChunkNumber(0);
        error.setErrorList(errorList);
        return error;
    }

    public static StagedFileUpload stagedFileUpload(String createdBy) {
        StagedFileUpload upload = new StagedFileUpload();
        ReflectionTestUtils.setField(upload, "id", fileId());
        upload.setFileId(fileId().toString());
        ReflectionTestUtils.setField(upload, "createdBy", createdBy);
        return upload;
    }

    public static MasterCategoryValueRow masterCategoryRow(String code, String name, String label) {
        return new MasterCategoryValueRow(1L, 1L, code, name, label, null, null, true, null);
    }

    public static User user(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        return user;
    }
}
