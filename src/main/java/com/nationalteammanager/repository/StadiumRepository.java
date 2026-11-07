package com.nationalteammanager.repository;

import com.nationalteammanager.entity.Stadium;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    @EntityGraph(attributePaths = "team")
    Optional<Stadium> findByIdAndTeamOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "team")
    List<Stadium> findByTeamOwnerIdOrderByTeamTeamNameAscMainStadiumDescNameAsc(Long ownerId);

    @EntityGraph(attributePaths = "team")
    List<Stadium> findByTeamIdOrderByMainStadiumDescNameAsc(Long teamId);

    Optional<Stadium> findByTeamIdAndMainStadiumTrue(Long teamId);

    boolean existsByTeamIdAndNameIgnoreCase(Long teamId, String name);

    boolean existsByTeamIdAndNameIgnoreCaseAndIdNot(Long teamId, String name, Long id);
}
