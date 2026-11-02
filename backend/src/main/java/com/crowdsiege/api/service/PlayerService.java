package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.AccountErrorCode;
import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.PlayerDetailResponse;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.RunState;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.RunRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Player progress (silver unlock state) and nickname maintenance.
 */
@Service
public class PlayerService {

    private static final int NICKNAME_MAX_LENGTH = 12;

    private final PlayerRepository playerRepository;
    private final RunRepository runRepository;
    private final GameProperties gameProperties;
    private final Clock clock;

    public PlayerService(PlayerRepository playerRepository, RunRepository runRepository,
                         GameProperties gameProperties, Clock clock) {
        this.playerRepository = playerRepository;
        this.runRepository = runRepository;
        this.gameProperties = gameProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PlayerDetailResponse getProgress(Long playerId) {
        return toResponse(getPlayer(playerId));
    }

    @Transactional
    public PlayerDetailResponse updateNickname(Long playerId, String nickname) {
        validateNickname(nickname);
        Player player = getPlayer(playerId);
        player.rename(nickname, clock.instant());
        return toResponse(player);
    }

    public Player getPlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.TOKEN_INVALID));
    }

    /**
     * Unlock hint shown while silver is locked (ASM-07): best bronze bosses killed over settled runs only.
     */
    public SilverUnlockHint silverUnlockHint(Player player) {
        if (player.isSilverUnlocked()) {
            return SilverUnlockHint.NONE;
        }
        int bestBossesKilled = runRepository.findBestBossesKilled(player.getId(), Tier.BRONZE, RunState.SETTLED)
                .orElse(0);
        return new SilverUnlockHint(gameProperties.getSilverUnlockBossSeq(), bestBossesKilled);
    }

    private PlayerDetailResponse toResponse(Player player) {
        SilverUnlockHint hint = silverUnlockHint(player);
        return new PlayerDetailResponse(String.valueOf(player.getId()), player.getNickname(),
                player.getPlayerKind(), player.isSilverUnlocked(), hint.silverUnlockBossSeq(),
                hint.bronzeBestBossesKilled());
    }

    /**
     * 400001 blank, 400008 longer than 12 characters, acc002 contains a banned word (D05, ASM-11).
     */
    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 nickname");
        }
        if (nickname.codePointCount(0, nickname.length()) > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_LENGTH, "參數長度不符 nickname");
        }
        if (gameProperties.getBannedWords().stream().anyMatch(nickname::contains)) {
            throw new BusinessException(AccountErrorCode.NICKNAME_CONTAINS_BANNED_WORD);
        }
    }

    public record SilverUnlockHint(Integer silverUnlockBossSeq, Integer bronzeBestBossesKilled) {

        static final SilverUnlockHint NONE = new SilverUnlockHint(null, null);
    }
}
