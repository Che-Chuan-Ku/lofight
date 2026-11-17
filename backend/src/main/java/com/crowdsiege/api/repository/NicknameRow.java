package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.PlayerKind;

/**
 * Light projection of a non-voided player used to compare nicknames in Java (see NicknameRules).
 */
public record NicknameRow(Long id, String nickname, PlayerKind playerKind) {
}
