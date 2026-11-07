package com.nationalteammanager.service;

import com.nationalteammanager.dto.CompetitionForm;
import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.CompetitionRepository;
import com.nationalteammanager.repository.MatchRepository;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Takmičenje je ili lično (vidi ga i mijenja samo vlasnik) ili zajedničko, odnosno zvanično (vlasnik je null):
 * zajednička takmičenja vide svi korisnici, a mijenja ih samo administrator. Samo administrator postavlja logotipe.
 */
@Service
@Transactional(readOnly = true)
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;
    private final CompetitionLogoStorage logoStorage;

    public CompetitionService(CompetitionRepository competitionRepository, MatchRepository matchRepository,
                              UserService userService, CompetitionLogoStorage logoStorage) {
        this.competitionRepository = competitionRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.logoStorage = logoStorage;
    }

    /** Lična takmičenja korisnika i sva zajednička takmičenja. */
    public List<Competition> findAll(Long userId) {
        return competitionRepository.findVisible(userId);
    }

    public Competition getVisible(Long id, Long userId) {
        return competitionRepository.findVisible(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.competition.notFound"));
    }

    /** Zajedničko takmičenje mijenja samo administrator; tuđe lično takmičenje se ne vidi (404). */
    public Competition getManageable(Long id, Long userId) {
        Competition competition = getVisible(id, userId);
        if (competition.isShared() && !isAdmin(userId)) {
            throw new AccessDeniedException("error.forbidden");
        }
        return competition;
    }

    /** Korisnik vidi samo svoje utakmice, i u zajedničkom takmičenju. */
    public List<Match> findMatches(Long id, Long userId) {
        getVisible(id, userId);
        return matchRepository.findByCompetitionIdAndHomeTeamOwnerIdOrderByKickoffDesc(id, userId);
    }

    public Resource loadLogo(Long id, Long userId) {
        String logoPath = getVisible(id, userId).getLogoPath();
        if (logoPath == null) {
            throw new ResourceNotFoundException("error.photo.notFound");
        }
        return logoStorage.load(logoPath).orElseThrow(() -> new ResourceNotFoundException("error.photo.notFound"));
    }

    @Transactional
    public Competition create(CompetitionForm form, Long userId) {
        boolean admin = isAdmin(userId);
        requireAdminForLogo(form, admin);
        if (form.isShared() && !admin) {
            throw new AccessDeniedException("error.forbidden");
        }
        Competition competition = new Competition(form.isShared() ? null : userService.getById(userId));
        applyForm(competition, form);
        return competitionRepository.save(competition);
    }

    @Transactional
    public void update(Long id, CompetitionForm form, Long userId) {
        Competition competition = getManageable(id, userId);
        requireAdminForLogo(form, isAdmin(userId));
        boolean matchesOutside = matchRepository.existsOutsidePeriod(id, form.getStartDate().atStartOfDay(),
                form.getEndDate().plusDays(1).atStartOfDay());
        if (matchesOutside) {
            throw new BusinessRuleException("error.competition.matchesOutside");
        }
        applyForm(competition, form);
    }

    @Transactional
    public void removeLogo(Long id, Long userId) {
        Competition competition = getManageable(id, userId);
        if (!isAdmin(userId)) {
            throw new AccessDeniedException("error.forbidden");
        }
        logoStorage.delete(competition.getLogoPath());
        competition.setLogoPath(null);
    }

    /** Utakmice ostaju sačuvane i postaju prijateljske. */
    @Transactional
    public void delete(Long id, Long userId) {
        Competition competition = getManageable(id, userId);
        matchRepository.findByCompetitionId(id).forEach(Match::detachFromCompetition);
        String logoPath = competition.getLogoPath();
        competitionRepository.delete(competition);
        logoStorage.delete(logoPath);
    }

    public boolean isAdmin(Long userId) {
        return userService.getById(userId).isAdmin();
    }

    private void requireAdminForLogo(CompetitionForm form, boolean admin) {
        boolean changesLogo = (form.getLogo() != null && !form.getLogo().isEmpty()) || form.isRemoveLogo();
        if (changesLogo && !admin) {
            throw new AccessDeniedException("error.forbidden");
        }
    }

    /** Bez novog fajla i bez oznake za brisanje postojeći logo ostaje nepromijenjen. */
    private void applyForm(Competition competition, CompetitionForm form) {
        competition.updateDetails(form.getName(), form.getSeason(), form.getType(), form.getStartDate(), form.getEndDate());
        competition.setLogoPath(logoStorage.replace(competition.getLogoPath(), form.getLogo(), form.isRemoveLogo()));
    }
}
