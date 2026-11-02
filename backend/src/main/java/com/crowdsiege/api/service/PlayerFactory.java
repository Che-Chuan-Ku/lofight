package com.crowdsiege.api.service;

import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.model.Wallet;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.service.gateway.NicknameGenerator;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Creates players with the system default nickname and an empty wallet (D05, D08).
 */
@Component
public class PlayerFactory {

    private final PlayerRepository playerRepository;
    private final WalletRepository walletRepository;
    private final NicknameGenerator nicknameGenerator;

    public PlayerFactory(PlayerRepository playerRepository, WalletRepository walletRepository,
                         NicknameGenerator nicknameGenerator) {
        this.playerRepository = playerRepository;
        this.walletRepository = walletRepository;
        this.nicknameGenerator = nicknameGenerator;
    }

    public Player createGuest(String deviceId, Instant now) {
        return create(PlayerKind.GUEST, deviceId, now);
    }

    public Player createMember(Instant now) {
        return create(PlayerKind.MEMBER, null, now);
    }

    private Player create(PlayerKind playerKind, String deviceId, Instant now) {
        Long playerId = playerRepository.nextId();
        Player player = playerRepository.save(new Player(playerId, playerKind, nicknameGenerator.next(), deviceId, now));
        walletRepository.save(new Wallet(playerId, 0, now));
        return player;
    }
}
