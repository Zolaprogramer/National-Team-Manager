package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final String REGISTER_VIEW = "auth/register";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal AppUserPrincipal user) {
        return user != null ? "redirect:/dashboard" : "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registration", new RegistrationForm());
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationForm form, BindingResult binding,
                           RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return REGISTER_VIEW;
        }
        try {
            userService.register(form);
        } catch (BusinessRuleException exception) {
            binding.reject(exception.getCode(), exception.getArguments(), null);
            return REGISTER_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "success.account.created");
        return "redirect:/login";
    }
}
