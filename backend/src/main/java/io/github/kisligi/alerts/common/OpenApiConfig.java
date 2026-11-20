package io.github.kisligi.alerts.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown in Swagger UI ({@code /swagger-ui.html}). */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI worldEventAlertsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("World Event Alerts API")
                .version("0.1.0")
                .description("Users define alert rules and are notified by email or Slack when a "
                        + "matching world event happens. Demo prototype: no authentication.")
                .license(new License().name("MIT")));
    }
}
