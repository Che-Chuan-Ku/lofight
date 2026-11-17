package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.AccountErrorCode;
import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.PlayerDetailResponse;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.RunState;
import com.crowdsiege.api.model.Tier;
import com.crowdsiege.api.repository.NicknameRow;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.RunRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Player progress (silver unlock state) and nickname maintenance.
 */
@Service
public class PlayerService {

    private static final int NICKNAME_MAX_LENGTH = 12;
    private static final long NICKNAME_LOCK_KEY = 7_305_001L;

    private final PlayerRepository playerRepository;
    private final RunRepository runRepository;
    private final GameProperties gameProperties;
    private final Clock clock;
    private final EntityManager entityManager;

    public PlayerService(PlayerRepository playerRepository, RunRepository runRepository,
                         GameProperties gameProperties, Clock clock, EntityManager entityManager) {
        this.entityManager = entityManager;
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
        String trimmed = nickname == null ? null : nickname.strip();
        validateNickname(trimmed);
        // One global advisory lock (released at commit) serializes renames, so two players cannot claim the same
        // name concurrently. A DB unique index is impossible: existing data already contains duplicates.
        entityManager.createNativeQuery("SELECT CAST(pg_advisory_xact_lock(?1) AS text)")
                .setParameter(1, NICKNAME_LOCK_KEY).getSingleResult();
        Player player = getPlayer(playerId);
        if (isTakenByOther(playerId, trimmed)) {
            throw new BusinessException(AccountErrorCode.NICKNAME_TAKEN);
        }
        player.rename(trimmed, clock.instant());
        return toResponse(player);
    }

    /**
     * True when another non-voided player (member or guest) already uses the name under NicknameRules.normalize.
     * Only user-initiated renames are checked.
     */
    boolean isTakenByOther(Long playerId, String nickname) {
        return !activePlayersNamed(playerId, nickname).isEmpty();
    }

    /**
     * Non-voided players (other than excludeId; 0 excludes nobody) whose nickname equals the name under
     * NicknameRules.normalize.
     */
    public List<NicknameRow> activePlayersNamed(Long excludeId, String nickname) {
        String wanted = NicknameRules.normalize(nickname);
        return playerRepository.findActiveNicknames().stream()
                .filter(p -> !p.id().equals(excludeId) && NicknameRules.normalize(p.nickname()).equals(wanted))
                .toList();
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
        if (NicknameRules.hasIllegalCharacter(nickname)) {
            // control / format / look-alike space characters: same answer as a banned word (acc002)
            throw new BusinessException(AccountErrorCode.NICKNAME_CONTAINS_BANNED_WORD);
        }
        if (gameProperties.getBannedWords().stream().anyMatch(nickname::contains)) {
            throw new BusinessException(AccountErrorCode.NICKNAME_CONTAINS_BANNED_WORD);
        }
    }

    public record SilverUnlockHint(Integer silverUnlockBossSeq, Integer bronzeBestBossesKilled) {

        static final SilverUnlockHint NONE = new SilverUnlockHint(null, null);
    }
}
