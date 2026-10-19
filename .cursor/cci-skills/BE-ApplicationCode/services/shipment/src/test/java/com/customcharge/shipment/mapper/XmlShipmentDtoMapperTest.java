package com.customcharge.shipment.mapper;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.shipment.converter.ShipmentXmlConverter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XmlShipmentDtoMapperTest {

    private final ShipmentXmlConverter xmlConverter = new ShipmentXmlConverter();

    @Mock
    private EntityRepository entityRepository;

    private XmlShipmentDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new XmlShipmentDtoMapper(entityRepository);
    }

    @Test
    void map_shouldResolveNestedHawbUpdate_forInfoLinkDocumentSh013() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <InfoLinkDocument>
                  <AccessResponse><DocumentType>3</DocumentType></AccessResponse>
                  <HAWBUpdate>
                    <HAWBNumber>bbb989900ss92</HAWBNumber>
                    <PINumber>SH013</PINumber>
                    <Comment2>222 CAI CAI 488012 EXP</Comment2>
                    <HAWBDetails>
                      <HAWBNumber>49787255641</HAWBNumber>
                      <ProductType>DIM</ProductType>
                      <CustomsAmount>1000</CustomsAmount>
                      <CustomsCurrencyCode>SAR</CustomsCurrencyCode>
                      <AdditionalProperties>
                        <ShipmentProfile>
                          <BusinessType>E-Commerce</BusinessType>
                        </ShipmentProfile>
                      </AdditionalProperties>
                    </HAWBDetails>
                    <MAWBDetails>
                      <MAWBNumber>488012</MAWBNumber>
                      <MAWBID>32256686</MAWBID>
                    </MAWBDetails>
                    <Items>
                      <Item>
                        <CountryOfOrigin>CN</CountryOfOrigin>
                      </Item>
                    </Items>
                  </HAWBUpdate>
                </InfoLinkDocument>
                """;

        ShipmentDto dto = mapXml(xml);

        assertThat(dto.getPiNumber()).isEqualTo("SH013");
        assertThat(dto.getHawb()).isEqualTo("bbb989900ss92");
        assertThat(dto.getComment2()).isEqualTo("222 CAI CAI 488012 EXP");
        assertThat(dto.getProductType()).isEqualTo("DIM");
        assertThat(dto.getCostOfGoods()).isEqualTo(1000.0);
        assertThat(dto.getCustomsCurrencyCode()).isEqualTo("SAR");
        assertThat(dto.getBusinessType()).isEqualTo("E-Commerce");
        assertThat(dto.getCountryOfOrigin()).isEqualTo("CN");
        assertThat(dto.getMawbConsole()).isEqualTo("488012");
        assertThat(dto.getMawbId()).isEqualTo(32256686L);
    }

    @Test
    void map_shouldResolveDestinationEntityFromHawbUpdateLevel() throws Exception {
        String xml = """
                <HAWBUpdate>
                  <HAWBNumber>HAWB-100</HAWBNumber>
                  <HAWBDetails>
                    <HAWBNumber>HAWB-100</HAWBNumber>
                    <HAWBDestinationEntity>RUH</HAWBDestinationEntity>
                  </HAWBDetails>
                </HAWBUpdate>
                """;

        ShipmentDto dto = mapXml(xml);

        assertThat(dto.getDestinationEntity()).isEqualTo("RUH");
    }

    @Test
    void map_shouldUseRootAsHawbUpdate_forBareHawbUpdateSh267() throws Exception {
        String xml = """
                <HAWBUpdate>
                  <HAWBNumber>989ss900ssw92w</HAWBNumber>
                  <PINumber>SH267</PINumber>
                  <Comment1>14 JOD</Comment1>
                  <HAWBDetails>
                    <HAWBNumber>50784863953</HAWBNumber>
                    <ProductType>RTS</ProductType>
                    <CustomsAmount>15</CustomsAmount>
                    <CustomsCurrencyCode>TND</CustomsCurrencyCode>
                  </HAWBDetails>
                </HAWBUpdate>
                """;

        ShipmentDto dto = mapXml(xml);

        assertThat(dto.getPiNumber()).isEqualTo("SH267");
        assertThat(dto.getHawb()).isEqualTo("989ss900ssw92w");
        assertThat(dto.getComment1()).isEqualTo("14 JOD");
        assertThat(dto.getProductType()).isEqualTo("RTS");
        assertThat(dto.getCostOfGoods()).isEqualTo(15.0);
        assertThat(dto.getCustomsCurrencyCode()).isEqualTo("TND");
    }

    @Test
    void map_shouldOverrideOriginAndDestination_forSh022() throws Exception {
        Entity origin = new Entity();
        origin.setCode("DXB");
        origin.setInfoAxsId("39");
        Entity destination = new Entity();
        destination.setCode("DXB");

        when(entityRepository.findByInfoAxsId("39")).thenReturn(Optional.of(origin));
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(destination));

        String xml = """
                <HAWBUpdate>
                  <HAWBNumber>37348019520</HAWBNumber>
                  <HAWBOriginEntity>LON</HAWBOriginEntity>
                  <UpdateEntity>39</UpdateEntity>
                  <PINumber>SH022</PINumber>
                  <Comment1>DXB,United Arab Emirates,14276</Comment1>
                  <HAWBDetails>
                    <HAWBNumber>37348019520</HAWBNumber>
                    <HAWBOriginEntity>LON</HAWBOriginEntity>
                    <HAWBDestinationEntity>DXB</HAWBDestinationEntity>
                  </HAWBDetails>
                  <MAWBDetails>
                    <MAWBNumber>10062200AQZ</MAWBNumber>
                  </MAWBDetails>
                </HAWBUpdate>
                """;

        ShipmentDto dto = mapXml(xml);

        assertThat(dto.getPiNumber()).isEqualTo("SH022");
        assertThat(dto.getHawbOriginEntity()).isEqualTo("DXB");
        assertThat(dto.getShipperEntity()).isEqualTo("DXB");
        assertThat(dto.getDestinationEntity()).isEqualTo("DXB");
        verify(entityRepository).findByInfoAxsId(eq("39"));
        verify(entityRepository).findByCodeIgnoreCase("DXB");
    }

    @Test
    void map_shouldNotSetDestinationFromComment1_whenEntityCodeUnknown_forSh022() throws Exception {
        when(entityRepository.findByCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        String xml = """
                <HAWBUpdate>
                  <HAWBNumber>HAWB-1</HAWBNumber>
                  <PINumber>SH022</PINumber>
                  <Comment1>UNKNOWN,Country,123</Comment1>
                  <HAWBDetails>
                    <HAWBDestinationEntity>FALLBACK</HAWBDestinationEntity>
                  </HAWBDetails>
                </HAWBUpdate>
                """;

        ShipmentDto dto = mapXml(xml);

        assertThat(dto.getDestinationEntity()).isEqualTo("FALLBACK");
    }

    @Test
    void resolveHawbUpdateNode_shouldPreferNestedChildWhenPresent() throws Exception {
        JsonNode node = xmlConverter.fromMessage(message("""
                <Root><HAWBUpdate><PINumber>SH013</PINumber></HAWBUpdate></Root>
                """));

        JsonNode resolved = mapper.resolveHawbUpdateNode(node);

        assertThat(resolved.path("PINumber").asText()).isEqualTo("SH013");
    }

    @Test
    void resolveHawbUpdateNode_shouldUseRootWhenNestedChildMissing() throws Exception {
        JsonNode node = xmlConverter.fromMessage(message("""
                <HAWBUpdate><PINumber>SH267</PINumber></HAWBUpdate>
                """));

        JsonNode resolved = mapper.resolveHawbUpdateNode(node);

        assertThat(resolved.path("PINumber").asText()).isEqualTo("SH267");
    }

    private ShipmentDto mapXml(String xml) throws Exception {
        JsonNode node = xmlConverter.fromMessage(message(xml));
        return mapper.map(node);
    }

    private static Message message(String xml) {
        return new Message(xml.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
