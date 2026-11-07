package com.nationalteammanager;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.service.CountryFlagService;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CountryFlagTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(9000);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    private static final String LIBRARY = "/webjars/flag-icons/7.2.3/flags/4x3/";

    @Autowired private MockMvc mvc;
    @Autowired private CountryFlagService flagService;
    @Autowired private UserService userService;
    @Autowired private NationalTeamService teamService;
    @Autowired private MatchService matchService;

    private User owner;
    private AppUserPrincipal principal;

    @BeforeEach
    void createOwner() {
        int id = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Flag Tester");
        form.setUsername("flag" + id);
        form.setEmail("flag" + id + "@example.com");
        form.setPassword("Lozinka123");
        form.setConfirmPassword("Lozinka123");
        owner = userService.register(form);
        principal = new AppUserPrincipal(owner);
    }

    @Test
    void recognizesCountriesInSeveralLanguagesAndSpellings() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("Srbija", "rs"), Map.entry("Serbia", "rs"), Map.entry("SRBIJA", "rs"),
                Map.entry("Bosna i Hercegovina", "ba"), Map.entry("Bosnia & Herzegovina", "ba"), Map.entry("BiH", "ba"),
                Map.entry("Hrvatska", "hr"), Map.entry("Croatia", "hr"),
                Map.entry("Njemačka", "de"), Map.entry("Nemačka", "de"), Map.entry("njemacka", "de"), Map.entry("Germany", "de"),
                Map.entry("Brazil", "br"), Map.entry("  Brazil  ", "br"),
                Map.entry("Crna Gora", "me"), Map.entry("Engleska", "gb-eng"), Map.entry("Škotska", "gb-sct"),
                Map.entry("Kosovo", "xk"), Map.entry("Francuska", "fr"), Map.entry("Argentina", "ar"), Map.entry("Japan", "jp"));
        expected.forEach((name, code) -> assertThat(flagService.findCode(name)).as(name).contains(code));

        assertThat(flagService.findCode("Atlantida")).isEmpty();
        assertThat(flagService.findCode("")).isEmpty();
        assertThat(flagService.findCode(null)).isEmpty();
    }

    @Test
    void flagFilesAreServedLocally() throws Exception {
        for (String code : new String[]{"rs", "ba", "hr", "de", "br", "gb-eng"}) {
            mvc.perform(get(LIBRARY + code + ".svg")).andExpect(status().isOk());
        }
        mvc.perform(get(CountryFlagService.DEFAULT_FLAG_URL)).andExpect(status().isOk());
    }

    @Test
    void previewFollowsTypedCountry() throws Exception {
        mvc.perform(get("/teams/flag-preview").param("country", "Njemačka").with(user(principal)))
                .andExpect(jsonPath("$.recognized").value(true))
                .andExpect(jsonPath("$.url").value(LIBRARY + "de.svg"));
        mvc.perform(get("/teams/flag-preview").param("country", "Atlantida").with(user(principal)))
                .andExpect(jsonPath("$.recognized").value(false))
                .andExpect(jsonPath("$.url").value(CountryFlagService.DEFAULT_FLAG_URL));
        mvc.perform(get("/teams/flag-preview").param("country", "Brazil")).andExpect(status().is3xxRedirection());
    }

    @Test
    void countrySuggestionsFollowLanguage() {
        assertThat(flagService.countryNames(Locale.forLanguageTag("sr"))).contains("Srbija", "Hrvatska", "Brazil");
        assertThat(flagService.countryNames(Locale.ENGLISH)).contains("Serbia", "Croatia", "Germany");
    }

    @Test
    void automaticFlagIsShownOnEveryPage() throws Exception {
        NationalTeam serbia = createTeam("Srbija");
        NationalTeam brazil = createTeam("Brazil");
        Match match = createMatch(serbia, brazil);
        String rs = LIBRARY + "rs.svg";
        String br = LIBRARY + "br.svg";

        assertThat(page("/teams")).contains(rs, br);
        assertThat(page("/teams/" + serbia.getId())).contains(rs);
        assertThat(page("/dashboard")).contains(rs, br);
        assertThat(page("/matches")).contains(rs, br);
        assertThat(page("/matches/" + match.getId())).contains(rs, br);
    }

    @Test
    void customFlagCanBeUploadedChangedAndRemovedAndIsPrivate() throws Exception {
        NationalTeam team = createTeam("Atlantida");
        String url = "/teams/" + team.getId() + "/flag";
        String details = "/teams/" + team.getId();

        // Nepoznata država dobija neutralnu zastavu
        assertThat(page(details)).contains(CountryFlagService.DEFAULT_FLAG_URL);
        mvc.perform(get(url).with(user(principal))).andExpect(status().isNotFound());

        mvc.perform(flagRequest(team, "Atlantida", new MockMultipartFile("flag", "f.svg", "image/svg+xml", "<svg/>".getBytes()), null))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JPG, PNG ili WEBP")));

        mvc.perform(flagRequest(team, "Atlantida", new MockMultipartFile("flag", "f.png", "image/png", PNG), "KEEP"))
                .andExpect(redirectedUrl(details));
        assertThat(page(details)).contains(url);
        mvc.perform(get(url).with(user(principal))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));

        String firstFile = stored(team).getFlagFileName();
        mvc.perform(flagRequest(team, "Atlantida", new MockMultipartFile("flag", "g.png", "image/png", PNG), "KEEP"));
        assertThat(stored(team).getFlagFileName()).isNotEqualTo(firstFile).isNotNull();

        // Izmjena drugih podataka ne dira postojeću zastavu
        mvc.perform(flagRequest(team, "Atlantida", emptyFile(), "KEEP"));
        assertThat(stored(team).getFlagFileName()).isNotNull();

        createOwner();
        mvc.perform(get(url).with(user(principal))).andExpect(status().isNotFound());
    }

    @Test
    void flagCanBeRemovedAndAutomaticFlagRestored() throws Exception {
        NationalTeam team = createTeam("Hrvatska");
        String details = "/teams/" + team.getId();
        String hr = LIBRARY + "hr.svg";
        assertThat(page(details)).contains(hr);

        mvc.perform(flagRequest(team, "Hrvatska", new MockMultipartFile("flag", "f.png", "image/png", PNG), "KEEP"));
        assertThat(page(details)).doesNotContain(hr);

        mvc.perform(flagRequest(team, "Hrvatska", emptyFile(), "NONE")).andExpect(redirectedUrl(details));
        assertThat(stored(team).getFlagFileName()).isNull();
        assertThat(page(details)).contains(CountryFlagService.DEFAULT_FLAG_URL).doesNotContain(hr);

        // Promjena države ne vraća zastavu dok je korisnik sam ne vrati
        mvc.perform(flagRequest(team, "Hrvatska", emptyFile(), "KEEP"));
        assertThat(page(details)).doesNotContain(hr);

        mvc.perform(flagRequest(team, "Hrvatska", emptyFile(), "AUTOMATIC"));
        assertThat(page(details)).contains(hr);
    }

    private String page(String url) throws Exception {
        return mvc.perform(get(url).with(user(principal))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private NationalTeam stored(NationalTeam team) {
        return teamService.getOwned(team.getId(), owner.getId());
    }

    private MockMultipartFile emptyFile() {
        return new MockMultipartFile("flag", "", "application/octet-stream", new byte[0]);
    }

    private MockHttpServletRequestBuilder flagRequest(NationalTeam team, String country, MockMultipartFile file, String choice) {
        var request = multipart("/teams/" + team.getId()).file(file).with(user(principal)).with(csrf())
                .param("countryName", country).param("teamName", team.getTeamName())
                .param("confederation", "UEFA").param("coach", team.getCoach()).param("foundedYear", "1950");
        if (choice != null) {
            request.param("flagChoice", choice);
        }
        return request;
    }

    private NationalTeam createTeam(String country) {
        NationalTeamForm form = new NationalTeamForm();
        form.setCountryName(country);
        form.setTeamName("Team" + SEQUENCE.incrementAndGet());
        form.setConfederation(Confederation.UEFA);
        form.setCoach("Coach");
        form.setFoundedYear(1950);
        return teamService.create(form, owner.getId());
    }

    private Match createMatch(NationalTeam home, NationalTeam away) {
        MatchForm form = new MatchForm();
        form.setHomeTeamId(home.getId());
        form.setAwayTeamId(away.getId());
        form.setKickoff(LocalDateTime.now().plusDays(50));
        form.setStadium("Stadium");
        form.setStatus(MatchStatus.SCHEDULED);
        return matchService.create(form, owner.getId());
    }
}
