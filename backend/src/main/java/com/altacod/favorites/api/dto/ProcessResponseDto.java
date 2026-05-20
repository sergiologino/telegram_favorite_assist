package com.altacod.favorites.api.dto;

import com.altacod.favorites.processing.PostProcessingService;

public record ProcessResponseDto(
        int processed,
        int failed,
        int skipped,
        int pendingBefore,
        int pendingRemaining,
        int totalServices
) {
    public static ProcessResponseDto from(
            PostProcessingService.ProcessingSummary summary,
            long pendingBefore,
            long pendingRemaining,
            long totalServices
    ) {
        return new ProcessResponseDto(
                summary.processed(),
                summary.failed(),
                summary.skipped(),
                (int) pendingBefore,
                (int) pendingRemaining,
                (int) totalServices
        );
    }
}
