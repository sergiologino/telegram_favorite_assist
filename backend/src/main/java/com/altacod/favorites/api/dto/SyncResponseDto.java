package com.altacod.favorites.api.dto;

import com.altacod.favorites.processing.PostProcessingService;
import com.altacod.favorites.telegram.TelegramBotService;
import com.altacod.favorites.telegram.TelegramExportParser;
import com.altacod.favorites.telegram.TelegramUserSessionService;

public record SyncResponseDto(
        int userImported,
        int userSkipped,
        String userError,
        int botImported,
        int botSkipped,
        String botError,
        int exportImported,
        int exportSkippedDuplicate,
        int exportSkippedEmpty,
        int processed,
        int failed,
        int skipped
) {
    public static SyncResponseDto from(
            TelegramUserSessionService.UserSyncResult user,
            TelegramBotService.BotSyncResult bot,
            PostProcessingService.ProcessingSummary processing
    ) {
        return new SyncResponseDto(
                user.imported(),
                user.skipped(),
                user.error(),
                bot.imported(),
                bot.skipped(),
                bot.error(),
                0,
                0,
                0,
                processing.processed(),
                processing.failed(),
                processing.skipped()
        );
    }

    public static SyncResponseDto fromImport(TelegramExportParser.ImportResult importResult) {
        return new SyncResponseDto(
                0,
                0,
                null,
                0,
                0,
                null,
                importResult.imported(),
                importResult.skippedDuplicate(),
                importResult.skippedEmpty(),
                0,
                0,
                0
        );
    }
}
