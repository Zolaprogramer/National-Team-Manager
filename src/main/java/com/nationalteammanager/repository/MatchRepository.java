package com.nationalteammanager.repository;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.enums.MatchStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Vlasnik utakmice je vlasnik domaće reprezentacije; servis garantuje da obje reprezentacije imaju istog vlasnika.
 */
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    List<Match> findAll(Specification<Match> specification, Sort sort);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    Optional<Match> findByIdAndHomeTeamOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    List<Match> findTop5ByHomeTeamOwnerIdAndStatusOrderByKickoffDesc(Long ownerId, MatchStatus status);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    List<Match> findTop5ByHomeTeamOwnerIdAndStatusOrderByKickoffAsc(Long ownerId, MatchStatus status);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    List<Match> findByHomeTeamOwnerIdAndStatus(Long ownerId, MatchStatus status);


    List<Match> findByCompetitionId(Long competitionId);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    List<Match> findByCompetitionIdAndHomeTeamOwnerIdOrderByKickoffDesc(Long competitionId, Long ownerId);

    long countByHomeTeamOwnerId(Long ownerId);

    boolean existsByVenueId(Long stadiumId);

    @EntityGraph(attributePaths = {"homeTeam", "awayTeam", "competition", "venue"})
    @Query("""
            select m from Match m
            where m.homeTeam.id = :teamId or m.awayTeam.id = :teamId
            order by m.kickoff desc
            """)
    List<Match> findByTeam(@Param("teamId") Long teamId);

    @Query("""
            select count(m) > 0 from Match m
            where m.homeTeam.id = :teamId or m.awayTeam.id = :teamId
            """)
    boolean existsByTeam(@Param("teamId") Long teamId);

    /** Pronalazi utakmice koje zauzimaju neku od reprezentacija u zadanom vremenskom periodu. */
    @Query("""
            select count(m) > 0 from Match m
            where (m.homeTeam.id in :teamIds or m.awayTeam.id in :teamIds)
              and m.kickoff >= :from and m.kickoff < :to
              and m.status <> com.nationalteammanager.enums.MatchStatus.CANCELLED
              and m.id <> :excludedMatchId
            """)
    boolean existsConflict(@Param("teamIds") List<Long> teamIds,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           @Param("excludedMatchId") Long excludedMatchId);

    @Query("""
            select count(m) > 0 from Match m
            where m.competition.id = :competitionId
              and (m.kickoff < :from or m.kickoff >= :to)
            """)
    boolean existsOutsidePeriod(@Param("competitionId") Long competitionId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
