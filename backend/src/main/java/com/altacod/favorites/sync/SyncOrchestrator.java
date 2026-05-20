package com.altacod.favorites.sync;

import com.altacod.favorites.processing.PostProcessingService;
import com.altacod.favorites.telegram.TelegramBotService;
import com.altacod.favorites.telegram.TelegramUserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SyncOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SyncOrchestrator.class);

    private final TelegramUserSessionService telegramUserSessionService;
    private final TelegramBotService telegramBotService;
    private final PostProcessingService postProcessingService;

    public SyncOrchestrator(
            TelegramUserSessionService telegramUserSessionService,
            TelegramBotService telegramBotService,
            PostProcessingService postProcessingService
    ) {
        this.telegramUserSessionService = telegramUserSessionService;
        this.telegramBotService = telegramBotService;
        this.postProcessingService = postProcessingService;
    }

    public SyncResult runFullSync() {
        TelegramUserSessionService.UserSyncResult userResult = telegramUserSessionService.syncSavedMessages();
        TelegramBotService.BotSyncResult botResult = telegramBotService.syncUpdates();
        PostProcessingService.ProcessingSummary processingSummary = postProcessingService.processPendingPosts();

        log.info(
                "Full sync completed: userImported={}, botImported={}, processed={}, failed={}, skipped={}",
                userResult.imported(),
                botResult.imported(),
                processingSummary.processed(),
                processingSummary.failed(),
                processingSummary.skipped()
        );

        return new SyncResult(userResult, botResult, processingSummary);
    }

    public record SyncResult(
            TelegramUserSessionService.UserSyncResult userResult,
            TelegramBotService.BotSyncResult botResult,
            PostProcessingService.ProcessingSummary processingSummary
    ) {
    }
}
