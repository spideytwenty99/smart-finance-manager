package com.saurabh.financemanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartFinanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Finance Manager API")
                        .description("Track income and expenses, set monthly budgets per category, "
                                + "and get a monthly report with budget status. "
                                + "Transactions without a category are categorized automatically from their description.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Saurabh")
                                .url("https://github.com/spideytwenty99/smart-finance-manager")));
    }
}