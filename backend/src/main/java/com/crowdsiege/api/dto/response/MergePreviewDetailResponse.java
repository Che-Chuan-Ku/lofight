package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.MergeTargetKind;
import java.util.List;

public record MergePreviewDetailResponse(
        MergeTargetKind targetKind,
        int guestCoins,
        boolean guestIsSilverUnlocked,
        List<BuffQuantityResponse> guestBuffInventories,
        List<BestScoreResponse> guestBestScores,
        int mergedCoins,
        boolean mergedIsSilverUnlocked,
        List<BuffQuantityResponse> mergedBuffInventories,
        List<BestScoreResponse> mergedBestScores) {
}
