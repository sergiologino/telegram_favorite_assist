package com.altacod.favorites.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class TelegramExportImportIntegrationTest {

    @Autowired
    private TelegramExportParser exportParser;

    @Test
    void importsRealExportFile() throws Exception {
        Path exportFile = Path.of("..", "Export", "result.json").toAbsolutePath().normalize();
        var root = new ObjectMapper().readTree(exportFile.toFile());
        assertDoesNotThrow(() -> exportParser.importExport(root));
    }
}
