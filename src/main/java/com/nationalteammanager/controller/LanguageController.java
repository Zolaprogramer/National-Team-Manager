package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.config.RefererPaths;
import com.nationalteammanager.enums.SupportedLanguage;
import com.nationalteammanager.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class LanguageController {

    private final LocaleResolver localeResolver;
    private final UserService userService;

    public LanguageController(LocaleResolver localeResolver, UserService userService) {
        this.localeResolver = localeResolver;
        this.userService = userService;
    }

    /** Jezik se postavlja u preglednik, a prijavljenom korisniku se čuva i u bazi. Nepoznat jezik se ignoriše. */
    @PostMapping("/language")
    public String change(@RequestParam("lang") String code, @AuthenticationPrincipal AppUserPrincipal user,
                         HttpServletRequest request, HttpServletResponse response) {
        SupportedLanguage.fromCode(code).ifPresent(language -> {
            localeResolver.setLocale(request, response, language.toLocale());
            if (user != null) {
                userService.changeLanguage(user.getId(), language);
            }
        });
        return "redirect:" + RefererPaths.pathWithQueryOf(request);
    }
}
