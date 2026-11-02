package com.crowdsiege.api.repository;

import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.model.PlayerIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerIdentityRepository extends JpaRepository<PlayerIdentity, Long> {

    Optional<PlayerIdentity> findByLoginProviderAndProviderSubject(LoginProvider loginProvider, String providerSubject);

    long countByLoginProviderAndProviderSubject(LoginProvider loginProvider, String providerSubject);
}
