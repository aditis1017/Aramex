//package com.customcharge.persistencelayer.config.aws;
//
//
//import org.springframework.boot.autoconfigure.condition.*;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.net.URI;
//
//@Configuration
//@ConditionalOnClass(name = "software.amazon.awssdk.services.sns.SnsAsyncClient")
//@EnableConfigurationProperties(SNSProperties.class)
//@ConditionalOnProperty(
//        prefix = "mvp.persistence.aws.sns",
//        name = "enabled",
//        havingValue = "true"
//)
//class SNSClientConfigurationImpl {
//
//    @Bean
//    @ConditionalOnMissingBean
//    public software.amazon.awssdk.services.sns.SnsAsyncClient snsAsyncClient(SNSProperties props) {
//
//        var builder = software.amazon.awssdk.services.sns.SnsAsyncClient.builder()
//                .region(software.amazon.awssdk.regions.Region.of(props.getRegion()));
//
//        // Optional Credentials
//        if (props.getAccessKey() != null && !props.getAccessKey().isBlank()) {
//            builder.credentialsProvider(
//                    software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
//                            software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
//                                    props.getAccessKey(),
//                                    props.getSecretKey()
//                            )
//                    )
//            );
//        }
//
//        // Optional Endpoint (for LocalStack etc.)
//        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
//            builder.endpointOverride(URI.create(props.getEndpoint()));
//        }
//
//        return builder.build();
//    }
//}