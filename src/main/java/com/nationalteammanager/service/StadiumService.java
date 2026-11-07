package com.nationalteammanager.service;

import com.nationalteammanager.dto.StadiumForm;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.MatchRepository;
import com.nationalteammanager.repository.StadiumRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final MatchRepository matchRepository;
    private final NationalTeamService teamService;
    private final PhotoStorageService photoStorageService;

    public StadiumService(StadiumRepository stadiumRepository, MatchRepository matchRepository,
                          NationalTeamService teamService, PhotoStorageService photoStorageService) {
        this.stadiumRepository = stadiumRepository;
        this.matchRepository = matchRepository;
        this.teamService = teamService;
        this.photoStorageService = photoStorageService;
    }

    public List<Stadium> findAll(Long ownerId) {
        return stadiumRepository.findByTeamOwnerIdOrderByTeamTeamNameAscMainStadiumDescNameAsc(ownerId);
    }

    /** Filter po reprezentaciji; tuđa reprezentacija daje 404. */
    public List<Stadium> findByTeam(Long teamId, Long ownerId) {
        teamService.getOwned(teamId, ownerId);
        return stadiumRepository.findByTeamIdOrderByMainStadiumDescNameAsc(teamId);
    }

    public Stadium getOwned(Long id, Long ownerId) {
        return stadiumRepository.findByIdAndTeamOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("error.stadium.notFound"));
    }

    public Resource loadPhoto(Long id, Long ownerId) {
        String fileName = getOwned(id, ownerId).getPhotoFileName();
        if (fileName == null) {
            throw new ResourceNotFoundException("error.photo.notFound");
        }
        return photoStorageService.load(fileName)
                .orElseThrow(() -> new ResourceNotFoundException("error.photo.notFound"));
    }

    @Transactional
    public Stadium create(StadiumForm form, Long ownerId) {
        NationalTeam team = teamService.getOwned(form.getTeamId(), ownerId);
        validate(form);
        if (stadiumRepository.existsByTeamIdAndNameIgnoreCase(team.getId(), form.getName())) {
            throw new BusinessRuleException("error.stadium.nameTaken");
        }
        Stadium stadium = new Stadium(team);
        applyForm(stadium, form, team);
        return stadiumRepository.save(stadium);
    }

    @Transactional
    public void update(Long id, StadiumForm form, Long ownerId) {
        Stadium stadium = getOwned(id, ownerId);
        NationalTeam team = teamService.getOwned(form.getTeamId(), ownerId);
        validate(form);
        if (stadiumRepository.existsByTeamIdAndNameIgnoreCaseAndIdNot(team.getId(), form.getName(), id)) {
            throw new BusinessRuleException("error.stadium.nameTaken");
        }
        if (!stadium.belongsTo(team) && matchRepository.existsByVenueId(id)) {
            throw new BusinessRuleException("error.stadium.teamLocked");
        }
        applyForm(stadium, form, team);
    }

    /** Stadion na kojem je zakazana ili odigrana utakmica ostaje, da se ne izgubi podatak o toj utakmici. */
    @Transactional
    public Long delete(Long id, Long ownerId) {
        Stadium stadium = getOwned(id, ownerId);
        if (matchRepository.existsByVenueId(id)) {
            throw new BusinessRuleException("error.stadium.hasMatches");
        }
        Long teamId = stadium.getTeam().getId();
        String photo = stadium.getPhotoFileName();
        stadiumRepository.delete(stadium);
        photoStorageService.delete(photo);
        return teamId;
    }

    private void validate(StadiumForm form) {
        if (form.getBuiltYear() > Year.now().getValue()) {
            throw new BusinessRuleException("error.stadium.builtFuture");
        }
    }

    private void applyForm(Stadium stadium, StadiumForm form, NationalTeam team) {
        if (form.isMainStadium()) {
            clearCurrentMainStadium(team, stadium);
        }
        stadium.updateDetails(team, form.getName(), form.getCity(), form.getCountry(), form.getCapacity(),
                form.getBuiltYear(), form.getSurface(), form.getAddress(), form.getDescription());
        stadium.setMainStadium(form.isMainStadium());
        stadium.setPhotoFileName(photoStorageService.replace(stadium.getPhotoFileName(), form.getPhoto(), form.isRemovePhoto()));
    }

    /** Odmah se upisuje u bazu, jer jedinstveni indeks ne dozvoljava ni trenutak s dva glavna stadiona. */
    private void clearCurrentMainStadium(NationalTeam team, Stadium newMain) {
        stadiumRepository.findByTeamIdAndMainStadiumTrue(team.getId())
                .filter(current -> !current.getId().equals(newMain.getId()))
                .ifPresent(current -> {
                    current.setMainStadium(false);
                    stadiumRepository.saveAndFlush(current);
                });
    }
}
