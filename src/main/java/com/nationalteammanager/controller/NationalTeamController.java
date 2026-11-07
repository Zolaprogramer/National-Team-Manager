package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.FlagPreview;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.FlagChoice;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.CountryFlagService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/teams")
public class NationalTeamController {

    private static final String FORM_VIEW = "teams/form";

    private final NationalTeamService teamService;
    private final PhotoStorageService photoStorageService;
    private final CountryFlagService flagService;

    public NationalTeamController(NationalTeamService teamService, PhotoStorageService photoStorageService,
                                  CountryFlagService flagService) {
        this.teamService = teamService;
        this.photoStorageService = photoStorageService;
        this.flagService = flagService;
    }

    @ModelAttribute("confederations")
    public Confederation[] confederations() {
        return Confederation.values();
    }

    @ModelAttribute("flagChoices")
    public FlagChoice[] flagChoices() {
        return FlagChoice.values();
    }

    @ModelAttribute("countryNames")
    public List<String> countryNames(Locale locale) {
        return flagService.countryNames(locale);
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("summaries", teamService.findSummaries(user.getId()));
        return "teams/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("details", teamService.getDetails(id, user.getId()));
        model.addAttribute("maxPlayers", PlayerService.MAX_PLAYERS_PER_TEAM);
        return "teams/details";
    }

    @GetMapping("/{id}/coach-photo")
    public ResponseEntity<Resource> coachPhoto(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        Resource photo = teamService.loadCoachPhoto(id, user.getId());
        return ResponseEntity.ok()
                .contentType(photoStorageService.mediaTypeOf(photo.getFilename()))
                .body(photo);
    }

    @GetMapping("/{id}/flag")
    public ResponseEntity<Resource> flag(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        Resource flag = teamService.loadFlag(id, user.getId());
        return ResponseEntity.ok()
                .contentType(photoStorageService.mediaTypeOf(flag.getFilename()))
                .body(flag);
    }

    @GetMapping("/flag-preview")
    @ResponseBody
    public FlagPreview flagPreview(@RequestParam(required = false) String country) {
        return flagService.preview(country);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("team", new NationalTeamForm());
        model.addAttribute("formAction", "/teams");
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("team") NationalTeamForm form, BindingResult binding,
                         @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        model.addAttribute("formAction", "/teams");
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            NationalTeam team = teamService.create(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.team.created");
            return "redirect:/teams/" + team.getId();
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("team", NationalTeamForm.from(teamService.getOwned(id, user.getId())));
        addEditAttributes(model, id, user);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("team") NationalTeamForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addEditAttributes(model, id, user);
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            teamService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.team.updated");
            return "redirect:/teams/" + id;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    private void addEditAttributes(Model model, Long id, AppUserPrincipal user) {
        NationalTeam team = teamService.getOwned(id, user.getId());
        model.addAttribute("formAction", "/teams/" + id);
        model.addAttribute("cancelUrl", "/teams/" + id);
        model.addAttribute("teamId", id);
        model.addAttribute("hasCoachPhoto", team.getCoachPhotoFileName() != null);
        model.addAttribute("hasCustomFlag", team.getFlagFileName() != null);
        model.addAttribute("currentFlagUrl", flagService.urlOf(team));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                         RedirectAttributes redirectAttributes) {
        teamService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.team.deleted");
        return "redirect:/teams";
    }
}
