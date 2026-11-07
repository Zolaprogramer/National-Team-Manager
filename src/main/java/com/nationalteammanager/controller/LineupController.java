package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.LineupForm;
import com.nationalteammanager.dto.LineupView;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.LineupService;
import jakarta.validation.Valid;
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

import java.util.Optional;

@Controller
@RequestMapping("/matches/{matchId}/lineups/{teamId}")
public class LineupController {

    private static final String FORM_VIEW = "lineups/form";

    private final LineupService lineupService;

    public LineupController(LineupService lineupService) {
        this.lineupService = lineupService;
    }

    @GetMapping
    public String view(@PathVariable Long matchId, @PathVariable Long teamId,
                       @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        Optional<LineupView> view = lineupService.findView(matchId, teamId, user.getId());
        if (view.isEmpty()) {
            return "redirect:/matches/" + matchId + "/lineups/" + teamId + "/edit";
        }
        model.addAttribute("view", view.get());
        model.addAttribute("editor", lineupService.getEditor(matchId, teamId, user.getId()));
        return "lineups/view";
    }

    @GetMapping("/edit")
    public String editForm(@PathVariable Long matchId, @PathVariable Long teamId,
                           @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("lineup", lineupService.getForm(matchId, teamId, user.getId()));
        addFormAttributes(model, matchId, teamId, user);
        return FORM_VIEW;
    }

    @PostMapping
    public String save(@PathVariable Long matchId, @PathVariable Long teamId,
                       @Valid @ModelAttribute("lineup") LineupForm form, BindingResult binding,
                       @AuthenticationPrincipal AppUserPrincipal user, Model model,
                       RedirectAttributes redirectAttributes) {
        addFormAttributes(model, matchId, teamId, user);
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            lineupService.save(matchId, teamId, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.lineup.saved");
            return "redirect:/matches/" + matchId + "/lineups/" + teamId;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @PostMapping("/delete")
    public String delete(@PathVariable Long matchId, @PathVariable Long teamId,
                         @AuthenticationPrincipal AppUserPrincipal user, RedirectAttributes redirectAttributes) {
        lineupService.delete(matchId, teamId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.lineup.deleted");
        return "redirect:/matches/" + matchId;
    }

    private void addFormAttributes(Model model, Long matchId, Long teamId, AppUserPrincipal user) {
        model.addAttribute("editor", lineupService.getEditor(matchId, teamId, user.getId()));
        model.addAttribute("formations", Formation.values());
        model.addAttribute("formationSlots", Formation.slotsByName());
        model.addAttribute("maxSubstitutes", LineupService.MAX_SUBSTITUTES);
    }
}
