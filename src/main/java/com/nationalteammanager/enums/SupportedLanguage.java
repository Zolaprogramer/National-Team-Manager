package com.nationalteammanager.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SupportedLanguage {
    SR("sr"),
    EN("en");

    public static final SupportedLanguage DEFAULT = SR;

    private final String code;

    SupportedLanguage(String code) {
        this.code = code;
    }

    public static Optional<SupportedLanguage> fromCode(String code) {
        return Arrays.stream(values()).filter(language -> language.code.equalsIgnoreCase(code)).findFirst();
    }

    public String getCode() {
        return code;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }
}
