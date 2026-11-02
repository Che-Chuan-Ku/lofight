package com.crowdsiege.api.steps.helpers;

import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.model.PlayerIdentity;
import com.crowdsiege.api.repository.PlayerIdentityRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolves a player reference cell: either a player name or 第三方身分 PROVIDER "sub" 的帳號.
 */
@Component
public class PlayerReferenceResolver {

    private static final Pattern IDENTITY_REFERENCE = Pattern.compile("第三方身分 (\\w+) \"(.+)\" 的帳號");

    @Autowired
    private PlayerIdentityRepository playerIdentityRepository;

    @Autowired
    private ScenarioContextHelper contextHelper;

    public Long resolve(String reference) {
        Matcher matcher = IDENTITY_REFERENCE.matcher(reference);
        if (matcher.matches()) {
            return accountOf(matcher.group(1), matcher.group(2));
        }
        return contextHelper.getPlayerId(reference);
    }

    public Long accountOf(String provider, String subject) {
        return playerIdentityRepository
                .findByLoginProviderAndProviderSubject(LoginProvider.valueOf(provider), subject)
                .map(PlayerIdentity::getPlayerId)
                .orElseThrow(() -> new AssertionError("第三方身分 " + provider + " \"" + subject + "\" 尚無帳號"));
    }
}
