package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.TacticsForm;
import com.nationalteammanager.enums.AttackDirection;
import com.nationalteammanager.enums.DefensiveLine;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.Mentality;
import com.nationalteammanager.enums.PassingStyle;
import com.nationalteammanager.enums.PlayStyle;
import com.nationalteammanager.enums.PlayTempo;
import com.nationalteammanager.enums.PlayWidth;
import com.nationalteammanager.enums.PressingIntensity;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.LineupService;
import com.nationalteammanager.service.TacticsService;
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
@RequestMapping("/matches/{matchId}/lineups/{teamId}/tactics")
public class TacticsController {

    private static final String BOARD_VIEW = "lineups/tactics";

    private final TacticsService tacticsService;

    public TacticsController(TacticsService tacticsService) {
        this.tacticsService = tacticsService;
    }

    @GetMapping
    public String board(@PathVariable Long matchId, @PathVariable Long teamId,
                        @AuthenticationPrincipal AppUserPrincipal user, Model model,
                        RedirectAttributes redirectAttributes) {
        Optional<TacticsForm> form = tacticsService.findForm(matchId, teamId, user.getId());
        if (form.isEmpty()) {
            redirectAttributes.addFlashAttribute("successMessage", "tactics.createLineupFirst");
            return "redirect:" + lineupPath(matchId, teamId) + "/edit";
        }
        model.addAttribute("tactics", form.get());
        addBoardAttributes(model, matchId, teamId, user);
        return BOARD_VIEW;
    }

    @PostMapping
    public String save(@PathVariable Long matchId, @PathVariable Long teamId,
                       @Valid @ModelAttribute("tactics") TacticsForm form, BindingResult binding,
                       @AuthenticationPrincipal AppUserPrincipal user, Model model,
                       RedirectAttributes redirectAttributes) {
        addBoardAttributes(model, matchId, teamId, user);
        if (binding.hasErrors()) {
            return BOARD_VIEW;
        }
        try {
            tacticsService.save(matchId, teamId, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.tactics.saved");
            return "redirect:" + lineupPath(matchId, teamId) + "/tactics";
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return BOARD_VIEW;
        }
    }

    @PostMapping("/restore")
    public String restore(@PathVariable Long matchId, @PathVariable Long teamId,
                          @AuthenticationPrincipal AppUserPrincipal user, RedirectAttributes redirectAttributes) {
        tacticsService.restorePrevious(matchId, teamId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.tactics.restored");
        return "redirect:" + lineupPath(matchId, teamId) + "/tactics";
    }

    private void addBoardAttributes(Model model, Long matchId, Long teamId, AppUserPrincipal user) {
        model.addAttribute("board", tacticsService.getBoard(matchId, teamId, user.getId()));
        model.addAttribute("formations", Formation.values());
        model.addAttribute("formationSlots", Formation.slotsByName());
        model.addAttribute("maxSubstitutes", LineupService.MAX_SUBSTITUTES);
        model.addAttribute("playStyles", PlayStyle.values());
        model.addAttribute("mentalities", Mentality.values());
        model.addAttribute("playWidths", PlayWidth.values());
        model.addAttribute("playTempos", PlayTempo.values());
        model.addAttribute("pressings", PressingIntensity.values());
        model.addAttribute("defensiveLines", DefensiveLine.values());
        model.addAttribute("passingStyles", PassingStyle.values());
        model.addAttribute("attackDirections", AttackDirection.values());
    }

    private String lineupPath(Long matchId, Long teamId) {
        return "/matches/" + matchId + "/lineups/" + teamId;
    }
}
