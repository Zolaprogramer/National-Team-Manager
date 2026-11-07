package com.nationalteammanager.config;

import com.nationalteammanager.enums.SupportedLanguage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;

/** Nakon prijave vraća jezik koji je korisnik posljednji izabrao, i na drugom pregledniku. */
@Component
public class LanguageLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LocaleResolver localeResolver;

    public LanguageLoginSuccessHandler(LocaleResolver localeResolver) {
        super("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
        this.localeResolver = localeResolver;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            SupportedLanguage.fromCode(principal.getPreferredLanguage()).ifPresent(
                    language -> localeResolver.setLocale(request, response, language.toLocale()));
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
