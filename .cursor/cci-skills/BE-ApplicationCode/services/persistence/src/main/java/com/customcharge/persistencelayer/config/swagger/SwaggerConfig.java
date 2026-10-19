package com.customcharge.persistencelayer.config.swagger;

import com.customcharge.persistencelayer.security.entra.SwaggerBearerSecurity;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
@ConditionalOnProperty(
        prefix = "mvp.persistence.swagger",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(SwaggerProperties props) {

        Info info = new Info()
                .title(props.getTitle())
                .description(props.getDescription())
                .version(props.getVersion());

        if(props.getContactName()!=null){
            Contact contact = new Contact()
                    .name(props.getContactName())
                    .email(props.getContactEmail())
                    .url(props.getContactUrl());
            info.contact(contact);
        }

        SecurityScheme bearerJwt = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "Microsoft Entra access token. In Swagger UI click **Authorize**, choose this scheme, "
                                + "and enter **only the JWT** (Swagger adds the `Bearer ` prefix).");

        SecurityScheme authorizationHeader = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(HttpHeaders.AUTHORIZATION)
                .description(
                        "Alternative: paste the full header value `Bearer <your_access_token>` "
                                + "(useful if your client does not support the HTTP bearer scheme above).");

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes(SwaggerBearerSecurity.BEARER_JWT_SCHEME, bearerJwt)
                        .addSecuritySchemes(
                                SwaggerBearerSecurity.AUTHORIZATION_HEADER_API_KEY_SCHEME, authorizationHeader))
                .security(List.of(
                        new SecurityRequirement().addList(SwaggerBearerSecurity.BEARER_JWT_SCHEME),
                        new SecurityRequirement()
                                .addList(SwaggerBearerSecurity.AUTHORIZATION_HEADER_API_KEY_SCHEME)));

        if (props.getServerUrl() != null
                && !props.getServerUrl().isBlank()) {

            openAPI.setServers(List.of(
                    new Server().url(props.getServerUrl())
            ));
        }

        return openAPI;
    }
}
