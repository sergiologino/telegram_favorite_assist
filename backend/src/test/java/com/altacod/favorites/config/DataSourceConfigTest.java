package com.altacod.favorites.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void createsMissingSqliteDirectory() throws Exception {
        Path dbFile = tempDir.resolve("nested").resolve("favorites.db");
        assertFalse(Files.exists(dbFile.getParent()));

        DataSourceConfig.ensureSqliteDirectoryExists("jdbc:sqlite:" + dbFile);

        assertTrue(Files.isDirectory(dbFile.getParent()));
    }

    @Test
    void ignoresNonSqliteUrls() throws Exception {
        assertDoesNotThrow(() -> DataSourceConfig.ensureSqliteDirectoryExists("jdbc:h2:mem:test"));
    }
}
