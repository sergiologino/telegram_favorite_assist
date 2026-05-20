package com.altacod.favorites.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SyncControllerImportIntegrationTest {

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:mem:SyncControllerImportIntegrationTest");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void importsRealExportFileThroughApi() throws Exception {
        Path exportFile = Path.of("..", "Export", "result.json").toAbsolutePath().normalize();
        byte[] content = Files.readAllBytes(exportFile);

        mockMvc.perform(multipart("/api/import/export")
                        .file(new MockMultipartFile("file", "result.json", "application/json", content)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
