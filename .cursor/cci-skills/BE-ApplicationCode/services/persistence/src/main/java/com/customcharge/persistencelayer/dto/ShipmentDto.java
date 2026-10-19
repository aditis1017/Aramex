package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentDto extends AbstractDto {
    // -------- BASIC --------
    private String hawb;
    private Long hawbId;
    private String productType;
    private String productGroup;
    private String paymentType;
    private String hawbAlertedDate;
    private String attnOf;
    private Long mawbConsole;
    private Long mawbId;

    // -------- ACCOUNTS --------
    private String shipperAccountNumber;
    private String consigneeAccountNumber;
    private String thirdPartyAccountNumber;
    private String thirdPartyName;
    private String thirdPartyNumber;
    private String thirdPartyReference;
    private String thirdPartyReference2;
    private String thirdPartyState;
    private Long consigneeID;
    private Long shipperID;
    private Long thirdPartyID;
    private String ShipperStateCode;
    private String ShipperZipCode;
    private String shipperReference;
    private String shipperReference2;

    // -------- ENTITIES --------
    private String shipperEntity;
    private String destinationEntity;
    private String thirdPartyEntity;
    private String thirdPartyCity;
    private String thirdPartyCountry;

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
    private String consigneeStateCode;
    private String consigneeZipCode;

    // -------- FINANCIAL --------
    private Double codValue;
    private String codCurrencyCode;
    private Double cashValue;
    private Double insuranceValue;
    private String insuranceCurrency;
    private String cashCurrency;

    // -------- MISC --------
    private String originLocationCode;
    private String destinationLocationCode;
    private String collectionReference;
    private String foreignHAWBNumber;
    private String hawbDimensions;

    // -------- REQUIRED --------
    private String countryOfOrigin;
    private String hawbOriginEntity;

    private String piNumber;
    private String comment1;
    private String comment2;
    private Double costOfGoods;
    private Double cifValue;
    private String cifValueCurrency;
    private Boolean skeleton;
    private String consigneeReference;
    private String consigneeReference2;

    // -------- TO DRIVE --------
    private String shipmentType;
    private String categoryType;
    private String businessType;
    private String accountBusinessType;
    private Boolean isShipmentFull = false;
    private String frdmFlag;
}
