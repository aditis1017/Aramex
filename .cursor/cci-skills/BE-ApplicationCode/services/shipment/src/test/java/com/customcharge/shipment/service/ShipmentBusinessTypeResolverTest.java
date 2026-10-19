package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_CORPORATE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_OIL_AND_GAS;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_RETAIL_CASH;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_WHOLESALE;
import static com.customcharge.shipment.constants.ShipmentConstants.BUSINESS_TYPE_E_COMMERCE;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2B;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2C;
import static org.assertj.core.api.Assertions.assertThat;

class ShipmentBusinessTypeResolverTest {

    private final ShipmentBusinessTypeResolver resolver = new ShipmentBusinessTypeResolver();

    @Test
    void resolve_shouldSetAccountBusinessTypeFromAr_andShipmentTypeFromKeyword() {
        Shipment shipment = new Shipment();
        shipment.setB2BShipment(false);
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessType(SHIPMENT_BUSINESS_TYPE_B2B);
        arAccount.setBusinessSegment(BUSINESS_TYPE_E_COMMERCE);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(shipment.getShipmentBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(shipment.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(message.getBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(message.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolve_shouldReturnB2b_whenKeywordFlagSetAndArAccountMissing() {
        Shipment shipment = new Shipment();
        shipment.setB2BShipment(true);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, null, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(shipment.getShipmentBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(shipment.getAccountBusinessType()).isNull();
        assertThat(message.getAccountBusinessType()).isNull();
    }

    @Test
    void resolve_shouldReturnB2b_whenKeywordFlagSetEvenIfArSegmentIsWholesale() {
        Shipment shipment = new Shipment();
        shipment.setB2BShipment(true);
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessSegment(AR_BUSINESS_TYPE_WHOLESALE);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(message.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            BUSINESS_TYPE_E_COMMERCE,
            AR_BUSINESS_TYPE_RETAIL_CASH
    })
    void resolveArBusinessType_shouldReturnB2c_forB2cSegments(String businessSegment) {
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessSegment(businessSegment);

        assertThat(resolver.resolveArBusinessType(arAccount)).contains(SHIPMENT_BUSINESS_TYPE_B2C);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            AR_BUSINESS_TYPE_CORPORATE,
            AR_BUSINESS_TYPE_WHOLESALE,
            AR_BUSINESS_TYPE_OIL_AND_GAS
    })
    void resolveArBusinessType_shouldReturnB2b_forB2bSegments(String businessSegment) {
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessSegment(businessSegment);

        assertThat(resolver.resolveArBusinessType(arAccount)).contains(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolve_shouldReturnB2c_whenKeywordNotSet_evenIfArSegmentIsWholesale() {
        Shipment shipment = new Shipment();
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessSegment(AR_BUSINESS_TYPE_WHOLESALE);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(message.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolve_shouldReturnB2c_whenKeywordNotSet_evenIfArSegmentIsCorporate() {
        Shipment shipment = new Shipment();
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessSegment(AR_BUSINESS_TYPE_CORPORATE);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(message.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolve_shouldReturnB2c_whenArBusinessTypeNullAndSegmentMissing() {
        Shipment shipment = new Shipment();
        ARAccount arAccount = new ARAccount();
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(message.getAccountBusinessType()).isNull();
    }

    @Test
    void resolve_shouldMapLegacyBusinessTypeAsAccountSegment_whenBusinessSegmentMissing() {
        Shipment shipment = new Shipment();
        ARAccount arAccount = new ARAccount();
        arAccount.setBusinessType(AR_BUSINESS_TYPE_CORPORATE);
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolve(shipment, arAccount, message)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
        assertThat(message.getAccountBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolve_shouldTolerateNullMessage() {
        Shipment shipment = new Shipment();
        shipment.setB2BShipment(true);

        assertThat(resolver.resolve(shipment, null, null)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(shipment.getShipmentBusinessType()).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
        assertThat(shipment.getAccountBusinessType()).isNull();
    }
}
