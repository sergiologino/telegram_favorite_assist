package com.altacod.favorites.telegram;

import com.altacod.favorites.domain.PostSource;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.SyncState;
import com.altacod.favorites.domain.SyncStateRepository;
import com.altacod.favorites.domain.TelegramPost;
import com.altacod.favorites.domain.TelegramPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramSavedMessagesImporter {

    private static final Logger log = LoggerFactory.getLogger(TelegramSavedMessagesImporter.class);

    private final TelegramPostRepository postRepository;
    private final SyncStateRepository syncStateRepository;

    public TelegramSavedMessagesImporter(
            TelegramPostRepository postRepository,
            SyncStateRepository syncStateRepository
    ) {
        this.postRepository = postRepository;
        this.syncStateRepository = syncStateRepository;
    }

    @Transactional
    public ImportResult importPayload(TelegramUserSyncPayload payload) {
        int imported = 0;
        int skipped = 0;
        long maxSeenId = payload.maxId();

        for (TelegramUserSyncPayload.Message message : payload.messages()) {
            if (message.id() > maxSeenId) {
                maxSeenId = message.id();
            }
            if (postRepository.findByTelegramMessageId(message.id()).isPresent()) {
                skipped++;
                continue;
            }

            TelegramPost post = new TelegramPost();
            post.setTelegramMessageId(message.id());
            post.setTextContent(message.text());
            post.setPostedAt(message.date());
            post.setSource(PostSource.USER_SESSION);
            post.setStatus(PostStatus.PENDING);
            postRepository.save(post);
            imported++;
        }

        if (maxSeenId > 0) {
            SyncState state = syncStateRepository.findById(1L).orElseGet(this::createState);
            if (maxSeenId > state.getLastUserMessageId()) {
                state.setLastUserMessageId(maxSeenId);
                syncStateRepository.save(state);
            }
        }

        log.info("Saved Messages import finished: imported={}, skipped={}, maxId={}", imported, skipped, maxSeenId);
        return new ImportResult(imported, skipped, maxSeenId);
    }

    private SyncState createState() {
        SyncState state = new SyncState();
        state.setId(1L);
        state.setLastUpdateId(0L);
        state.setLastUserMessageId(0L);
        return syncStateRepository.save(state);
    }

    public record ImportResult(int imported, int skipped, long maxId) {
    }
}
