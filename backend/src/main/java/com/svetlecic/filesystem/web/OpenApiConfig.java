package com.svetlecic.filesystem.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("File system API")
                .description("Browser-based file system: browse and create folders. Errors are returned as "
                        + "RFC 9457 problem details with a stable code and the request id."));
    }

    @Bean
    OpenApiCustomizer apiVersionParameter(@Value("${spring.mvc.apiversion.supported}") List<String> supported) {
        var versions = supported.stream().map(version -> "v" + version).toList();
        var parameter = new PathParameter()
                .name("version")
                .description("API version")
                .schema(new StringSchema()._enum(versions)._default(versions.getLast()));

        return openApi -> openApi.getPaths().entrySet().stream()
                .filter(path -> path.getKey().contains("{version}"))
                .forEach(path -> path.getValue().addParametersItem(parameter));
    }
}
