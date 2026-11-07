package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.PlayerForm;
import com.nationalteammanager.dto.PlayerSearchForm;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.PlayerPosition;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.PhotoStorageService;
import com.nationalteammanager.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlayerController {

    private static final String FORM_VIEW = "players/form";

    private final PlayerService playerService;
    private final NationalTeamService teamService;
    private final PhotoStorageService photoStorageService;

    public PlayerController(PlayerService playerService, NationalTeamService teamService,
                            PhotoStorageService photoStorageService) {
        this.playerService = playerService;
        this.teamService = teamService;
        this.photoStorageService = photoStorageService;
    }

    @ModelAttribute("positions")
    public PlayerPosition[] positions() {
        return PlayerPosition.values();
    }

    /** Neispravna vrijednost filtera (npr. tekst umjesto broja) se samo ignoriše, pa BindingResult ostaje nekorišten. */
    @GetMapping("/players")
    public String list(@ModelAttribute("search") PlayerSearchForm search, BindingResult ignoredErrors,
                       @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("players", playerService.search(search, user.getId()));
        model.addAttribute("teams", teamService.findAll(user.getId()));
        model.addAttribute("fullTeamIds", playerService.findFullTeamIds(user.getId()));
        model.addAttribute("maxPlayers", PlayerService.MAX_PLAYERS_PER_TEAM);
        return "players/list";
    }

    @GetMapping("/players/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("player", playerService.getOwned(id, user.getId()));
        return "players/details";
    }

    @GetMapping("/players/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        Resource photo = playerService.loadPhoto(id, user.getId());
        return ResponseEntity.ok()
                .contentType(photoStorageService.mediaTypeOf(photo.getFilename()))
                .body(photo);
    }

    @GetMapping("/teams/{teamId}/players/new")
    public String newForm(@PathVariable Long teamId, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        NationalTeam team = playerService.requireFreeSquadPlace(teamId, user.getId());
        PlayerForm form = new PlayerForm();
        form.setNationality(team.getCountryName());
        model.addAttribute("player", form);
        addCreateFormAttributes(model, team);
        return FORM_VIEW;
    }

    @PostMapping("/teams/{teamId}/players")
    public String create(@PathVariable Long teamId, @Valid @ModelAttribute("player") PlayerForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addCreateFormAttributes(model, teamService.getOwned(teamId, user.getId()));
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            playerService.create(teamId, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.player.created");
            return "redirect:/teams/" + teamId;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/players/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        Player player = playerService.getOwned(id, user.getId());
        model.addAttribute("player", PlayerForm.from(player));
        addEditFormAttributes(model, player);
        return FORM_VIEW;
    }

    @PostMapping("/players/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("player") PlayerForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addEditFormAttributes(model, playerService.getOwned(id, user.getId()));
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            playerService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.player.updated");
            return "redirect:/players/" + id;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @PostMapping("/players/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                         RedirectAttributes redirectAttributes) {
        Long teamId = playerService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.player.deleted");
        return "redirect:/teams/" + teamId;
    }

    private void addCreateFormAttributes(Model model, NationalTeam team) {
        model.addAttribute("teamName", team.getTeamName());
        model.addAttribute("formAction", "/teams/" + team.getId() + "/players");
        model.addAttribute("cancelUrl", "/teams/" + team.getId());
        model.addAttribute("hasPhoto", false);
    }

    private void addEditFormAttributes(Model model, Player player) {
        model.addAttribute("teamName", player.getTeam().getTeamName());
        model.addAttribute("formAction", "/players/" + player.getId());
        model.addAttribute("cancelUrl", "/players/" + player.getId());
        model.addAttribute("hasPhoto", player.getPhotoFileName() != null);
        model.addAttribute("playerId", player.getId());
    }
}
