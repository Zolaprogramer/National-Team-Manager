package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.MatchEventForm;
import com.nationalteammanager.dto.MatchFilterForm;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.MatchResultForm;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.enums.EventType;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.CompetitionService;
import com.nationalteammanager.service.MatchEventService;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.StadiumService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
@RequestMapping("/matches")
public class MatchController {

    private static final String FORM_VIEW = "matches/form";

    private final MatchService matchService;
    private final MatchEventService eventService;
    private final NationalTeamService teamService;
    private final CompetitionService competitionService;
    private final MessageSource messageSource;
    private final StadiumService stadiumService;

    public MatchController(MatchService matchService, MatchEventService eventService,
                           NationalTeamService teamService, CompetitionService competitionService,
                           MessageSource messageSource, StadiumService stadiumService) {
        this.stadiumService = stadiumService;
        this.messageSource = messageSource;
        this.matchService = matchService;
        this.eventService = eventService;
        this.teamService = teamService;
        this.competitionService = competitionService;
    }

    @ModelAttribute("statuses")
    public MatchStatus[] statuses() {
        return MatchStatus.values();
    }

    /** Neispravna vrijednost filtera se ignoriše, pa BindingResult ostaje nekorišten. */
    @GetMapping
    public String list(@ModelAttribute("filter") MatchFilterForm filter, BindingResult ignoredErrors,
                       @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("matches", matchService.find(filter, user.getId()));
        model.addAttribute("teams", teamService.findAll(user.getId()));
        return "matches/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("details", matchService.getDetails(id, user.getId()));
        model.addAttribute("eventTypes", EventType.values());
        return "matches/details";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("match", new MatchForm());
        addFormAttributes(model, user, "/matches", "/matches");
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("match") MatchForm form, BindingResult binding,
                         @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addFormAttributes(model, user, "/matches", "/matches");
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            Match match = matchService.create(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.match.created");
            return "redirect:/matches/" + match.getId();
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("match", MatchForm.from(matchService.getOwned(id, user.getId())));
        addFormAttributes(model, user, "/matches/" + id, "/matches/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("match") MatchForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addFormAttributes(model, user, "/matches/" + id, "/matches/" + id);
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            matchService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.match.updated");
            return "redirect:/matches/" + id;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                         RedirectAttributes redirectAttributes) {
        matchService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.match.deleted");
        return "redirect:/matches";
    }

    @PostMapping("/{id}/result")
    public String recordResult(@PathVariable Long id, @Valid @ModelAttribute MatchResultForm form,
                               BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user,
                               RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(binding));
        } else {
            matchService.recordResult(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.result.saved");
        }
        return "redirect:/matches/" + id;
    }

    @PostMapping("/{id}/events")
    public String addEvent(@PathVariable Long id, @Valid @ModelAttribute MatchEventForm form,
                           BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user,
                           RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstError(binding));
        } else {
            eventService.add(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.event.created");
        }
        return "redirect:/matches/" + id;
    }

    @PostMapping("/{id}/events/{eventId}/delete")
    public String deleteEvent(@PathVariable Long id, @PathVariable Long eventId,
                              @AuthenticationPrincipal AppUserPrincipal user,
                              RedirectAttributes redirectAttributes) {
        eventService.delete(id, eventId, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.event.deleted");
        return "redirect:/matches/" + id;
    }

    private void addFormAttributes(Model model, AppUserPrincipal user, String formAction, String cancelUrl) {
        model.addAttribute("teams", teamService.findAll(user.getId()));
        model.addAttribute("competitions", competitionService.findAll(user.getId()));
        model.addAttribute("stadiums", stadiumService.findAll(user.getId()));
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelUrl", cancelUrl);
    }

    private String firstError(BindingResult binding) {
        return messageSource.getMessage(binding.getAllErrors().get(0), LocaleContextHolder.getLocale());
    }
}
