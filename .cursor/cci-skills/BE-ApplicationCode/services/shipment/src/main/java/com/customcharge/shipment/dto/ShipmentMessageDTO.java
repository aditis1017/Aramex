package com.customcharge.shipment.dto;

import com.customcharge.persistencelayer.dto.AbstractDto;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class ShipmentMessageDTO extends AbstractDto {

    // -------- BASIC --------
    private String hawb;
    private String productType;
    private String productGroup;
    private String paymentType;
    private String cmt;
    private String alertedDate; // keep String, convert later
    private String attnOf;

    // -------- ACCOUNTS --------
    private String shipperAccountNumber;
    private String consigneeAccountNumber;
    private String thirdPartyAccountNumber;

    // -------- ENTITIES --------
    private String shipperEntity;
    private String destinationEntity;
    private String thirdThirdPartyEntity;

    // -------- DETAILS --------
    private Integer pieces;
    private String services;
    private String pickupDate;

    private Double hawbWeight;
    private Double chargeableWeight;
    private String hawbWeightUnit;

    private Double collectAmount;
    private String collectCurrencyCode;

    private Double customsAmount;
    private String customsCurrencyCode;

    private String commodityCountryCode;
    private String commodityDescription;

    // -------- SHIPPER --------
    private String shipperName;
    private String shipperCity;
    private String shipperCountryCode;

    // -------- CONSIGNEE --------
    private String consigneeName;
    private String consigneeCity;
    private String consigneeCountryCode;

    // -------- FINANCIAL --------
    private Double codValue;
    private String codCurrencyCode;
    private Double cashValue;
    private Double insuranceValue;

    // -------- MISC --------
    private String originLocationCode;
    private String destinationLocationCode;
    private String collectionReference;

    // -------- REQUIRED --------
    private String countryOfOrigin;
    private String hawbOriginEntity;

    private String piNumber;
    private Double costOfGoods;

    // -------- TO DRIVE --------
    private String shipmentType;
    private String categoryType;
    private String businessType;
}