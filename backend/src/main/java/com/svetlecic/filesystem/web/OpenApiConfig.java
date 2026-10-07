package com.svetlecic.filesystem.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info().title("File system API"));
    }

    @Bean
    OpenApiCustomizer apiVersionParameter(@Value("${spring.mvc.apiversion.supported}") List<String> supported) {
        List<String> versions = supported.stream().map(v -> "v" + v).toList();
        var parameter = new PathParameter()
                .name("version")
                .schema(new StringSchema()._enum(versions)._default(versions.getLast()));

        return openApi -> openApi.getPaths().forEach((path, item) -> {
            if (path.contains("{version}")) {
                item.addParametersItem(parameter);
            }
        });
    }
}
