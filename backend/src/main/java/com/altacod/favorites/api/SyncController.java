package com.altacod.favorites.api;

import com.altacod.favorites.api.dto.ProcessResponseDto;
import com.altacod.favorites.api.dto.SyncResponseDto;
import com.altacod.favorites.domain.PostStatus;
import com.altacod.favorites.domain.ServiceItemRepository;
import com.altacod.favorites.domain.TelegramPostRepository;
import com.altacod.favorites.processing.PostProcessingService;
import com.altacod.favorites.sync.SyncOrchestrator;
import com.altacod.favorites.telegram.TelegramExportParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class SyncController {

    private final SyncOrchestrator syncOrchestrator;
    private final TelegramExportParser exportParser;
    private final PostProcessingService postProcessingService;
    private final TelegramPostRepository postRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ObjectMapper objectMapper;

    public SyncController(
            SyncOrchestrator syncOrchestrator,
            TelegramExportParser exportParser,
            PostProcessingService postProcessingService,
            TelegramPostRepository postRepository,
            ServiceItemRepository serviceItemRepository,
            ObjectMapper objectMapper
    ) {
        this.syncOrchestrator = syncOrchestrator;
        this.exportParser = exportParser;
        this.postProcessingService = postProcessingService;
        this.postRepository = postRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/process/pending")
    public ProcessResponseDto processPending() {
        long pendingBefore = postRepository.countByStatus(PostStatus.PENDING);
        PostProcessingService.ProcessingSummary summary = postProcessingService.processPendingPosts();
        return ProcessResponseDto.from(
                summary,
                pendingBefore,
                postRepository.countByStatus(PostStatus.PENDING),
                serviceItemRepository.count()
        );
    }

    @PostMapping("/sync/trigger")
    public SyncResponseDto triggerSync() {
        SyncOrchestrator.SyncResult result = syncOrchestrator.runFullSync();
        return SyncResponseDto.from(result.userResult(), result.botResult(), result.processingSummary());
    }

    @PostMapping(value = "/import/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SyncResponseDto importExport(@RequestParam("file") MultipartFile file) throws Exception {
        JsonNode root = objectMapper.readTree(file.getInputStream());
        TelegramExportParser.ImportResult importResult = exportParser.importExport(root);
        return SyncResponseDto.fromImport(importResult);
    }
}
