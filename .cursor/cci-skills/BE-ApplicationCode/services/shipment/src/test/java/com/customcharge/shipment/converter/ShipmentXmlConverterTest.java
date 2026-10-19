package com.customcharge.shipment.converter;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentXmlConverterTest {

    private final ShipmentXmlConverter converter = new ShipmentXmlConverter();

    @Test
    void fromMessage_shouldParseValidXml() {
        String xml = """
                <ShipmentDto>
                  <hawb>HAWB-1</hawb>
                </ShipmentDto>
                """;
        Message message = new Message(xml.getBytes(StandardCharsets.UTF_8), new MessageProperties());

        JsonNode node = converter.fromMessage(message);

        assertThat(node.get("hawb").asText()).isEqualTo("HAWB-1");
    }

    @Test
    void fromMessage_shouldThrow_whenXmlInvalid() {
        Message message = new Message("<not-xml".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        assertThatThrownBy(() -> converter.fromMessage(message))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void toMessage_shouldNotBeSupported() {
        assertThatThrownBy(() -> converter.toMessage("x", new MessageProperties()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
