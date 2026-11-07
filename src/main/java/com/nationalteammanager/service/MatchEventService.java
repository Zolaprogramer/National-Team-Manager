package com.nationalteammanager.service;

import com.nationalteammanager.dto.MatchEventForm;
import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.MatchEvent;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.EventType;
import com.nationalteammanager.enums.LineupRole;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.MatchEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class MatchEventService {

    private static final int MAX_YELLOW_CARDS = 2;

    private final MatchEventRepository eventRepository;
    private final LineupRepository lineupRepository;
    private final MatchService matchService;
    private final PlayerService playerService;

    public MatchEventService(MatchEventRepository eventRepository, LineupRepository lineupRepository,
                             MatchService matchService, PlayerService playerService) {
        this.eventRepository = eventRepository;
        this.lineupRepository = lineupRepository;
        this.matchService = matchService;
        this.playerService = playerService;
    }

    /** Novi gol automatski povećava rezultat, da rezultat i događaji ostanu usklađeni. */
    @Transactional
    public void add(Long matchId, MatchEventForm form, Long ownerId) {
        Match match = matchService.getOwned(matchId, ownerId);
        Player player = playerService.getOwned(form.getPlayerId(), ownerId);

        if (!match.getStatus().hasStarted()) {
            throw new BusinessRuleException("error.event.matchNotStarted");
        }
        if (!match.involves(player.getTeam())) {
            throw new BusinessRuleException("error.event.playerNotInMatch");
        }
        validateEventRules(match, player, form.getType());

        eventRepository.save(new MatchEvent(match, player, form.getMinute(), form.getType(), form.getDescription()));
        if (form.getType() == EventType.GOAL) {
            match.addGoalFor(player.getTeam());
        }
    }

    @Transactional
    public void delete(Long matchId, Long eventId, Long ownerId) {
        Match match = matchService.getOwned(matchId, ownerId);
        MatchEvent event = eventRepository.findByIdAndMatchId(eventId, matchId)
                .orElseThrow(() -> new ResourceNotFoundException("error.event.notFound"));

        if (event.getType() == EventType.GOAL) {
            NationalTeam team = event.getPlayer().getTeam();
            long goals = count(match, team, EventType.GOAL);
            if (count(match, team, EventType.ASSIST) >= goals) {
                throw new BusinessRuleException("error.event.assistWithGoal");
            }
            match.removeGoalFor(team);
        }
        eventRepository.delete(event);
    }

    private void validateEventRules(Match match, Player player, EventType type) {
        switch (type) {
            case ASSIST -> {
                if (count(match, player.getTeam(), EventType.ASSIST) >= count(match, player.getTeam(), EventType.GOAL)) {
                    throw new BusinessRuleException("error.event.assistBeforeGoal");
                }
            }
            case YELLOW_CARD -> {
                if (countForPlayer(match, player, EventType.YELLOW_CARD) >= MAX_YELLOW_CARDS) {
                    throw new BusinessRuleException("error.event.maxYellow");
                }
            }
            case RED_CARD -> {
                if (countForPlayer(match, player, EventType.RED_CARD) > 0) {
                    throw new BusinessRuleException("error.event.alreadyRed");
                }
            }
            case SUBSTITUTION -> validateSubstitution(match, player);
            case GOAL -> { }
        }
    }

    /** Ako sastav postoji, u igru može ući samo igrač koji je na klupi. */
    private void validateSubstitution(Match match, Player player) {
        if (countForPlayer(match, player, EventType.SUBSTITUTION) > 0) {
            throw new BusinessRuleException("error.event.alreadySubstituted");
        }
        Optional<Lineup> lineup = lineupRepository.findByMatchIdAndTeamId(match.getId(), player.getTeam().getId());
        boolean onBench = lineup.isEmpty() || lineup.get().getPlayersWithRole(LineupRole.SUBSTITUTE).stream()
                .anyMatch(substitute -> substitute.getPlayer().getId().equals(player.getId()));
        if (!onBench) {
            throw new BusinessRuleException("error.event.notOnBench");
        }
    }

    private long count(Match match, NationalTeam team, EventType type) {
        return eventRepository.countByMatchIdAndPlayerTeamIdAndType(match.getId(), team.getId(), type);
    }

    private long countForPlayer(Match match, Player player, EventType type) {
        return eventRepository.countByMatchIdAndPlayerIdAndType(match.getId(), player.getId(), type);
    }
}
