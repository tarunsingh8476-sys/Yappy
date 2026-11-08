package com.smartcommunity.membership;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByServerIdAndUserId(Long serverId, Long userId);

    long countByServerIdAndBannedFalse(Long serverId);

    boolean existsByServerIdAndUserIdAndBannedFalse(Long serverId, Long userId);

    Page<Membership> findByServerIdAndBannedFalse(Long serverId, Pageable pageable);
}
