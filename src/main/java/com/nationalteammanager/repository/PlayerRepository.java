package com.nationalteammanager.repository;

import com.nationalteammanager.entity.Player;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    @Override
    @EntityGraph(attributePaths = "team")
    List<Player> findAll(Specification<Player> specification, Sort sort);

    @EntityGraph(attributePaths = "team")
    Optional<Player> findByIdAndTeamOwnerId(Long id, Long ownerId);

    List<Player> findByTeamIdOrderByJerseyNumberAsc(Long teamId);

    @EntityGraph(attributePaths = "team")
    List<Player> findByTeamOwnerIdOrderByLastNameAscFirstNameAsc(Long ownerId);

    long countByTeamId(Long teamId);

    @Query("""
            select p.team.id from Player p
            where p.team.owner.id = :ownerId
            group by p.team.id
            having count(p) >= :limit
            """)
    List<Long> findTeamIdsWithAtLeast(@Param("ownerId") Long ownerId, @Param("limit") long limit);

    boolean existsByTeamIdAndJerseyNumber(Long teamId, Integer jerseyNumber);

    boolean existsByTeamIdAndJerseyNumberAndIdNot(Long teamId, Integer jerseyNumber, Long id);
}
