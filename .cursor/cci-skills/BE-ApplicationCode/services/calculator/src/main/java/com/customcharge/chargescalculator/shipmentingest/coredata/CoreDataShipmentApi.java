package com.customcharge.chargescalculator.shipmentingest.coredata;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CoreDataShipmentApi {

    private static final DateTimeFormatter US_DATE_TIME =
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US);

    private final CoreDataShipmentProperties properties;
    private final RestClient restClient;

    public CoreDataShipmentApi(CoreDataShipmentProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public Optional<ShipmentDto> fetchShipmentByAwb(String awb) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        String normalizedAwb = awb == null ? "" : awb.strip();
        if (normalizedAwb.isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonNode response = restClient.post()
                    .uri(properties.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("Ids", List.of(resolveAwbId(normalizedAwb))))
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response, normalizedAwb);
        } catch (RestClientResponseException ex) {
            throw new CoreDataShipmentException(
                    "Shipment details HTTP " + ex.getStatusCode().value() + ": " + ex.getStatusText(),
                    ex);
        } catch (RestClientException ex) {
            throw new CoreDataShipmentException("Failed to get Shipment details for awb : "+awb, ex);
        }
    }

    private static Object resolveAwbId(String awb) {
        try {
            return Long.parseLong(awb);
        } catch (NumberFormatException ex) {
            return awb;
        }
    }

    private Optional<ShipmentDto> parseResponse(JsonNode response, String awb) {
        if (response == null || response.isNull()) {
            throw new CoreDataShipmentException("Shipment details returned empty response for awb=" + awb);
        }
        if (!response.path("success").asBoolean(false)) {
            throw new CoreDataShipmentException("Shipment details unsuccessful for awb=" + awb);
        }

        JsonNode shipments = response.path("shipments");
        if (!shipments.isArray() || shipments.isEmpty()) {
            return Optional.empty();
        }

        ShipmentDto dto = mapShipment(shipments.get(0));
        if (dto.getHawb() == null || dto.getHawb().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(dto);
    }

    private static ShipmentDto mapShipment(JsonNode node) {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb(text(node, "AWB"));
        dto.setHawbId(longValue(node, "Id"));
        dto.setHawbOriginEntity(text(node, "Origin"));
        dto.setShipperEntity(text(node, "Origin"));
        dto.setDestinationEntity(text(node, "Destination"));
        dto.setProductGroup(text(node, "ProductGroup"));
        dto.setProductType(text(node, "Type"));
        dto.setPaymentType(firstText(node, "PaymentType", "CMT"));
        dto.setServices(text(node, "Services"));
        dto.setPieces(intValue(node, "PCS"));
        dto.setHawbWeight(doubleValue(node, "Weight"));
        dto.setChargeableWeight(doubleValue(node, "ChargingWeight"));
        dto.setHawbWeightUnit(text(node, "Unit"));
        dto.setCodValue(doubleValue(node, "CODValue"));
        dto.setCodCurrencyCode(text(node, "CODCurrency"));
        dto.setCollectAmount(doubleValue(node, "CollectAmount"));
        dto.setCollectCurrencyCode(text(node, "CollectCurrency"));
        dto.setCustomsAmount(doubleValue(node, "Customs"));
        dto.setCustomsCurrencyCode(text(node, "CustomsCurrency"));
        dto.setCostOfGoods(doubleValue(node, "Customs"));
        dto.setCashValue(doubleValue(node, "CashValue"));
        dto.setCashCurrency(text(node, "CashCurrency"));
        dto.setInsuranceValue(doubleValue(node, "InsuranceValue"));
        dto.setInsuranceCurrency(text(node, "InsuranceCurrency"));
        dto.setShipperAccountNumber(text(node, "ShipperNumber"));
        dto.setConsigneeAccountNumber(text(node, "ConsigneeNumber"));
        dto.setThirdPartyAccountNumber(text(node, "ThirdPartyNumber"));
        dto.setShipperCity(text(node, "OriginCity"));
        dto.setConsigneeCity(text(node, "DestCity"));
        dto.setShipperCountryCode(text(node, "OriginCountry"));
        dto.setConsigneeCountryCode(text(node, "DestCountry"));
        dto.setCountryOfOrigin(text(node, "OriginCountry"));
        dto.setOriginLocationCode(text(node, "OriginLocationCode"));
        dto.setDestinationLocationCode(text(node, "DestinationLocationCode"));
        dto.setShipperName(text(node, "ShipperName"));
        dto.setConsigneeName(text(node, "ConsigneeName"));
        dto.setShipperID(longValue(node, "ShipperId"));
        dto.setConsigneeID(longValue(node, "ConsigneeId"));
        dto.setThirdPartyID(longValue(node, "ThirdPartyId"));
        dto.setThirdPartyName(text(node, "ThirdPartyName"));
        dto.setThirdPartyCity(text(node, "ThirdPartyCity"));
        dto.setThirdPartyCountry(text(node, "ThirdPartyCountry"));
        dto.setThirdPartyState(text(node, "ThirdPartyState"));
        dto.setCommodityCountryCode(text(node, "CommodityCountryCode"));
        dto.setCommodityDescription(text(node, "CommodityDescription"));
        dto.setCollectionReference(text(node, "CollectionReference"));
        dto.setForeignHAWBNumber(text(node, "ForeignHAWBNumber"));
        dto.setAttnOf(text(node, "AttnOf"));
        dto.setShipperReference(text(node, "ShipperReference"));
        dto.setShipperReference2(text(node, "ShipperReference2"));
        dto.setConsigneeReference(text(node, "ConsigneeReference"));
        dto.setConsigneeReference2(text(node, "ConsigneeReference2"));
        dto.setThirdPartyReference(text(node, "ThirdPartyReference"));
        dto.setThirdPartyReference2(text(node, "ThirdPartyReference2"));
        dto.setConsigneeStateCode(text(node, "DestState"));
        dto.setConsigneeZipCode(text(node, "DestZipCode"));
        dto.setShipperStateCode(text(node, "OriginState"));
        dto.setShipperZipCode(text(node, "OriginZipCode"));
        dto.setSkeleton(booleanValue(node, "Skeleton"));
        toIsoDateTime(text(node, "PickupDate")).ifPresent(dto::setPickupDate);
        toIsoDateTime(text(node, "TS")).ifPresent(dto::setHawbAlertedDate);
        return dto;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private static Double doubleValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private static Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private static Boolean booleanValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private static Optional<String> toIsoDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.strip();
        try {
            return Optional.of(LocalDateTime.parse(trimmed).toString());
        } catch (DateTimeParseException ignored) {
            // try US format
        }
        try {
            return Optional.of(LocalDateTime.parse(trimmed, US_DATE_TIME).toString());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
}
