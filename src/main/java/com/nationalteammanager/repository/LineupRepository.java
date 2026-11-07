package com.nationalteammanager.repository;

import com.nationalteammanager.dto.PlayerMatchRef;
import com.nationalteammanager.entity.Lineup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LineupRepository extends JpaRepository<Lineup, Long> {

    @EntityGraph(attributePaths = {"captain", "penaltyTaker", "freeKickTaker", "cornerTaker", "lineupPlayers", "lineupPlayers.player"})
    Optional<Lineup> findByMatchIdAndTeamId(Long matchId, Long teamId);

    @EntityGraph(attributePaths = "team")
    List<Lineup> findByMatchId(Long matchId);

    boolean existsByCaptainId(Long playerId);

    @Query("""
            select count(lp) > 0 from LineupPlayer lp
            where lp.player.id = :playerId
            """)
    boolean isPlayerInAnyLineup(@Param("playerId") Long playerId);

    @Query("""
            select new com.nationalteammanager.dto.PlayerMatchRef(lp.player.id, l.match.id)
            from LineupPlayer lp join lp.lineup l
            where l.match.status = com.nationalteammanager.enums.MatchStatus.FINISHED
              and lp.role = com.nationalteammanager.enums.LineupRole.STARTER
              and l.team.owner.id = :ownerId
            """)
    List<PlayerMatchRef> findStarterAppearances(@Param("ownerId") Long ownerId);
}
