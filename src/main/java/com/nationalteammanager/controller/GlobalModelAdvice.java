package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.RegistrationForm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Podaci i podešavanja zajednički za sve kontrolere. */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentUser")
    public AppUserPrincipal currentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        return principal;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /** Uklanja razmake oko unosa i prazno polje pretvara u null. Lozinke moraju ostati netaknute. */
    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        if (!(binder.getTarget() instanceof RegistrationForm)) {
            binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        }
    }
}
