package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.CompetitionForm;
import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.enums.CompetitionType;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.CompetitionLogoStorage;
import com.nationalteammanager.service.CompetitionService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/competitions")
public class CompetitionController {

    private static final String FORM_VIEW = "competitions/form";

    private final CompetitionService competitionService;
    private final CompetitionLogoStorage logoStorage;

    public CompetitionController(CompetitionService competitionService, CompetitionLogoStorage logoStorage) {
        this.competitionService = competitionService;
        this.logoStorage = logoStorage;
    }

    @ModelAttribute("competitionTypes")
    public CompetitionType[] competitionTypes() {
        return CompetitionType.values();
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("competitions", competitionService.findAll(user.getId()));
        return "competitions/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("competition", competitionService.getVisible(id, user.getId()));
        model.addAttribute("matches", competitionService.findMatches(id, user.getId()));
        return "competitions/details";
    }

    /** Logo vidi svaki korisnik koji vidi takmičenje; SVG se servira u zaštićenom (sandbox) načinu. */
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> logo(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        Resource logo = competitionService.loadLogo(id, user.getId());
        return ResponseEntity.ok()
                .contentType(logoStorage.mediaTypeOf(logo.getFilename()))
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .body(logo);
    }

    @PostMapping("/{id}/logo/delete")
    public String deleteLogo(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                             RedirectAttributes redirectAttributes) {
        competitionService.removeLogo(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.logo.deleted");
        return "redirect:/competitions/" + id;
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        CompetitionForm form = new CompetitionForm();
        form.setShared(user.isAdmin());
        model.addAttribute("competition", form);
        model.addAttribute("formAction", "/competitions");
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("competition") CompetitionForm form, BindingResult binding,
                         @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        model.addAttribute("formAction", "/competitions");
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            Competition competition = competitionService.create(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.competition.created");
            return "redirect:/competitions/" + competition.getId();
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        Competition competition = competitionService.getManageable(id, user.getId());
        model.addAttribute("competition", CompetitionForm.from(competition));
        addEditAttributes(model, competition);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("competition") CompetitionForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addEditAttributes(model, competitionService.getManageable(id, user.getId()));
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            competitionService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.competition.updated");
            return "redirect:/competitions/" + id;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    private void addEditAttributes(Model model, Competition competition) {
        model.addAttribute("formAction", "/competitions/" + competition.getId());
        model.addAttribute("competitionId", competition.getId());
        model.addAttribute("currentLogo", competition);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                         RedirectAttributes redirectAttributes) {
        competitionService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.competition.deleted");
        return "redirect:/competitions";
    }
}
