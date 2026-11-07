package com.nationalteammanager.repository;

import com.nationalteammanager.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Zajedničko takmičenje nema vlasnika, pa se vlasnik spaja s LEFT JOIN da takva takmičenja ne ispadnu iz rezultata. */
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    @Query("select c from Competition c left join c.owner o where o is null or o.id = :userId "
            + "order by case when o is null then 0 else 1 end, c.startDate desc")
    List<Competition> findVisible(@Param("userId") Long userId);

    @Query("select c from Competition c left join c.owner o where c.id = :id and (o is null or o.id = :userId)")
    Optional<Competition> findVisible(@Param("id") Long id, @Param("userId") Long userId);
}
