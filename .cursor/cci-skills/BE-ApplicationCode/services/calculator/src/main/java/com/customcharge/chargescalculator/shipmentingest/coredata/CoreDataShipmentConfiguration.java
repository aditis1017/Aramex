package com.customcharge.chargescalculator.shipmentingest.coredata;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(prefix = "custom-charge.shipment-details-api", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoreDataShipmentConfiguration {

    public static final String REST_CLIENT_BEAN = "coreDataShipmentRestClient";

    @Bean(REST_CLIENT_BEAN)
    RestClient coreDataShipmentRestClient(CoreDataShipmentProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Bean
    CoreDataShipmentApi coreDataShipmentApi(
            CoreDataShipmentProperties properties,
            @Qualifier(REST_CLIENT_BEAN) RestClient coreDataShipmentRestClient) {
        return new CoreDataShipmentApi(properties, coreDataShipmentRestClient);
    }
}
