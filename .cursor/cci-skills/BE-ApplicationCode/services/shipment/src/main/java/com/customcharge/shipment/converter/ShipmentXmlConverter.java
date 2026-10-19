package com.customcharge.shipment.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static com.customcharge.shipment.constants.LogMessages.XML_CONVERSION_FAILED;
import static com.customcharge.shipment.constants.LogMessages.XML_RECEIVED_FROM_QUEUE;

import com.customcharge.shipment.constants.QueueMessages;

@Slf4j
@Component("shipmentXmlConverter")
public class ShipmentXmlConverter implements MessageConverter {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public JsonNode fromMessage(Message message) throws MessageConversionException {
        try {
            String xml = extractXml(message);
            return xmlMapper.readTree(xml);
        } catch (Exception ex) {
            log.error(XML_CONVERSION_FAILED, ex);
            throw new MessageConversionException(QueueMessages.XML_CONVERSION_FAILED, ex);
        }
    }

    public JsonNode fromXmlString(String xml) throws MessageConversionException {
        try {
            return xmlMapper.readTree(xml);
        } catch (Exception ex) {
            log.error(XML_CONVERSION_FAILED, ex);
            throw new MessageConversionException(QueueMessages.XML_CONVERSION_FAILED, ex);
        }
    }

    public static String extractXml(Message message) {
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    @Override
    public Message toMessage(Object object, org.springframework.amqp.core.MessageProperties messageProperties) {
        throw new UnsupportedOperationException();
    }
}
