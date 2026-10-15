package com.saurabh.financemanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI smartFinanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Finance Manager API")
                        .description("Track income and expenses, set monthly budgets per category "
                                + "and get a monthly report with budget status. "
                                + "Transactions without a category are categorized automatically from their description. "
                                + "Register or log in to get a token, then click Authorize and paste it.")
                        .version("1.1.0")
                        .contact(new Contact()
                                .name("Saurabh")
                                .url("https://github.com/spideytwenty99/smart-finance-manager")))

                // How a token is sent: "Authorization: Bearer <token>"
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the token from register or login, without the word 'Bearer'.")))

                // Every endpoint needs the token by default
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))

                // Order of the groups in Swagger UI, following the real flow
                .tags(List.of(
                        new Tag().name("Authentication"),
                        new Tag().name("Categories"),
                        new Tag().name("Transactions"),
                        new Tag().name("Budgets"),
                        new Tag().name("Reports")));
    }

    @Bean
    public OpenApiCustomizer unauthorizedResponseCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/auth")) {
                return;   // register and login are open
            }
            pathItem.readOperations().forEach(operation ->
                    operation.getResponses().addApiResponse("401", new ApiResponse()
                            .description("Missing or invalid token")
                            .content(new Content().addMediaType("application/json",
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))))));
        });
    }
}