package com.nationalteammanager.config;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

/** Uzima samo putanju iz Referer zaglavlja, da preusmjerenje ne može odvesti na drugu stranicu. */
public final class RefererPaths {

    private static final String FALLBACK_PATH = "/dashboard";

    private RefererPaths() {
    }

    public static String pathOf(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return FALLBACK_PATH;
        }
        try {
            String path = URI.create(referer).getPath();
            return path == null || !path.startsWith("/") || path.startsWith("//") ? FALLBACK_PATH : path;
        } catch (IllegalArgumentException exception) {
            return FALLBACK_PATH;
        }
    }

    /** Putanja sa upitom, da promjena jezika ne izgubi filtere; poruke prijave (?error) se ne ponavljaju. */
    public static String pathWithQueryOf(HttpServletRequest request) {
        String path = pathOf(request);
        String referer = request.getHeader("Referer");
        if (referer == null || "/login".equals(path)) {
            return path;
        }
        try {
            String query = URI.create(referer).getRawQuery();
            return query == null ? path : path + "?" + query;
        } catch (IllegalArgumentException exception) {
            return path;
        }
    }
}
