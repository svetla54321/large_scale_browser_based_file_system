package com.svetlecic.filesystem.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("local")
class LocalFlywayConfig {

    @Bean
    FlywayMigrationStrategy rebuildDatabaseWhenMigrationsChange() {
        return flyway -> {
            var validation = flyway.validateWithResult();
            if (!validation.validationSuccessful) {
                log.warn("Migrations differ from the local database, rebuilding it from scratch: {}",
                        validation.getAllErrorMessages());
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
