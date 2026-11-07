package com.nationalteammanager.service;

import com.nationalteammanager.dto.TacticsBoard;
import com.nationalteammanager.dto.TacticsForm;
import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Taktička tabla nadograđuje postojeći sastav, pa se pravila sastava provjeravaju u LineupService. */
@Service
@Transactional(readOnly = true)
public class TacticsService {

    private final LineupRepository lineupRepository;
    private final PlayerRepository playerRepository;
    private final LineupService lineupService;
    private final MatchService matchService;
    private final LineupSnapshotMapper snapshotMapper;

    public TacticsService(LineupRepository lineupRepository, PlayerRepository playerRepository,
                          LineupService lineupService, MatchService matchService,
                          LineupSnapshotMapper snapshotMapper) {
        this.lineupRepository = lineupRepository;
        this.playerRepository = playerRepository;
        this.lineupService = lineupService;
        this.matchService = matchService;
        this.snapshotMapper = snapshotMapper;
    }

    /** Vraća prazno ako sastav još ne postoji, jer se tabla otvara nad već napravljenim sastavom. */
    public Optional<TacticsForm> findForm(Long matchId, Long teamId, Long ownerId) {
        lineupService.participant(matchService.getOwned(matchId, ownerId), teamId);
        return lineupRepository.findByMatchIdAndTeamId(matchId, teamId).map(TacticsForm::from);
    }

    public TacticsBoard getBoard(Long matchId, Long teamId, Long ownerId) {
        Match match = matchService.getOwned(matchId, ownerId);
        NationalTeam team = lineupService.participant(match, teamId);
        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new BusinessRuleException("error.lineup.cancelled");
        }
        List<TacticsBoard.BoardPlayer> players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(teamId).stream()
                .map(TacticsBoard.BoardPlayer::of)
                .toList();
        boolean previousStateExists = lineupRepository.findByMatchIdAndTeamId(matchId, teamId)
                .map(lineup -> lineup.getPreviousState() != null)
                .orElse(false);
        return new TacticsBoard(match, team, players, previousStateExists);
    }

    @Transactional
    public void save(Long matchId, Long teamId, TacticsForm form, Long ownerId) {
        lineupService.participant(matchService.getOwned(matchId, ownerId), teamId);
        lineupRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("error.lineup.notFound"));

        Lineup lineup = lineupService.applyLineup(matchId, teamId, form, ownerId, form.getDuties());
        validateIndividualRoles(form);

        Map<Long, Player> squad = lineupService.squadOf(teamId);
        lineup.updateTactics(form.toSettings(), squad.get(form.getPenaltyTakerId()), squad.get(form.getFreeKickTakerId()),
                squad.get(form.getCornerTakerId()));
    }

    /** Vraća stanje prije posljednjeg čuvanja. Trenutno stanje postaje prethodno, pa se vraćanje može i poništiti. */
    @Transactional
    public void restorePrevious(Long matchId, Long teamId, Long ownerId) {
        lineupService.participant(matchService.getOwned(matchId, ownerId), teamId);
        Lineup lineup = lineupRepository.findByMatchIdAndTeamId(matchId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("error.lineup.notFound"));
        if (lineup.getPreviousState() == null) {
            throw new BusinessRuleException("error.tactics.noPrevious");
        }
        save(matchId, teamId, snapshotMapper.fromJson(lineup.getPreviousState()), ownerId);
    }

    private void validateIndividualRoles(TacticsForm form) {
        List<Long> starters = form.getStarterIds();
        for (Long taker : new Long[]{form.getPenaltyTakerId(), form.getFreeKickTakerId(), form.getCornerTakerId()}) {
            if (taker != null && !starters.contains(taker)) {
                throw new BusinessRuleException("error.tactics.takerNotStarter");
            }
        }
        Set<Long> selected = new HashSet<>(starters);
        selected.addAll(form.getSubstituteIds());
        if (!selected.containsAll(form.getDuties().keySet())) {
            throw new BusinessRuleException("error.tactics.dutyOutsideLineup");
        }
    }
}
