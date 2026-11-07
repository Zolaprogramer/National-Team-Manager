package com.nationalteammanager.config;

import com.nationalteammanager.enums.SupportedLanguage;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class I18nConfig {

    private static final String LANGUAGE_COOKIE = "ntm_language";

    /** Tekstovi se nalaze u messages_sr.properties i messages_en.properties. */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(SupportedLanguage.DEFAULT.toLocale());
        return messageSource;
    }

    /** Jezik trenutnog preglednika se čuva u kolačiću; prijavljenom korisniku se čuva i u bazi. */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(LANGUAGE_COOKIE);
        resolver.setDefaultLocale(SupportedLanguage.DEFAULT.toLocale());
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }
}
