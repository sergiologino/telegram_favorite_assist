package com.altacod.favorites.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DataSourceConfig {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    @Bean
    @Primary
    DataSource dataSource(DataSourceProperties properties) throws IOException {
        ensureSqliteDirectoryExists(properties.getUrl());
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    static void ensureSqliteDirectoryExists(String jdbcUrl) throws IOException {
        if (jdbcUrl == null || !jdbcUrl.startsWith(SQLITE_PREFIX)) {
            return;
        }

        String databasePath = jdbcUrl.substring(SQLITE_PREFIX.length()).trim();
        if (databasePath.isBlank() || isInMemoryDatabase(databasePath)) {
            return;
        }

        Path dbFile = Path.of(databasePath).toAbsolutePath().normalize();
        Path parent = dbFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static boolean isInMemoryDatabase(String databasePath) {
        return databasePath.startsWith("mem:")
                || databasePath.startsWith("file::memory:")
                || ":memory:".equals(databasePath);
    }
}
