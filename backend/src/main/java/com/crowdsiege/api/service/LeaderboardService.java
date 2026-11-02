package com.crowdsiege.api.service;

import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.LeaderboardDetailResponse;
import com.crowdsiege.api.dto.response.LeaderboardEntryListResponse;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.Run;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.LeaderboardEntryRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.RunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Board query per (board version, tier): top N, the caller's rank and up to two neighbours each side (D06, G01-G03).
 */
@Service
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PlayerRepository playerRepository;
    private final RunRepository runRepository;
    private final GameProperties gameProperties;
    private final ResponseTimes responseTimes;

    public LeaderboardService(LeaderboardEntryRepository leaderboardEntryRepository,
                              PlayerRepository playerRepository, RunRepository runRepository,
                              GameProperties gameProperties, ResponseTimes responseTimes) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.playerRepository = playerRepository;
        this.runRepository = runRepository;
        this.gameProperties = gameProperties;
        this.responseTimes = responseTimes;
    }

    @Transactional(readOnly = true)
    public LeaderboardDetailResponse getLeaderboard(Long callerId, String tierParam, String boardVersionParam) {
        Tier tier = EnumParameters.required(Tier.class, tierParam, "tier");
        String boardVersion = boardVersionParam == null || boardVersionParam.isBlank()
                ? gameProperties.getBoardVersion() : boardVersionParam;
        List<LeaderboardEntry> ranked = leaderboardEntryRepository.findByBoardVersionAndTier(boardVersion, tier)
                .stream()
                .sorted(LeaderboardRanking.BOARD_ORDER)
                .toList();
        List<LeaderboardEntryListResponse> rows = toRows(ranked);
        int myIndex = IntStream.range(0, ranked.size())
                .filter(index -> ranked.get(index).getPlayerId().equals(callerId))
                .findFirst()
                .orElse(-1);
        return new LeaderboardDetailResponse(boardVersion, tier,
                rows.subList(0, Math.min(rows.size(), gameProperties.getLeaderboardSize())),
                myIndex < 0 ? null : rows.get(myIndex), neighbors(rows, myIndex));
    }

    private List<LeaderboardEntryListResponse> neighbors(List<LeaderboardEntryListResponse> rows, int myIndex) {
        if (myIndex < 0) {
            return List.of();
        }
        int span = gameProperties.getLeaderboardNeighborSpan();
        List<LeaderboardEntryListResponse> neighbors = new ArrayList<>();
        for (int index = Math.max(0, myIndex - span); index <= Math.min(rows.size() - 1, myIndex + span); index++) {
            if (index != myIndex) {
                neighbors.add(rows.get(index));
            }
        }
        return neighbors;
    }

    private List<LeaderboardEntryListResponse> toRows(List<LeaderboardEntry> ranked) {
        Map<Long, Player> players = playerRepository
                .findAllById(ranked.stream().map(LeaderboardEntry::getPlayerId).toList()).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<Long, Run> runs = runRepository.findAllById(ranked.stream().map(LeaderboardEntry::getRunId).toList())
                .stream()
                .collect(Collectors.toMap(Run::getId, Function.identity()));
        return IntStream.range(0, ranked.size())
                .mapToObj(index -> {
                    LeaderboardEntry entry = ranked.get(index);
                    Run run = runs.get(entry.getRunId());
                    return new LeaderboardEntryListResponse(index + 1, String.valueOf(entry.getPlayerId()),
                            players.get(entry.getPlayerId()).getNickname(), entry.isVictory(),
                            entry.getGameSeconds(), run == null ? null : run.getEquippedBuff(),
                            responseTimes.toResponse(entry.getSettledAt()));
                })
                .toList();
    }
}
