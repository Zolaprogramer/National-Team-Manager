package com.nationalteammanager.repository;

import com.nationalteammanager.dto.PlayerEventCount;
import com.nationalteammanager.dto.PlayerMatchRef;
import com.nationalteammanager.entity.MatchEvent;
import com.nationalteammanager.enums.EventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    @EntityGraph(attributePaths = {"player", "player.team"})
    List<MatchEvent> findByMatchIdOrderByMinuteAscIdAsc(Long matchId);

    @EntityGraph(attributePaths = {"player", "player.team"})
    Optional<MatchEvent> findByIdAndMatchId(Long id, Long matchId);

    long countByMatchIdAndPlayerIdAndType(Long matchId, Long playerId, EventType type);

    long countByMatchIdAndPlayerTeamIdAndType(Long matchId, Long teamId, EventType type);

    boolean existsByPlayerId(Long playerId);

    @Query("""
            select new com.nationalteammanager.dto.PlayerEventCount(e.player.id, e.type, count(e))
            from MatchEvent e
            where e.match.status = com.nationalteammanager.enums.MatchStatus.FINISHED
              and e.player.team.owner.id = :ownerId
            group by e.player.id, e.type
            """)
    List<PlayerEventCount> countEventsPerPlayer(@Param("ownerId") Long ownerId);

    @Query("""
            select new com.nationalteammanager.dto.PlayerMatchRef(e.player.id, e.match.id)
            from MatchEvent e
            where e.match.status = com.nationalteammanager.enums.MatchStatus.FINISHED
              and e.type = com.nationalteammanager.enums.EventType.SUBSTITUTION
              and e.player.team.owner.id = :ownerId
            """)
    List<PlayerMatchRef> findSubstituteAppearances(@Param("ownerId") Long ownerId);
}
