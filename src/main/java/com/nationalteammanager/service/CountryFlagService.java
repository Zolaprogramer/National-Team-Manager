package com.nationalteammanager.service;

import com.nationalteammanager.dto.FlagPreview;
import com.nationalteammanager.entity.NationalTeam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Pronalazi zastavu po nazivu države. SVG fajlovi su u lokalnoj biblioteci flag-icons (WebJar),
 * a nazivi država dolaze iz Jave na više jezika, pa aplikacija ne koristi nijedan eksterni servis.
 */
@Service
public class CountryFlagService {

    public static final String DEFAULT_FLAG_URL = "/img/flag-default.svg";

    private static final String LIBRARY_PATH = "webjars/flag-icons/7.2.3/flags/4x3/";
    private static final List<Locale> NAME_LANGUAGES = Stream.of("en", "sr-Latn", "bs", "hr", "de")
            .map(Locale::forLanguageTag)
            .toList();

    /** Nazivi koje Java ne poznaje, među njima i fudbalske reprezentacije unutar Ujedinjenog Kraljevstva. */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("bih", "ba"),
            Map.entry("bosna", "ba"),
            Map.entry("bosnia", "ba"),
            Map.entry("srbija", "rs"),
            Map.entry("hrvatska", "hr"),
            Map.entry("njemacka", "de"),
            Map.entry("nemacka", "de"),
            Map.entry("brazil", "br"),
            Map.entry("crna gora", "me"),
            Map.entry("makedonija", "mk"),
            Map.entry("kosovo", "xk"),
            Map.entry("sad", "us"),
            Map.entry("usa", "us"),
            Map.entry("amerika", "us"),
            Map.entry("engleska", "gb-eng"),
            Map.entry("england", "gb-eng"),
            Map.entry("skotska", "gb-sct"),
            Map.entry("scotland", "gb-sct"),
            Map.entry("vels", "gb-wls"),
            Map.entry("wales", "gb-wls"),
            Map.entry("sjeverna irska", "gb-nir"),
            Map.entry("northern ireland", "gb-nir"));

    private final Map<String, String> codesByName = new HashMap<>();

    public CountryFlagService() {
        for (String country : Locale.getISOCountries()) {
            for (Locale language : NAME_LANGUAGES) {
                codesByName.put(normalize(new Locale.Builder().setRegion(country).build().getDisplayCountry(language)),
                        country.toLowerCase(Locale.ROOT));
            }
        }
        codesByName.putAll(ALIASES);
    }

    /** Oznaka države, samo ako za nju postoji fajl u biblioteci. */
    public Optional<String> findCode(String countryName) {
        if (countryName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(codesByName.get(normalize(countryName)))
                .filter(code -> new ClassPathResource("META-INF/resources/" + LIBRARY_PATH + code + ".svg").exists());
    }

    public FlagPreview preview(String countryName) {
        Optional<String> code = findCode(countryName);
        return new FlagPreview(code.isPresent(), code.map(this::libraryUrl).orElse(DEFAULT_FLAG_URL));
    }

    /** Redoslijed: vlastita slika, pa zastava države, pa neutralna zastava. */
    public String urlOf(NationalTeam team) {
        if (team.getFlagFileName() != null) {
            return "/teams/" + team.getId() + "/flag";
        }
        if (team.isFlagRemoved()) {
            return DEFAULT_FLAG_URL;
        }
        return findCode(team.getCountryName()).map(this::libraryUrl).orElse(DEFAULT_FLAG_URL);
    }

    /** Nazivi država na jeziku korisnika, kao prijedlozi u obrascu. */
    public List<String> countryNames(Locale locale) {
        Collator collator = Collator.getInstance(locale);
        return Stream.of(Locale.getISOCountries())
                .map(country -> new Locale.Builder().setRegion(country).build().getDisplayCountry(displayLocale(locale)))
                .sorted(collator)
                .toList();
    }

    private Locale displayLocale(Locale locale) {
        return "sr".equals(locale.getLanguage()) ? Locale.forLanguageTag("sr-Latn") : locale;
    }

    private String libraryUrl(String code) {
        return "/" + LIBRARY_PATH + code + ".svg";
    }

    /** Velika i mala slova, razmaci i dijakritici ne utiču na prepoznavanje države. */
    private String normalize(String name) {
        String withoutMarks = Normalizer.normalize(name.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutMarks.replace('&', ' ').trim().replaceAll("\\s+", " ");
    }
}
