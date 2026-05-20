package com.altacod.favorites.scheduler;

import com.altacod.favorites.sync.SyncOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncOrchestrator syncOrchestrator;

    public SyncScheduler(SyncOrchestrator syncOrchestrator) {
        this.syncOrchestrator = syncOrchestrator;
    }

    @Scheduled(cron = "${app.sync.cron:0 0 8,20 * * *}", zone = "${app.timezone:Europe/Moscow}")
    public void scheduledSync() {
        log.info("Starting scheduled sync");
        syncOrchestrator.runFullSync();
    }
}
