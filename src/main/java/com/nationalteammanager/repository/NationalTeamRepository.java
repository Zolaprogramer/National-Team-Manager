package com.nationalteammanager.repository;

import com.nationalteammanager.entity.NationalTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NationalTeamRepository extends JpaRepository<NationalTeam, Long> {

    List<NationalTeam> findByOwnerIdOrderByTeamNameAsc(Long ownerId);

    Optional<NationalTeam> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndTeamNameIgnoreCase(Long ownerId, String teamName);

    boolean existsByOwnerIdAndTeamNameIgnoreCaseAndIdNot(Long ownerId, String teamName, Long id);

    long countByOwnerId(Long ownerId);
}
