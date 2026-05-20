package com.altacod.favorites.telegram;

import com.altacod.favorites.config.AppProperties;
import com.altacod.favorites.domain.SyncState;
import com.altacod.favorites.domain.SyncStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TelegramUserSessionService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUserSessionService.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 120;

    private final AppProperties properties;
    private final SyncStateRepository syncStateRepository;
    private final TelegramUserSyncPayloadParser payloadParser;
    private final TelegramSavedMessagesImporter savedMessagesImporter;

    public TelegramUserSessionService(
            AppProperties properties,
            SyncStateRepository syncStateRepository,
            TelegramUserSyncPayloadParser payloadParser,
            TelegramSavedMessagesImporter savedMessagesImporter
    ) {
        this.properties = properties;
        this.syncStateRepository = syncStateRepository;
        this.payloadParser = payloadParser;
        this.savedMessagesImporter = savedMessagesImporter;
    }

    public boolean isConfigured() {
        AppProperties.UserApi userApi = properties.telegram().userApi();
        if (!userApi.enabled()) {
            return false;
        }
        if (isBlank(userApi.apiId()) || isBlank(userApi.apiHash())) {
            return false;
        }
        return Files.exists(resolveSessionFile(userApi.sessionPath()));
    }

    public UserSyncResult syncSavedMessages() {
        if (!isConfigured()) {
            return UserSyncResult.skipped("Telegram User API is not configured or session file is missing");
        }

        AppProperties.UserApi userApi = properties.telegram().userApi();
        Path projectRoot = resolveProjectRoot();
        Path scriptPath = projectRoot.resolve(userApi.syncScript()).normalize();
        if (!Files.exists(scriptPath)) {
            return UserSyncResult.failed("Sync script not found: " + scriptPath);
        }

        long sinceId = syncStateRepository.findById(1L)
                .map(SyncState::getLastUserMessageId)
                .orElse(0L);

        try {
            List<String> command = new ArrayList<>();
            command.add(userApi.pythonExecutable());
            command.add(scriptPath.toString());
            command.add("--since-id");
            command.add(String.valueOf(sinceId));
            command.add("--limit");
            command.add(String.valueOf(userApi.messageLimit()));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(projectRoot.toFile());
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return UserSyncResult.failed("Telegram User API sync timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String error = stderr.isBlank() ? "Telegram User API sync failed with exit code " + exitCode : stderr.trim();
                return UserSyncResult.failed(error);
            }

            TelegramUserSyncPayload payload = payloadParser.parse(stdout);
            TelegramSavedMessagesImporter.ImportResult importResult = savedMessagesImporter.importPayload(payload);
            return UserSyncResult.success(importResult.imported(), importResult.skipped(), null);
        } catch (Exception ex) {
            log.error("Telegram User API sync failed: {}", ex.getMessage());
            return UserSyncResult.failed(ex.getMessage());
        }
    }

    Path resolveProjectRoot() {
        return Path.of(properties.projectRoot()).toAbsolutePath().normalize();
    }

    Path resolveSessionFile(String configuredPath) {
        Path path = Path.of(configuredPath);
        if (!path.isAbsolute()) {
            path = resolveProjectRoot().resolve(path).normalize();
        }
        if (path.toString().endsWith(".session")) {
            return path;
        }
        return Path.of(path + ".session");
    }

    private String readStream(java.io.InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record UserSyncResult(
            int imported,
            int skipped,
            String error,
            boolean configured
    ) {
        public static UserSyncResult success(int imported, int skipped, String error) {
            return new UserSyncResult(imported, skipped, error, true);
        }

        public static UserSyncResult failed(String error) {
            return new UserSyncResult(0, 0, error, true);
        }

        public static UserSyncResult skipped(String error) {
            return new UserSyncResult(0, 0, error, false);
        }

        public boolean success() {
            return error == null;
        }
    }
}
