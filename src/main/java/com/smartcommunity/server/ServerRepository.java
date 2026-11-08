package com.smartcommunity.server;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    Optional<Server> findByInviteCode(String inviteCode);

    Page<Server> findByVisibilityIgnoreCase(String visibility, Pageable pageable);

    @Query("SELECT s FROM Server s JOIN s.memberships m WHERE m.user.id = :userId AND m.banned = false")
    List<Server> findAllJoinedServersByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Server s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) AND UPPER(s.visibility) = 'PUBLIC'")
    Page<Server> searchPublicServers(@Param("query") String query, Pageable pageable);
}
