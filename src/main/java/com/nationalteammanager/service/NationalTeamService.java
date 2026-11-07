package com.nationalteammanager.service;

import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.TeamDetails;
import com.nationalteammanager.dto.TeamStatistics;
import com.nationalteammanager.dto.TeamSummary;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.enums.FlagChoice;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.MatchRepository;
import com.nationalteammanager.repository.NationalTeamRepository;
import com.nationalteammanager.repository.PlayerRepository;
import com.nationalteammanager.repository.StadiumRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NationalTeamService {

    private final NationalTeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;
    private final PhotoStorageService photoStorageService;
    private final StadiumRepository stadiumRepository;

    public NationalTeamService(NationalTeamRepository teamRepository, PlayerRepository playerRepository,
                               MatchRepository matchRepository, UserService userService,
                               PhotoStorageService photoStorageService, StadiumRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.photoStorageService = photoStorageService;
    }

    public List<NationalTeam> findAll(Long ownerId) {
        return teamRepository.findByOwnerIdOrderByTeamNameAsc(ownerId);
    }

    public List<TeamSummary> findSummaries(Long ownerId) {
        return findAll(ownerId).stream().map(team -> new TeamSummary(team, statisticsOf(team))).toList();
    }

    public NationalTeam getOwned(Long id, Long ownerId) {
        return teamRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("error.team.notFound"));
    }

    public Resource loadCoachPhoto(Long id, Long ownerId) {
        String fileName = getOwned(id, ownerId).getCoachPhotoFileName();
        if (fileName == null) {
            throw new ResourceNotFoundException("error.photo.notFound");
        }
        return photoStorageService.load(fileName)
                .orElseThrow(() -> new ResourceNotFoundException("error.photo.notFound"));
    }

    public Resource loadFlag(Long id, Long ownerId) {
        String fileName = getOwned(id, ownerId).getFlagFileName();
        if (fileName == null) {
            throw new ResourceNotFoundException("error.photo.notFound");
        }
        return photoStorageService.load(fileName)
                .orElseThrow(() -> new ResourceNotFoundException("error.photo.notFound"));
    }

    public TeamDetails getDetails(Long id, Long ownerId) {
        NationalTeam team = getOwned(id, ownerId);
        List<Player> players = playerRepository.findByTeamIdOrderByJerseyNumberAsc(id);
        List<Match> matches = matchRepository.findByTeam(id);
        return new TeamDetails(team, players, matches, TeamStatistics.of(team, players.size(), matches),
                stadiumRepository.findByTeamIdOrderByMainStadiumDescNameAsc(id));
    }

    @Transactional
    public NationalTeam create(NationalTeamForm form, Long ownerId) {
        validateFoundedYear(form);
        if (teamRepository.existsByOwnerIdAndTeamNameIgnoreCase(ownerId, form.getTeamName())) {
            throw new BusinessRuleException("error.team.nameTaken");
        }
        NationalTeam team = new NationalTeam(userService.getById(ownerId));
        applyForm(team, form);
        return teamRepository.save(team);
    }

    @Transactional
    public void update(Long id, NationalTeamForm form, Long ownerId) {
        validateFoundedYear(form);
        NationalTeam team = getOwned(id, ownerId);
        if (teamRepository.existsByOwnerIdAndTeamNameIgnoreCaseAndIdNot(ownerId, form.getTeamName(), id)) {
            throw new BusinessRuleException("error.team.nameTaken");
        }
        applyForm(team, form);
    }

    /** Igrači se brišu zajedno s reprezentacijom, ali reprezentacija s utakmicama mora ostati zbog statistike. */
    @Transactional
    public void delete(Long id, Long ownerId) {
        NationalTeam team = getOwned(id, ownerId);
        if (matchRepository.existsByTeam(id)) {
            throw new BusinessRuleException("error.team.hasMatches");
        }
        List<String> photos = team.getPlayers().stream().map(Player::getPhotoFileName).toList();
        String coachPhoto = team.getCoachPhotoFileName();
        String flag = team.getFlagFileName();
        List<String> stadiumPhotos = team.getStadiums().stream().map(Stadium::getPhotoFileName).toList();
        teamRepository.delete(team);
        photos.forEach(photoStorageService::delete);
        photoStorageService.delete(coachPhoto);
        photoStorageService.delete(flag);
        stadiumPhotos.forEach(photoStorageService::delete);
    }

    private void applyFlag(NationalTeam team, NationalTeamForm form) {
        boolean hasUpload = form.getFlag() != null && !form.getFlag().isEmpty();
        if (hasUpload) {
            team.updateFlag(photoStorageService.replace(team.getFlagFileName(), form.getFlag(), false), false);
        } else if (form.getFlagChoice() == FlagChoice.AUTOMATIC) {
            team.updateFlag(photoStorageService.replace(team.getFlagFileName(), null, true), false);
        } else if (form.getFlagChoice() == FlagChoice.NONE) {
            team.updateFlag(photoStorageService.replace(team.getFlagFileName(), null, true), true);
        }
    }

    private TeamStatistics statisticsOf(NationalTeam team) {
        return TeamStatistics.of(team, playerRepository.countByTeamId(team.getId()),
                matchRepository.findByTeam(team.getId()));
    }

    private void validateFoundedYear(NationalTeamForm form) {
        if (form.getFoundedYear() > Year.now().getValue()) {
            throw new BusinessRuleException("error.team.foundedFuture");
        }
    }

    private void applyForm(NationalTeam team, NationalTeamForm form) {
        team.updateDetails(form.getCountryName(), form.getTeamName(), form.getConfederation(), form.getCoach(),
                form.getFoundedYear(), form.getDescription());
        team.setCoachPhotoFileName(photoStorageService.replace(team.getCoachPhotoFileName(), form.getCoachPhoto(),
                form.isRemoveCoachPhoto()));
        applyFlag(team, form);
    }
}
