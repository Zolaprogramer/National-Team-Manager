package com.nationalteammanager.service;

import com.nationalteammanager.dto.PlayerForm;
import com.nationalteammanager.dto.PlayerSearchForm;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.LineupRepository;
import com.nationalteammanager.repository.MatchEventRepository;
import com.nationalteammanager.repository.PlayerRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    public static final int MAX_PLAYERS_PER_TEAM = 30;

    private static final Sort SEARCH_ORDER = Sort.by("team.teamName", "jerseyNumber");

    private final PlayerRepository playerRepository;
    private final NationalTeamService teamService;
    private final LineupRepository lineupRepository;
    private final MatchEventRepository eventRepository;
    private final PhotoStorageService photoStorageService;

    public PlayerService(PlayerRepository playerRepository, NationalTeamService teamService,
                         LineupRepository lineupRepository, MatchEventRepository eventRepository,
                         PhotoStorageService photoStorageService) {
        this.playerRepository = playerRepository;
        this.teamService = teamService;
        this.lineupRepository = lineupRepository;
        this.eventRepository = eventRepository;
        this.photoStorageService = photoStorageService;
    }

    public List<Player> search(PlayerSearchForm search, Long ownerId) {
        return playerRepository.findAll(PlayerSpecifications.matching(ownerId, search), SEARCH_ORDER);
    }

    public Player getOwned(Long id, Long ownerId) {
        return playerRepository.findByIdAndTeamOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("error.player.notFound"));
    }

    public Resource loadPhoto(Long id, Long ownerId) {
        Player player = getOwned(id, ownerId);
        if (player.getPhotoFileName() == null) {
            throw new ResourceNotFoundException("error.photo.notFound");
        }
        return photoStorageService.load(player.getPhotoFileName())
                .orElseThrow(() -> new ResourceNotFoundException("error.photo.notFound"));
    }

    /** Reprezentacija s punim spiskom ne prima nove igrače; mjesto se oslobađa brisanjem igrača. */
    public NationalTeam requireFreeSquadPlace(Long teamId, Long ownerId) {
        NationalTeam team = teamService.getOwned(teamId, ownerId);
        if (playerRepository.countByTeamId(teamId) >= MAX_PLAYERS_PER_TEAM) {
            throw new BusinessRuleException("error.player.limit", MAX_PLAYERS_PER_TEAM);
        }
        return team;
    }

    public Set<Long> findFullTeamIds(Long ownerId) {
        return Set.copyOf(playerRepository.findTeamIdsWithAtLeast(ownerId, MAX_PLAYERS_PER_TEAM));
    }

    @Transactional
    public Player create(Long teamId, PlayerForm form, Long ownerId) {
        NationalTeam team = requireFreeSquadPlace(teamId, ownerId);
        if (playerRepository.existsByTeamIdAndJerseyNumber(teamId, form.getJerseyNumber())) {
            throw jerseyTaken(form);
        }
        Player player = new Player(team);
        applyForm(player, form);
        return playerRepository.save(player);
    }

    @Transactional
    public void update(Long id, PlayerForm form, Long ownerId) {
        Player player = getOwned(id, ownerId);
        if (playerRepository.existsByTeamIdAndJerseyNumberAndIdNot(player.getTeam().getId(), form.getJerseyNumber(), id)) {
            throw jerseyTaken(form);
        }
        applyForm(player, form);
    }

    /** Igrač koji je dio sastava ili događaja se ne briše, jer bi se time promijenila historija utakmica. */
    @Transactional
    public Long delete(Long id, Long ownerId) {
        Player player = getOwned(id, ownerId);
        if (lineupRepository.isPlayerInAnyLineup(id) || lineupRepository.existsByCaptainId(id)
                || eventRepository.existsByPlayerId(id)) {
            throw new BusinessRuleException("error.player.inUse");
        }
        Long teamId = player.getTeam().getId();
        String photo = player.getPhotoFileName();
        playerRepository.delete(player);
        photoStorageService.delete(photo);
        return teamId;
    }

    private void applyForm(Player player, PlayerForm form) {
        player.updateDetails(form.getFirstName(), form.getLastName(), form.getBirthDate(), form.getNationality(),
                form.getPosition(), form.getJerseyNumber(), form.getHeightCm(), form.getClub());
        player.setPhotoFileName(photoStorageService.replace(player.getPhotoFileName(), form.getPhoto(), form.isRemovePhoto()));
    }

    private BusinessRuleException jerseyTaken(PlayerForm form) {
        return new BusinessRuleException("error.player.jerseyTaken", form.getJerseyNumber());
    }
}
