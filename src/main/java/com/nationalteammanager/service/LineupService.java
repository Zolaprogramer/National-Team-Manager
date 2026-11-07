package com.nationalteammanager.service;

import com.nationalteammanager.dto.LineupEditor;
import com.nationalteammanager.dto.LineupForm;
import com.nationalteammanager.dto.LineupView;
import com.nationalteammanager.dto.TacticsForm;
import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.LineupPlayer;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.FormationSlot;
import com.nationalteammanager.enums.LineupRole;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.enums.PlayerDuty;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LineupService {

    public static final int MAX_SUBSTITUTES = 15;

    private final LineupRepository lineupRepository;
    private final PlayerRepository playerRepository;
    private final MatchService matchService;
    private final LineupSnapshotMapper snapshotMapper;

    public LineupService(LineupRepository lineupRepository, PlayerRepository playerRepository,
                         MatchService matchService, LineupSnapshotMapper snapshotMapper) {
        this.lineupRepository = lineupRepository;
        this.playerRepository = playerRepository;
        this.matchService = matchService;
        this.snapshotMapper = snapshotMapper;
    }

    public Optional<LineupView> findView(Long matchId, Long teamId, Long ownerId) {
        Match match = matchService.getOwned(matchId, ownerId);
        participant(match, teamId);
        return lineupRepository.findByMatchIdAndTeamId(matchId, teamId).map(this::toView);
    }

    public LineupEditor getEditor(Long matchId, Long teamId, Long ownerId) {
        Match match = matchService.getOwned(matchId, ownerId);
        NationalTeam team = participant(match, teamId);
        List<Player> squad = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId);
        Map<Boolean, List<Player>> byGoalkeeper = squad.stream().collect(Collectors.partitioningBy(Player::isGoalkeeper));
        List<Player> outfield = byGoalkeeper.get(false).stream()
                .sorted(Comparator.comparing(Player::getPosition).thenComparing(Player::getJerseyNumber))
                .toList();
        return new LineupEditor(match, team, squad, byGoalkeeper.get(true), outfield);
    }

    public LineupForm getForm(Long matchId, Long teamId, Long ownerId) {
        participant(matchService.getOwned(matchId, ownerId), teamId);
        return lineupRepository.findByMatchIdAndTeamId(matchId, teamId).map(LineupForm::from).orElseGet(LineupForm::new);
    }

    /** Taktičke postavke i uloge postojećih igrača ostaju nepromijenjene. */
    @Transactional
    public void save(Long matchId, Long teamId, LineupForm form, Long ownerId) {
        applyLineup(matchId, teamId, form, ownerId, null);
    }

    /**
     * Provjerava pravila sastava i čuva ga, a prethodno stanje pamti za vraćanje.
     * Ako je duties null, igrači koji ostaju u sastavu zadržavaju svoje uloge.
     */
    @Transactional
    public Lineup applyLineup(Long matchId, Long teamId, LineupForm form, Long ownerId, Map<Long, PlayerDuty> duties) {
        Match match = matchService.getOwned(matchId, ownerId);
        NationalTeam team = participant(match, teamId);
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new BusinessRuleException("error.lineup.cancelled");
        }
        Map<Long, Player> squad = squadOf(teamId);
        validate(form, squad);

        Lineup lineup = lineupRepository.findByMatchIdAndTeamId(matchId, teamId).orElseGet(() -> new Lineup(match, team));
        if (lineup.getId() != null) {
            lineup.rememberPreviousState(snapshotMapper.toJson(TacticsForm.from(lineup)));
        }
        Map<Long, PlayerDuty> playerDuties = duties != null ? duties : lineup.getDutiesByPlayer();

        lineup.updateDetails(form.getFormation(), form.getTactic(), squad.get(form.getCaptainId()));
        lineup.removeTakersOutside(form.getStarterIds());

        // Hibernate izvršava INSERT prije DELETE, pa se stari igrači moraju obrisati prije dodavanja novih.
        // U suprotnom bi ponovo izabrani igrač narušio jedinstveni ključ (lineupId, playerId).
        lineup.getLineupPlayers().clear();
        lineupRepository.saveAndFlush(lineup);

        List<Long> starters = form.getStarterIds();
        for (int slot = 0; slot < starters.size(); slot++) {
            Long id = starters.get(slot);
            lineup.addStarter(squad.get(id), slot, playerDuties.getOrDefault(id, PlayerDuty.BALANCED));
        }
        substitutesOf(form).forEach(id ->
                lineup.addSubstitute(squad.get(id), playerDuties.getOrDefault(id, PlayerDuty.BALANCED)));
        return lineup;
    }

    @Transactional
    public void delete(Long matchId, Long teamId, Long ownerId) {
        participant(matchService.getOwned(matchId, ownerId), teamId);
        Lineup lineup = lineupRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("error.lineup.notFound"));
        lineupRepository.delete(lineup);
    }

    Map<Long, Player> squadOf(Long teamId) {
        return playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
    }

    NationalTeam participant(Match match, Long teamId) {
        if (match.getHomeTeam().getId().equals(teamId)) {
            return match.getHomeTeam();
        }
        if (match.getAwayTeam().getId().equals(teamId)) {
            return match.getAwayTeam();
        }
        throw new ResourceNotFoundException("error.lineup.notParticipant");
    }

    private void validate(LineupForm form, Map<Long, Player> squad) {
        List<Long> starters = form.getStarterIds();
        boolean allSlotsFilled = starters.size() == LineupForm.STARTER_COUNT && starters.stream().allMatch(Objects::nonNull);
        if (!allSlotsFilled) {
            throw new BusinessRuleException("error.lineup.startersCount", LineupForm.STARTER_COUNT);
        }
        List<Long> substitutes = substitutesOf(form);
        if (substitutes.size() > MAX_SUBSTITUTES) {
            throw new BusinessRuleException("error.lineup.maxSubstitutes", MAX_SUBSTITUTES);
        }

        Set<Long> selected = new HashSet<>();
        for (Long id : starters) {
            requireFirstSelection(selected, id, squad);
        }
        for (Long id : substitutes) {
            requireFirstSelection(selected, id, squad);
        }

        validateGoalkeeperPlacement(starters, squad);
        if (!starters.contains(form.getCaptainId())) {
            throw new BusinessRuleException("error.lineup.captain");
        }
    }

    private void requireFirstSelection(Set<Long> selected, Long playerId, Map<Long, Player> squad) {
        if (!squad.containsKey(playerId)) {
            throw new BusinessRuleException("error.lineup.foreignPlayer");
        }
        if (!selected.add(playerId)) {
            throw new BusinessRuleException("error.lineup.duplicate", squad.get(playerId).getFullName());
        }
    }

    private void validateGoalkeeperPlacement(List<Long> starters, Map<Long, Player> squad) {
        for (int slot = 0; slot < starters.size(); slot++) {
            boolean goalkeeperSlot = slot == Formation.GOALKEEPER_SLOT;
            if (goalkeeperSlot != squad.get(starters.get(slot)).isGoalkeeper()) {
                throw new BusinessRuleException("error.lineup.goalkeeper");
            }
        }
    }

    private List<Long> substitutesOf(LineupForm form) {
        return form.getSubstituteIds().stream().filter(Objects::nonNull).toList();
    }

    private LineupView toView(Lineup lineup) {
        List<FormationSlot> slots = lineup.getFormation().getSlots();
        Long captainId = lineup.getCaptain().getId();
        List<LineupView.PitchPlayer> starters = lineup.getPlayersWithRole(LineupRole.STARTER).stream()
                .map(starter -> new LineupView.PitchPlayer(starter.getPlayer(), slots.get(starter.getSlotIndex()),
                        starter.getPlayer().getId().equals(captainId)))
                .toList();
        List<Player> substitutes = lineup.getPlayersWithRole(LineupRole.SUBSTITUTE).stream()
                .map(LineupPlayer::getPlayer)
                .toList();
        return new LineupView(lineup, starters, substitutes);
    }
}
