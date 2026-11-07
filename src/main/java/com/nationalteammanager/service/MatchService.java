package com.nationalteammanager.service;

import com.nationalteammanager.dto.MatchDetails;
import com.nationalteammanager.dto.MatchFilterForm;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.MatchResultForm;
import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.enums.EventType;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.MatchEventRepository;
import com.nationalteammanager.repository.MatchRepository;
import com.nationalteammanager.repository.PlayerRepository;
import com.nationalteammanager.repository.StadiumRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MatchService {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;
    private static final long NO_MATCH_ID = -1L;

    private final MatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final LineupRepository lineupRepository;
    private final PlayerRepository playerRepository;
    private final NationalTeamService teamService;
    private final CompetitionService competitionService;
    private final StadiumRepository stadiumRepository;

    public MatchService(MatchRepository matchRepository, MatchEventRepository eventRepository,
                        LineupRepository lineupRepository, PlayerRepository playerRepository,
                        NationalTeamService teamService, CompetitionService competitionService,
                        StadiumRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
        this.matchRepository = matchRepository;
        this.eventRepository = eventRepository;
        this.lineupRepository = lineupRepository;
        this.playerRepository = playerRepository;
        this.teamService = teamService;
        this.competitionService = competitionService;
    }

    public List<Match> find(MatchFilterForm filter, Long ownerId) {
        boolean upcoming = MatchFilterForm.VIEW_UPCOMING.equals(filter.getView());
        Sort sort = upcoming ? Sort.by("kickoff") : Sort.by(Sort.Direction.DESC, "kickoff");
        return matchRepository.findAll(matching(filter, ownerId), sort);
    }

    public List<Match> findRecentResults(Long ownerId) {
        return matchRepository.findTop5ByHomeTeamOwnerIdAndStatusOrderByKickoffDesc(ownerId, MatchStatus.FINISHED);
    }

    public List<Match> findUpcoming(Long ownerId) {
        return matchRepository.findTop5ByHomeTeamOwnerIdAndStatusOrderByKickoffAsc(ownerId, MatchStatus.SCHEDULED);
    }

    public Match getOwned(Long id, Long ownerId) {
        return matchRepository.findByIdAndHomeTeamOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("error.match.notFound"));
    }

    public MatchDetails getDetails(Long id, Long ownerId) {
        Match match = getOwned(id, ownerId);
        List<Lineup> lineups = lineupRepository.findByMatchId(id);
        return new MatchDetails(match,
                eventRepository.findByMatchIdOrderByMinuteAscIdAsc(id),
                playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getHomeTeam().getId()),
                playerRepository.findByTeamIdOrderByJerseyNumberAsc(match.getAwayTeam().getId()),
                hasLineup(lineups, match.getHomeTeam()),
                hasLineup(lineups, match.getAwayTeam()));
    }

    @Transactional
    public Match create(MatchForm form, Long ownerId) {
        Match match = new Match();
        applyForm(match, form, ownerId);
        return matchRepository.save(match);
    }

    @Transactional
    public void update(Long id, MatchForm form, Long ownerId) {
        applyForm(getOwned(id, ownerId), form, ownerId);
    }

    /** Sastavi i događaji se brišu zajedno s utakmicom (cascade u entitetu Match). */
    @Transactional
    public void delete(Long id, Long ownerId) {
        matchRepository.delete(getOwned(id, ownerId));
    }

    @Transactional
    public void recordResult(Long id, MatchResultForm form, Long ownerId) {
        Match match = getOwned(id, ownerId);
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new BusinessRuleException("error.match.cancelledResult");
        }
        MatchStatus status = form.isFinished() ? MatchStatus.FINISHED : MatchStatus.IN_PROGRESS;
        validateKickoffForStatus(match.getKickoff(), status);
        applyStatusAndScore(match, status, form.getHomeGoals(), form.getAwayGoals());
    }

    private void applyForm(Match match, MatchForm form, Long ownerId) {
        NationalTeam home = teamService.getOwned(form.getHomeTeamId(), ownerId);
        NationalTeam away = teamService.getOwned(form.getAwayTeamId(), ownerId);
        Competition competition = form.getCompetitionId() == null
                ? null : competitionService.getVisible(form.getCompetitionId(), ownerId);

        validateTeamChange(match, home, away);
        validateKickoff(form, competition);
        validateTeamsAvailable(match, form, home, away);

        Stadium venue = resolveVenue(form, home, ownerId);
        match.updateDetails(home, away, competition, form.getKickoff(), venue,
                venue != null ? venue.getName() : form.getStadium());
        applyStatusAndScore(match, form.getStatus(), form.getHomeGoals(), form.getAwayGoals());
    }

    /** Stadion iz evidencije mora pripadati domaćinu, ali ne mora biti njegov glavni stadion. */
    private Stadium resolveVenue(MatchForm form, NationalTeam home, Long ownerId) {
        if (form.getStadiumId() == null) {
            if (form.getStadium() == null || form.getStadium().isBlank()) {
                throw new BusinessRuleException("error.match.stadiumRequired");
            }
            return null;
        }
        Stadium venue = stadiumRepository.findByIdAndTeamOwnerId(form.getStadiumId(), ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("error.stadium.notFound"));
        if (!venue.belongsTo(home)) {
            throw new BusinessRuleException("error.match.venueNotHome");
        }
        return venue;
    }

    private void validateTeamChange(Match match, NationalTeam home, NationalTeam away) {
        if (match.getId() == null) {
            return;
        }
        boolean teamsChanged = !match.isHomeTeam(home) || !match.getAwayTeam().getId().equals(away.getId());
        if (teamsChanged && (!match.getLineups().isEmpty() || !match.getEvents().isEmpty())) {
            throw new BusinessRuleException("error.match.teamsLocked");
        }
    }

    private void validateKickoff(MatchForm form, Competition competition) {
        LocalDateTime kickoff = form.getKickoff();
        if (kickoff.getYear() < MIN_YEAR || kickoff.getYear() > MAX_YEAR) {
            throw new BusinessRuleException("error.match.invalidDate");
        }
        validateKickoffForStatus(kickoff, form.getStatus());
        if (competition != null && !competition.includes(kickoff.toLocalDate())) {
            throw new BusinessRuleException("error.match.outsideCompetition", competition.getName());
        }
    }

    private void validateKickoffForStatus(LocalDateTime kickoff, MatchStatus status) {
        boolean inFuture = kickoff.isAfter(LocalDateTime.now());
        if (status == MatchStatus.SCHEDULED && !inFuture) {
            throw new BusinessRuleException("error.match.scheduledFuture");
        }
        if (status.hasStarted() && inFuture) {
            throw new BusinessRuleException("error.match.notStarted");
        }
    }

    /** Reprezentacija ne može odigrati dvije utakmice istog dana. */
    private void validateTeamsAvailable(Match match, MatchForm form, NationalTeam home, NationalTeam away) {
        if (form.getStatus() == MatchStatus.CANCELLED) {
            return;
        }
        LocalDateTime dayStart = form.getKickoff().toLocalDate().atStartOfDay();
        Long excludedId = match.getId() == null ? NO_MATCH_ID : match.getId();
        if (matchRepository.existsConflict(List.of(home.getId(), away.getId()), dayStart, dayStart.plusDays(1), excludedId)) {
            throw new BusinessRuleException("error.match.dayConflict");
        }
    }

    private void applyStatusAndScore(Match match, MatchStatus status, Integer homeGoals, Integer awayGoals) {
        boolean hasEvents = match.getId() != null && !match.getEvents().isEmpty();
        if (!status.hasStarted()) {
            if (hasEvents) {
                throw new BusinessRuleException("error.match.hasEvents");
            }
            match.updateResult(status, null, null);
            return;
        }
        if (status == MatchStatus.FINISHED && (homeGoals == null || awayGoals == null)) {
            throw new BusinessRuleException("error.match.resultRequired");
        }
        int home = homeGoals == null ? 0 : homeGoals;
        int away = awayGoals == null ? 0 : awayGoals;
        if (match.getId() != null) {
            validateScoreCoversGoalEvents(match, match.getHomeTeam(), home);
            validateScoreCoversGoalEvents(match, match.getAwayTeam(), away);
        }
        match.updateResult(status, home, away);
    }

    private void validateScoreCoversGoalEvents(Match match, NationalTeam team, int goals) {
        long recorded = eventRepository.countByMatchIdAndPlayerTeamIdAndType(match.getId(), team.getId(), EventType.GOAL);
        if (goals < recorded) {
            throw new BusinessRuleException("error.match.scoreBelowGoals", team.getTeamName(), recorded);
        }
    }

    private boolean hasLineup(List<Lineup> lineups, NationalTeam team) {
        return lineups.stream().anyMatch(lineup -> lineup.getTeam().getId().equals(team.getId()));
    }

    private Specification<Match> matching(MatchFilterForm filter, Long ownerId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("homeTeam").get("owner").get("id"), ownerId));

            if (filter.getTeamId() != null) {
                predicates.add(builder.or(
                        builder.equal(root.get("homeTeam").get("id"), filter.getTeamId()),
                        builder.equal(root.get("awayTeam").get("id"), filter.getTeamId())));
            }
            if (filter.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), filter.getStatus()));
            }
            if (MatchFilterForm.VIEW_UPCOMING.equals(filter.getView())) {
                predicates.add(builder.equal(root.get("status"), MatchStatus.SCHEDULED));
            } else if (MatchFilterForm.VIEW_FINISHED.equals(filter.getView())) {
                predicates.add(builder.equal(root.get("status"), MatchStatus.FINISHED));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
