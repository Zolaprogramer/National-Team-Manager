package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.StadiumForm;
import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.enums.StadiumSurface;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.PhotoStorageService;
import com.nationalteammanager.service.StadiumService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stadiums")
public class StadiumController {

    private static final String FORM_VIEW = "stadiums/form";

    private final StadiumService stadiumService;
    private final NationalTeamService teamService;
    private final PhotoStorageService photoStorageService;

    public StadiumController(StadiumService stadiumService, NationalTeamService teamService,
                             PhotoStorageService photoStorageService) {
        this.stadiumService = stadiumService;
        this.teamService = teamService;
        this.photoStorageService = photoStorageService;
    }

    @ModelAttribute("surfaces")
    public StadiumSurface[] surfaces() {
        return StadiumSurface.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long teamId, @AuthenticationPrincipal AppUserPrincipal user,
                       Model model) {
        model.addAttribute("stadiums", teamId == null
                ? stadiumService.findAll(user.getId()) : stadiumService.findByTeam(teamId, user.getId()));
        model.addAttribute("teams", teamService.findAll(user.getId()));
        model.addAttribute("selectedTeamId", teamId);
        return "stadiums/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("stadium", stadiumService.getOwned(id, user.getId()));
        return "stadiums/details";
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        Resource photo = stadiumService.loadPhoto(id, user.getId());
        return ResponseEntity.ok()
                .contentType(photoStorageService.mediaTypeOf(photo.getFilename()))
                .body(photo);
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long teamId, @AuthenticationPrincipal AppUserPrincipal user,
                          Model model) {
        StadiumForm form = new StadiumForm();
        form.setTeamId(teamId);
        model.addAttribute("stadium", form);
        addFormAttributes(model, user, "/stadiums", "/stadiums");
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("stadium") StadiumForm form, BindingResult binding,
                         @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addFormAttributes(model, user, "/stadiums", "/stadiums");
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            Stadium stadium = stadiumService.create(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.stadium.created");
            return "redirect:/stadiums/" + stadium.getId();
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("stadium", StadiumForm.from(stadiumService.getOwned(id, user.getId())));
        addEditAttributes(model, id, user);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("stadium") StadiumForm form,
                         BindingResult binding, @AuthenticationPrincipal AppUserPrincipal user, Model model,
                         RedirectAttributes redirectAttributes) {
        addEditAttributes(model, id, user);
        if (binding.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            stadiumService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "success.stadium.updated");
            return "redirect:/stadiums/" + id;
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return FORM_VIEW;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user,
                         RedirectAttributes redirectAttributes) {
        stadiumService.delete(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "success.stadium.deleted");
        return "redirect:/stadiums";
    }

    private void addFormAttributes(Model model, AppUserPrincipal user, String formAction, String cancelUrl) {
        model.addAttribute("teams", teamService.findAll(user.getId()));
        model.addAttribute("formAction", formAction);
        model.addAttribute("cancelUrl", cancelUrl);
    }

    private void addEditAttributes(Model model, Long id, AppUserPrincipal user) {
        Stadium stadium = stadiumService.getOwned(id, user.getId());
        addFormAttributes(model, user, "/stadiums/" + id, "/stadiums/" + id);
        model.addAttribute("stadiumId", id);
        model.addAttribute("hasPhoto", stadium.getPhotoFileName() != null);
    }
}
