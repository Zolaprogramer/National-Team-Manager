package com.nationalteammanager.service;

import com.nationalteammanager.dto.PlayerSearchForm;
import com.nationalteammanager.entity.Player;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PlayerSpecifications {

    private PlayerSpecifications() {
    }

    /** U upit ulaze samo popunjeni kriteriji, a vlasnik je uvijek obavezan. */
    static Specification<Player> matching(Long ownerId, PlayerSearchForm search) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("team").get("owner").get("id"), ownerId));

            if (search.getTeamId() != null) {
                predicates.add(builder.equal(root.get("team").get("id"), search.getTeamId()));
            }
            if (StringUtils.hasText(search.getFirstName())) {
                predicates.add(contains(builder, root, "firstName", search.getFirstName()));
            }
            if (StringUtils.hasText(search.getLastName())) {
                predicates.add(contains(builder, root, "lastName", search.getLastName()));
            }
            if (search.getPosition() != null) {
                predicates.add(builder.equal(root.get("position"), search.getPosition()));
            }
            if (search.getJerseyNumber() != null) {
                predicates.add(builder.equal(root.get("jerseyNumber"), search.getJerseyNumber()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate contains(CriteriaBuilder builder, Root<Player> root, String field, String value) {
        return builder.like(builder.lower(root.get(field)), "%" + value.trim().toLowerCase(Locale.ROOT) + "%");
    }
}
