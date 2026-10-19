package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shipment_hawb_number",
                columnNames = "hawb_number"
        )
)
@Getter
@Setter
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_SHIPMENT_CHARGE,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.HAWB_NUMBER, path = "hawbNumber")
        })
public class Shipment extends AbstractEntity {

        // AttentionOf
        @Column(name = "attention_of", length = 200)
        private String attentionOf;

        // HAWBNumber
        @Column(name = "hawb_number", length = 100)
        private String hawbNumber;

        // CashCurrencyCode
        @Column(name = "cash_currency", length = 100)
        private String cashCurrency;

        // CashValue
        @Column(name = "cash_value")
        private Double cashValue;

        // ChargeableWeight
        @Column(name = "chargeable_weight")
        private Double ChargeableWeight;

        // PaymentType (CMT)
        @Column(name = "payment_type", length = 200)
        private String paymentType;

        // CODCurrencyCode
        @Column(name = "cod_currency", length = 200)
        private String codCurrency;

        // CODValue
        @Column(name = "cod_value")
        private Double codValue;

        // CollectAmount
        @Column(name = "collect_amount")
        private Double collectAmount;

        // CollectCurrencyCode
        @Column(name = "collect_currency", length = 200)
        private String collectCurrency;

        // CollectionReference
        @Column(name = "collection_reference", length = 100)
        private String collectionReference;

        // CommodityCountryCode
        @Column(name = "commodity_country_code", length = 100)
        private String commodityCountryCode;

        // CommodityDescription
        @Column(name = "commodity_description", columnDefinition = "TEXT")
        private String commodityDescription;

        // ConsigneeID
        @Column(name = "consignee_id")
        private Long consigneeID;

        // ConsigneeName
        @Column(name = "consignee_name", length = 200)
        private String consigneeName;

        // ConsigneeNumber
        @Column(name = "consignee_number", length = 200)
        private String consigneeNumber;

        // ConsigneeReference
        @Column(name = "consignee_reference")
        private String consigneeReference;

        // ConsigneeReference2
        @Column(name = "consignee_reference2")
        private String consigneeReference2;

        // CustomsCurrencyCode
        @Column(name = "customs_currency", length = 100)
        private String customsCurrency;

        // ConsigneeCity (Destination City)
        @Column(name = "consignee_city", length = 100)
        private String consigneeCity;

        // ConsigneeCountryCode (Destination Country)
        @Column(name = "consignee_country_code", length = 100)
        private String consigneeCountryCode;

        // HAWBDestinationEntity (Destination Entity)
        @Column(name = "hawb_destination_entity", length = 100)
        private String HAWBDestinationEntity;

        // DestinationLocationCode
        @Column(name = "destination_location_code", length = 100)
        private String destinationLocationCode;

        // ConsigneeStateCode (Destination State)
        @Column(name = "consignee_state_code", length = 100)
        private String ConsigneeStateCode;

        // ConsigneeZipCode (Destination Zip)
        @Column(name = "consignee_zip_code", length = 100)
        private String ConsigneeZipCode;

        // ForeignHAWBNumber
        @Column(name = "foreign_hawb_number")
        private String foreignHAWBNumber;

        // HAWBDimensions
        @Column(name = "hawb_dimensions", columnDefinition = "TEXT")
        private String hawbDimensions;

        // HAWBID (idRef)
        @Column(name = "hawbid")
        private Long HAWBID;

        // InsuranceCurrencyCode
        @Column(name = "insurance_currency", length = 200)
        private String insuranceCurrency;

        // InsuranceValue
        @Column(name = "insurance_value")
        private Double insuranceValue;

        // HAWBOriginEntity
        @Column(name = "origin_entity", length = 200)
        private String originEntity;

        // ShipperCity (Origin City)
        @Column(name = "shipper_city", length = 200)
        private String ShipperCity;

        // ShipperCountryCode (Origin Country)
        @Column(name = "shipper_country_code", length = 100)
        private String ShipperCountryCode;

        // OriginLocationCode
        @Column(name = "origin_location_code", length = 100)
        private String OriginLocationCode;

        // ShipperStateCode (Origin State)
        @Column(name = "shipper_state_code", length = 100)
        private String ShipperStateCode;

        // ShipperZipCode (Origin Zip Code)
        @Column(name = "shipper_zip_code", length = 200)
        private String ShipperZipCode;

        // Pieces
        @Column(name = "pieces")
        private Integer pieces;

        // PickupDate
        @Column(name = "pickup_date")
        private LocalDateTime pickupDate;

        // HAWBProductGroup
        @Column(name = "product_group", length = 20)
        private String productGroup;

        // Services
        @Column(name = "services")
        private String services;

        // ShipperID
        @Column(name = "shipper_id")
        private Long shipperID;

        // ShipperName
        @Column(name = "shipper_name", length = 200)
        private String shipperName;

        // ShipperNumber
        @Column(name = "shipper_number", length = 200)
        private String shipperNumber;

        // ShipperReference
        @Column(name = "shipper_reference")
        private String shipperReference;

        // ShipperReference2
        @Column(name = "shipper_reference2")
        private String shipperReference2;

        // Skeleton
        @Column(name = "skeleton")
        private Boolean skeleton;

        // ThirdParty Entity
        @Column(name = "third_party_entity", length = 200)
        private String thirdPartyEntity;

        // ThirdPartyCity
        @Column(name = "third_party_city", length = 200)
        private String thirdPartyCity;

        // ThirdPartyCountryCode
        @Column(name = "third_party_country", length = 100)
        private String thirdPartyCountry;

        // ThirdPartyID
        @Column(name = "third_party_id")
        private Long thirdPartyID;

        // ThirdPartyName
        @Column(name = "third_party_name", length = 200)
        private String thirdPartyName;

        // ThirdPartyNumber
        @Column(name = "third_party_number", length = 200)
        private String thirdPartyNumber;

        // ThirdPartyReference
        @Column(name = "third_party_reference")
        private String thirdPartyReference;

        // ThirdPartyReference2
        @Column(name = "third_party_reference2")
        private String thirdPartyReference2;

        // ThirdPartyStateCode
        @Column(name = "third_party_state", length = 20)
        private String thirdPartyState;

        // HAWBAlertedDate (TS)
        @Column(name = "hawb_alerted_date")
        private LocalDateTime HAWBAlertedDate;

        // Product Type
        @Column(name = "product_type", length = 50)
        private String productType;

        // HAWBWeightUnit
        @Column(name = "unit", length = 20)
        private String unit;

        // HAWBWeight
        @Column(name = "weight")
        private Double weight;

        // CustomsAmount
        @Column(name = "cost_of_goods")
        private Double costOfGoods;

        // CIF Value
        @Column(name = "cif_value")
        private Double cifValue;

        // CIF Value Currency
        @Column(name = "cif_value_currency", length = 50)
        private String cifValueCurrency;

        // BusinessType
        @Column(name = "business_type", length = 50)
        private String businessType;

        // PI Number
        @Column(name = "pi_number", length = 50)
        private String piNumber;

        // To Drive
        @Column(name = "category_type", length = 50)
        private String categoryType;

        // To Drive
        @Column(name = "shipment_type", length = 50)
        private String shipmentType;

        // MAWBID
        @Column(name = "mawb_id")
        private Long mawbId;

        // MAWBNumber
        @Column(name = "mawb_console", length = 100)
        private String MAWBConsole;

        // B2B Shipment
        @Column(name = "b2b_shipment")
        private Boolean b2BShipment;

        @Column(name = "shipment_business_type", length = 10)
        private String shipmentBusinessType;

        @Column(name = "account_business_type", length = 10)
        private String accountBusinessType;

        // Actual Upload Count
        @Column(name = "actual_upload_count")
        private Integer actualUploadCount;

        // Manual Shipment Flag
        @Column(name = "is_manual_shipment")
        private Boolean isManualShipment = false;
}