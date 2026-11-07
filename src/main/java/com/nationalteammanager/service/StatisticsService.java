package com.nationalteammanager.service;

import com.nationalteammanager.dto.DashboardView;
import com.nationalteammanager.dto.OverallStatistics;
import com.nationalteammanager.dto.PlayerEventCount;
import com.nationalteammanager.dto.PlayerMatchRef;
import com.nationalteammanager.dto.PlayerStatistics;
import com.nationalteammanager.dto.TeamStatistics;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.EventType;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.MatchEventRepository;
import com.nationalteammanager.repository.MatchRepository;
import com.nationalteammanager.repository.NationalTeamRepository;
import com.nationalteammanager.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Sva statistika se računa iz utakmica, događaja i sastava; ništa se ne upisuje ručno. */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final int RANKING_SIZE = 5;

    private final NationalTeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final LineupRepository lineupRepository;
    private final MatchService matchService;

    public StatisticsService(NationalTeamRepository teamRepository, PlayerRepository playerRepository,
                             MatchRepository matchRepository, MatchEventRepository eventRepository,
                             LineupRepository lineupRepository, MatchService matchService) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.eventRepository = eventRepository;
        this.lineupRepository = lineupRepository;
        this.matchService = matchService;
    }

    public DashboardView getDashboard(Long ownerId) {
        return new DashboardView(getOverallStatistics(ownerId), matchService.findRecentResults(ownerId),
                matchService.findUpcoming(ownerId), teamRepository.findByOwnerIdOrderByTeamNameAsc(ownerId));
    }

    public OverallStatistics getOverallStatistics(Long ownerId) {
        List<NationalTeam> teams = teamRepository.findByOwnerIdOrderByTeamNameAsc(ownerId);
        List<Player> players = playerRepository.findByTeamOwnerIdOrderByLastNameAscFirstNameAsc(ownerId);
        List<Match> finished = matchRepository.findByHomeTeamOwnerIdAndStatus(ownerId, MatchStatus.FINISHED);

        List<TeamStatistics> teamStatistics = buildTeamStatistics(teams, players, finished);
        List<PlayerStatistics> playerStatistics = buildPlayerStatistics(players, ownerId);

        int wins = teamStatistics.stream().mapToInt(TeamStatistics::wins).sum();
        int losses = teamStatistics.stream().mapToInt(TeamStatistics::losses).sum();
        int draws = (int) finished.stream().filter(match -> match.getHomeGoals().equals(match.getAwayGoals())).count();
        int goals = finished.stream().mapToInt(match -> match.getHomeGoals() + match.getAwayGoals()).sum();

        return new OverallStatistics(teams.size(), players.size(), matchRepository.countByHomeTeamOwnerId(ownerId),
                finished.size(), wins, losses, draws, goals,
                ranking(playerStatistics, PlayerStatistics::goals),
                ranking(playerStatistics, PlayerStatistics::appearances),
                teamStatistics, playerStatistics);
    }

    private List<TeamStatistics> buildTeamStatistics(List<NationalTeam> teams, List<Player> players, List<Match> finished) {
        Map<Long, Long> squadSizes = players.stream()
                .collect(Collectors.groupingBy(player -> player.getTeam().getId(), Collectors.counting()));
        return teams.stream()
                .map(team -> TeamStatistics.of(team, squadSizes.getOrDefault(team.getId(), 0L), finished))
                .sorted(Comparator.comparingInt(TeamStatistics::points).reversed()
                        .thenComparing(Comparator.comparingInt(TeamStatistics::goalDifference).reversed())
                        .thenComparing(TeamStatistics::teamName))
                .toList();
    }

    private List<PlayerStatistics> buildPlayerStatistics(List<Player> players, Long ownerId) {
        Map<Long, Map<EventType, Long>> events = eventRepository.countEventsPerPlayer(ownerId).stream()
                .collect(Collectors.groupingBy(PlayerEventCount::playerId,
                        Collectors.toMap(PlayerEventCount::type, PlayerEventCount::total)));
        Map<Long, Set<Long>> appearances = appearancesPerPlayer(ownerId);

        return players.stream()
                .map(player -> {
                    Map<EventType, Long> counts = events.getOrDefault(player.getId(), Map.of());
                    return new PlayerStatistics(player.getId(), player.getFullName(), player.getTeam().getTeamName(),
                            player.getPosition(),
                            appearances.getOrDefault(player.getId(), Set.of()).size(),
                            counts.getOrDefault(EventType.GOAL, 0L),
                            counts.getOrDefault(EventType.ASSIST, 0L),
                            counts.getOrDefault(EventType.YELLOW_CARD, 0L),
                            counts.getOrDefault(EventType.RED_CARD, 0L));
                })
                .toList();
    }

    /** Nastup je početak utakmice u prvih 11 ili ulazak s klupe; po utakmici se broji jednom. */
    private Map<Long, Set<Long>> appearancesPerPlayer(Long ownerId) {
        Map<Long, Set<Long>> appearances = new HashMap<>();
        Stream.concat(lineupRepository.findStarterAppearances(ownerId).stream(),
                        eventRepository.findSubstituteAppearances(ownerId).stream())
                .forEach((PlayerMatchRef ref) -> appearances
                        .computeIfAbsent(ref.playerId(), id -> new HashSet<>())
                        .add(ref.matchId()));
        return appearances;
    }

    private List<PlayerStatistics> ranking(List<PlayerStatistics> players, ToLongFunction<PlayerStatistics> metric) {
        return players.stream()
                .filter(player -> metric.applyAsLong(player) > 0)
                .sorted(Comparator.comparingLong(metric).reversed().thenComparing(PlayerStatistics::playerName))
                .limit(RANKING_SIZE)
                .toList();
    }
}
