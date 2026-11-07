package com.nationalteammanager;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.LineupForm;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.PlayerForm;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.dto.TacticsForm;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.FormationSlot;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.enums.PlayerPosition;
import com.nationalteammanager.repository.UserRepository;
import com.nationalteammanager.service.LineupService;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.PlayerService;
import com.nationalteammanager.service.TacticsService;
import com.nationalteammanager.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Taktička tabla, fotografija selektora i izbor jezika, provjereni kroz stvarne HTTP zahtjeve. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TacticsAndLanguageTest {

    private static final String LANGUAGE_COOKIE = "ntm_language";
    private static final AtomicInteger SEQUENCE = new AtomicInteger(5000);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    private static final Pattern SERBIAN_LETTERS = Pattern.compile("[čćšđžČĆŠĐŽ]");
    private static final Pattern MESSAGE_KEY = Pattern.compile("#\\{([A-Za-z][\\w.]*)[(}]");

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private NationalTeamService teamService;
    @Autowired private PlayerService playerService;
    @Autowired private MatchService matchService;
    @Autowired private LineupService lineupService;
    @Autowired private TacticsService tacticsService;

    private User owner;
    private AppUserPrincipal principal;
    private NationalTeam team;
    private NationalTeam opponent;
    private Match match;
    private List<Player> squad;

    @BeforeEach
    void createLineup() {
        owner = registerUser();
        principal = new AppUserPrincipal(owner);
        team = createTeam(owner);
        opponent = createTeam(owner);
        squad = createSquad(team);
        match = createMatch(team, opponent);

        lineupService.save(match.getId(), team.getId(), startingLineup(), owner.getId());
    }

    private LineupForm startingLineup() {
        LineupForm form = new LineupForm();
        for (int slot = 0; slot < 11; slot++) {
            form.getStarterIds().set(slot, squad.get(slot).getId());
        }
        form.setCaptainId(squad.get(5).getId());
        form.setSubstituteIds(new ArrayList<>(List.of(squad.get(11).getId(), squad.get(12).getId())));
        return form;
    }

    // Formacije

    @Test
    void everyFormationHasElevenDistinctPositionsAndOneGoalkeeper() {
        assertThat(Formation.values()).extracting(Formation::getLabel)
                .containsExactlyInAnyOrder("4-3-3", "4-4-2", "4-2-3-1", "3-5-2", "3-4-3", "5-3-2");
        for (Formation formation : Formation.values()) {
            List<FormationSlot> slots = formation.getSlots();
            assertThat(slots).hasSize(11);
            assertThat(slots).filteredOn(slot -> slot.label().equals("GK")).hasSize(1);
            assertThat(slots.get(Formation.GOALKEEPER_SLOT).label()).isEqualTo("GK");
            assertThat(slots.stream().map(slot -> slot.x() + ":" + slot.y()).distinct()).hasSize(11);
        }
    }

    // Čuvanje i učitavanje taktike

    @Test
    void savedTacticsArePersistedAndDifferPerMatch() throws Exception {
        Long captain = squad.get(9).getId();
        Long taker = squad.get(10).getId();

        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_5_3_2", "HIGH_PRESS")
                        .param("captainId", captain.toString())
                        .param("penaltyTakerId", taker.toString())
                        .param("freeKickTakerId", captain.toString())
                        .param("cornerTakerId", taker.toString())
                        .param("duties[" + taker + "]", "OFFENSIVE"))
                .andExpect(redirectedUrl(tacticsUrl(match, team)));

        String board = page(tacticsUrl(match, team));
        assertThat(board).contains("value=\"F_5_3_2\" selected=\"selected\"");
        assertThat(board).contains("value=\"HIGH_PRESS\" selected=\"selected\"");
        assertThat(board).contains("\"" + taker + "\":\"OFFENSIVE\"");
        assertThat(page(lineupUrl(match, team))).contains("5-3-2", "Visoki presing");

        // Druga utakmica iste reprezentacije ima svoju taktiku
        Match other = createMatch(team, opponent);
        lineupService.save(other.getId(), team.getId(), startingLineup(), owner.getId());
        String otherBoard = page(tacticsUrl(other, team));
        assertThat(otherBoard).contains("value=\"F_4_3_3\" selected=\"selected\"");
        assertThat(otherBoard).contains("value=\"POSSESSION\" selected=\"selected\"");
    }

    @Test
    void swappingPlayersIsSavedAndBenchPlayerCanEnter() throws Exception {
        Long first = squad.get(1).getId();
        Long second = squad.get(9).getId();
        Long benchPlayer = squad.get(12).getId();
        Long replaced = squad.get(2).getId();

        MockHttpServletRequestBuilder request = tacticsRequest(validParams(tacticsPost(match, team)), "F_4_3_3")
                .param("captainId", squad.get(5).getId().toString());
        replaceParam(request, "starterIds[1]", second);
        replaceParam(request, "starterIds[9]", first);
        replaceParam(request, "starterIds[2]", benchPlayer);
        replaceSubstitutes(request, squad.get(11).getId(), replaced);

        mvc.perform(request).andExpect(redirectedUrl(tacticsUrl(match, team)));

        LineupForm saved = lineupService.getForm(match.getId(), team.getId(), owner.getId());
        assertThat(saved.getStarterIds().get(1)).isEqualTo(second);
        assertThat(saved.getStarterIds().get(9)).isEqualTo(first);
        assertThat(saved.getStarterIds().get(2)).isEqualTo(benchPlayer);
        assertThat(saved.getSubstituteIds()).contains(replaced).doesNotContain(benchPlayer);
    }

    @Test
    void previousLineupCanBeRestored() throws Exception {
        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_3_4_3", "DIRECT")
                        .param("captainId", squad.get(5).getId().toString()))
                .andExpect(redirectedUrl(tacticsUrl(match, team)));
        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_4_2_3_1", "DEFENSIVE")
                        .param("captainId", squad.get(5).getId().toString()))
                .andExpect(redirectedUrl(tacticsUrl(match, team)));

        mvc.perform(post(tacticsUrl(match, team) + "/restore").with(user(principal)).with(csrf()))
                .andExpect(redirectedUrl(tacticsUrl(match, team)));
        String restored = page(tacticsUrl(match, team));
        assertThat(restored).contains("value=\"F_3_4_3\" selected=\"selected\"", "value=\"DIRECT\" selected=\"selected\"");

        // Vraćanje se može i poništiti, jer je trenutno stanje postalo prethodno
        mvc.perform(post(tacticsUrl(match, team) + "/restore").with(user(principal)).with(csrf()));
        assertThat(page(tacticsUrl(match, team))).contains("value=\"F_4_2_3_1\" selected=\"selected\"");
    }

    @Test
    void normalLineupEditKeepsTacticalSettingsAndRolesOfRemainingPlayers() throws Exception {
        Long taker = squad.get(10).getId();
        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_4_3_3", "COUNTER_ATTACK")
                .param("captainId", squad.get(5).getId().toString())
                .param("penaltyTakerId", taker.toString())
                .param("cornerTakerId", squad.get(9).getId().toString())
                .param("duties[" + squad.get(9).getId() + "]", "DEFENSIVE"));

        LineupForm plain = lineupService.getForm(match.getId(), team.getId(), owner.getId());
        plain.getSubstituteIds().remove(squad.get(12).getId());
        plain.getStarterIds().set(10, squad.get(12).getId());
        lineupService.save(match.getId(), team.getId(), plain, owner.getId());

        String board = page(tacticsUrl(match, team));
        assertThat(board).contains("value=\"COUNTER_ATTACK\" selected=\"selected\"");
        assertThat(board).contains("\"" + squad.get(9).getId() + "\":\"DEFENSIVE\"");
        // Izvođač penala je izašao iz početnih 11, pa je njegova uloga uklonjena, a uloga izvođača kornera ostaje
        TacticsForm saved = tacticsService.findForm(match.getId(), team.getId(), owner.getId()).orElseThrow();
        assertThat(saved.getPenaltyTakerId()).isNull();
        assertThat(saved.getCornerTakerId()).isEqualTo(squad.get(9).getId());
    }

    // Validacije

    @Test
    void invalidBoardsAreRejectedAndNothingIsSaved() throws Exception {
        expectBoardError(params -> replaceParam(params, "starterIds[10]", null), "tačno 11");
        expectBoardError(params -> replaceParam(params, "starterIds[10]", squad.get(9).getId()), "dva puta");
        expectBoardError(params -> {
            replaceParam(params, "starterIds[0]", squad.get(1).getId());
            replaceParam(params, "starterIds[1]", squad.get(0).getId());
        }, "golman");
        expectBoardError(params -> replaceParam(params, "starterIds[10]", squad.get(11).getId()), "golman");
        expectBoardError(params -> replaceSubstitutes(params, squad.get(10).getId()), "dva puta");
        expectBoardError(params -> params.param("penaltyTakerId", squad.get(11).getId().toString()), "Izvođač prekida");
        expectBoardError(params -> params.param("duties[" + squad.get(13).getId() + "]", "OFFENSIVE"), "samo igraču iz sastava");

        Player foreign = createSquad(createTeam(owner)).get(1);
        expectBoardError(params -> replaceParam(params, "starterIds[10]", foreign.getId()), "pripadati");

        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_4_3_3")
                        .param("captainId", squad.get(11).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kapiten mora biti")));

        // Neispravan enum ne prolazi validaciju
        mvc.perform(tacticsRequest(validParams(tacticsPost(match, team)), "F_4_3_3", "NEPOSTOJECI")
                        .param("captainId", squad.get(5).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Unesena vrijednost nije ispravna")));

        assertThat(lineupService.getForm(match.getId(), team.getId(), owner.getId()).getFormation()).isEqualTo(Formation.F_4_3_3);
    }

    // Vlasništvo

    @Test
    void userCannotOpenOrChangeAnotherUsersTactics() throws Exception {
        AppUserPrincipal stranger = new AppUserPrincipal(registerUser());
        String url = tacticsUrl(match, team);

        mvc.perform(get(url).with(user(stranger))).andExpect(status().isNotFound());
        mvc.perform(post(url + "/restore").with(user(stranger)).with(csrf())).andExpect(status().isNotFound());

        MockHttpServletRequestBuilder request = validParams(post(url).with(user(stranger)).with(csrf()))
                .param("formation", "F_5_3_2").param("captainId", squad.get(5).getId().toString());
        mvc.perform(request).andExpect(status().isNotFound());
        assertThat(lineupService.getForm(match.getId(), team.getId(), owner.getId()).getFormation()).isEqualTo(Formation.F_4_3_3);

        // Reprezentacija koja ne igra utakmicu nema taktičku tablu
        NationalTeam third = createTeam(owner);
        mvc.perform(get(tacticsUrl(match, third)).with(user(principal))).andExpect(status().isNotFound());
    }

    @Test
    void boardWithoutLineupLeadsToLineupForm() throws Exception {
        mvc.perform(get(tacticsUrl(match, opponent)).with(user(principal)))
                .andExpect(redirectedUrl(lineupUrl(match, opponent) + "/edit"));
    }

    // Fotografije na tabli

    @Test
    void boardUsesExistingPlayerPhotosAndSilhouetteAsFallback() throws Exception {
        Player withPhoto = squad.get(9);
        PlayerForm form = PlayerForm.from(withPhoto);
        form.setPhoto(new MockMultipartFile("photo", "x.png", "image/png", PNG));
        playerService.update(withPhoto.getId(), form, owner.getId());

        // Thymeleaf u JavaScriptu zapisuje kosu crtu kao \/
        String board = page(tacticsUrl(match, team)).replace("\\/", "/");
        assertThat(board).contains("\"photoUrl\":\"/players/" + withPhoto.getId() + "/photo\"");
        assertThat(board).contains("\"photoUrl\":null", "/img/player-silhouette.svg");
        assertThat(page(lineupUrl(match, team))).contains("/players/" + withPhoto.getId() + "/photo", "/img/player-silhouette.svg");
        mvc.perform(get("/img/player-silhouette.svg")).andExpect(status().isOk());
        mvc.perform(get("/img/coach-avatar.svg")).andExpect(status().isOk());
    }

    // Fotografija selektora

    @Test
    void coachPhotoCanBeAddedChangedRemovedAndIsPrivate() throws Exception {
        String photoUrl = "/teams/" + team.getId() + "/coach-photo";
        assertThat(page("/teams/" + team.getId())).contains("/img/coach-avatar.svg").doesNotContain(photoUrl);
        mvc.perform(get(photoUrl).with(user(principal))).andExpect(status().isNotFound());

        mvc.perform(coachRequest(new MockMultipartFile("coachPhoto", "fake.png", "image/png", "nije slika".getBytes()), false))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JPG, PNG ili WEBP")));

        mvc.perform(coachRequest(new MockMultipartFile("coachPhoto", "c.png", "image/png", PNG), false))
                .andExpect(redirectedUrl("/teams/" + team.getId()));
        mvc.perform(get(photoUrl).with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        assertThat(page("/teams/" + team.getId())).contains(photoUrl, team.getCoach());
        assertThat(page("/dashboard")).contains(photoUrl, team.getCoach());
        assertThat(page(tacticsUrl(match, team))).contains(photoUrl);

        String firstFile = teamService.getOwned(team.getId(), owner.getId()).getCoachPhotoFileName();
        mvc.perform(coachRequest(new MockMultipartFile("coachPhoto", "d.png", "image/png", PNG), false));
        assertThat(teamService.getOwned(team.getId(), owner.getId()).getCoachPhotoFileName()).isNotEqualTo(firstFile);

        mvc.perform(get(photoUrl).with(user(new AppUserPrincipal(registerUser())))).andExpect(status().isNotFound());

        mvc.perform(coachRequest(new MockMultipartFile("coachPhoto", "", "application/octet-stream", new byte[0]), true))
                .andExpect(redirectedUrl("/teams/" + team.getId()));
        assertThat(teamService.getOwned(team.getId(), owner.getId()).getCoachPhotoFileName()).isNull();
        assertThat(page("/teams/" + team.getId())).contains("/img/coach-avatar.svg");
    }

    // Jezik

    @Test
    void translationFilesHaveSameKeysAndCoverEveryTemplateAndEnum() throws Exception {
        Properties serbian = load("messages_sr.properties");
        Properties english = load("messages_en.properties");
        assertThat(english.stringPropertyNames()).isEqualTo(serbian.stringPropertyNames());

        Set<String> keys = serbian.stringPropertyNames();
        var templates = new PathMatchingResourcePatternResolver().getResources("classpath:templates/**/*.html");
        assertThat(templates).isNotEmpty();
        for (var template : templates) {
            String html = template.getContentAsString(StandardCharsets.UTF_8);
            Matcher matcher = MESSAGE_KEY.matcher(html);
            while (matcher.find()) {
                assertThat(keys).as(template.getFilename()).contains(matcher.group(1));
            }
        }
        List<Class<? extends Enum<?>>> enums = List.of(Confederation.class, PlayerPosition.class, MatchStatus.class,
                com.nationalteammanager.enums.CompetitionType.class, com.nationalteammanager.enums.EventType.class,
                com.nationalteammanager.enums.PlayStyle.class, com.nationalteammanager.enums.Mentality.class,
                com.nationalteammanager.enums.PlayWidth.class, com.nationalteammanager.enums.PlayTempo.class,
                com.nationalteammanager.enums.PressingIntensity.class, com.nationalteammanager.enums.DefensiveLine.class,
                com.nationalteammanager.enums.PassingStyle.class, com.nationalteammanager.enums.AttackDirection.class,
                com.nationalteammanager.enums.PlayerDuty.class);
        for (Class<? extends Enum<?>> type : enums) {
            for (Enum<?> value : type.getEnumConstants()) {
                assertThat(keys).contains(type.getSimpleName() + "." + value.name());
            }
        }
    }

    @Test
    void everyMainPageIsFullyTranslatedInBothLanguages() throws Exception {
        for (String url : List.of("/dashboard", "/statistics", "/teams", "/teams/new", "/teams/" + team.getId(),
                "/teams/" + team.getId() + "/edit", "/teams/" + team.getId() + "/players/new", "/players",
                "/players/" + squad.get(0).getId(), "/players/" + squad.get(0).getId() + "/edit", "/matches",
                "/matches/new", "/matches/" + match.getId(), "/matches/" + match.getId() + "/edit",
                lineupUrl(match, team), lineupUrl(match, team) + "/edit", tacticsUrl(match, team), "/competitions",
                "/competitions/new")) {
            String english = pageIn("en", url);
            assertThat(english).as(url).doesNotContain("??");
            assertThat(SERBIAN_LETTERS.matcher(english.replaceAll("Srpski", "")).find())
                    .as("srpski tekst na engleskoj stranici " + url + ": " + firstSerbianLine(english)).isFalse();
            assertThat(pageIn("sr", url)).as(url).doesNotContain("??");
        }

        assertThat(pageIn("sr", "/dashboard")).contains("Reprezentacije", "Odjava", "Dobro došli");
        assertThat(pageIn("en", "/dashboard")).contains("National teams", "Log out", "Welcome");
        assertThat(pageIn("en", tacticsUrl(match, team))).contains("Save tactics", "Playing style", "Very attacking", "Substitutes");
        assertThat(pageIn("sr", tacticsUrl(match, team))).contains("Sačuvaj taktiku", "Stil igre", "Veoma ofanzivan", "Rezervni igrači");

        // Podaci se ne prevode
        assertThat(pageIn("en", "/teams/" + team.getId())).contains(team.getTeamName(), team.getCoach(), team.getCountryName());
    }

    @Test
    void validationAndBusinessMessagesFollowTheLanguage() throws Exception {
        Cookie englishCookie = new Cookie(LANGUAGE_COOKIE, "en");

        mvc.perform(post("/teams").with(user(principal)).with(csrf()).cookie(englishCookie)
                        .param("countryName", "").param("teamName", "").param("coach", ""))
                .andExpect(content().string(containsString("Enter the country.")))
                .andExpect(content().string(not(containsString("Unesite naziv"))));

        mvc.perform(post("/teams/" + team.getId() + "/players").with(user(principal)).with(csrf()).cookie(englishCookie)
                        .param("firstName", "A").param("lastName", "B").param("birthDate", "2000-01-01")
                        .param("nationality", "X").param("position", "DEFENDER")
                        .param("jerseyNumber", squad.get(0).getJerseyNumber().toString())
                        .param("heightCm", "180"))
                .andExpect(content().string(containsString("Jersey number 1 is already taken")));

        mvc.perform(post("/players/" + squad.get(0).getId() + "/delete").with(user(principal)).with(csrf()).cookie(englishCookie))
                .andExpect(flash().attribute("errorMessage", containsString("cannot be deleted")));

        mvc.perform(post("/teams/" + team.getId() + "/players").with(user(principal)).with(csrf()).cookie(englishCookie)
                        .param("firstName", "A").param("lastName", "B").param("birthDate", "nije-datum")
                        .param("nationality", "X").param("position", "DEFENDER").param("jerseyNumber", "tekst")
                        .param("heightCm", "180"))
                .andExpect(content().string(containsString("The entered value is not valid.")))
                .andExpect(content().string(not(containsString("Failed to convert"))));

        mvc.perform(get("/teams/999999").with(user(principal)).cookie(englishCookie))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("The national team was not found.")));
        mvc.perform(get("/teams/999999").with(user(principal)))
                .andExpect(content().string(containsString("Reprezentacija nije pronađena.")));
    }

    @Test
    void languageIsRememberedForUserAcrossLogoutAndLogin() throws Exception {
        String username = owner.getUsername();

        MvcResult change = mvc.perform(post("/language").param("lang", "en").with(user(principal)).with(csrf())
                        .header("Referer", "http://localhost/matches?view=finished"))
                .andExpect(redirectedUrl("/matches?view=finished"))
                .andExpect(cookie().value(LANGUAGE_COOKIE, "en"))
                .andReturn();
        assertThat(change.getResponse().getCookie(LANGUAGE_COOKIE).getMaxAge()).isPositive();
        assertThat(userRepository.findById(owner.getId()).orElseThrow().getPreferredLanguage()).isEqualTo("en");

        // Prijava s drugog preglednika, bez kolačića, vraća sačuvani jezik
        mvc.perform(formLogin("/login").user(username).password("Lozinka123"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(cookie().value(LANGUAGE_COOKIE, "en"));

        mvc.perform(post("/language").param("lang", "sr").with(user(principal)).with(csrf()));
        mvc.perform(formLogin("/login").user(username).password("Lozinka123"))
                .andExpect(cookie().value(LANGUAGE_COOKIE, "sr"));

        // Nepoznat jezik se ignoriše, a referer ne može preusmjeriti na drugu stranicu
        mvc.perform(post("/language").param("lang", "de").with(user(principal)).with(csrf())
                        .header("Referer", "https://example.com/zlo"))
                .andExpect(redirectedUrl("/zlo"));
        assertThat(userRepository.findById(owner.getId()).orElseThrow().getPreferredLanguage()).isEqualTo("sr");
    }

    @Test
    void anonymousVisitorCanChooseLanguageAndRegisterInIt() throws Exception {
        MvcResult result = mvc.perform(post("/language").param("lang", "en").with(csrf())
                        .header("Referer", "http://localhost/login?error"))
                .andExpect(redirectedUrl("/login"))
                .andReturn();
        Cookie languageCookie = result.getResponse().getCookie(LANGUAGE_COOKIE);

        mvc.perform(get("/login").cookie(languageCookie))
                .andExpect(content().string(containsString("Username")))
                .andExpect(content().string(containsString("Log in")));
        mvc.perform(get("/register").cookie(languageCookie))
                .andExpect(content().string(containsString("Create account")));

        String username = "engleski" + SEQUENCE.incrementAndGet();
        mvc.perform(post("/register").with(csrf()).cookie(languageCookie)
                .param("fullName", "English User").param("username", username)
                .param("email", username + "@example.com")
                .param("password", "Lozinka123").param("confirmPassword", "Lozinka123"));
        assertThat(userRepository.findByUsernameIgnoreCase(username).orElseThrow().getPreferredLanguage()).isEqualTo("en");
    }

    // Pomoćne metode

    private String page(String url) throws Exception {
        return mvc.perform(get(url).with(user(principal))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String pageIn(String language, String url) throws Exception {
        return mvc.perform(get(url).with(user(principal)).cookie(new Cookie(LANGUAGE_COOKIE, language)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String firstSerbianLine(String html) {
        return html.lines().filter(line -> SERBIAN_LETTERS.matcher(line).find() && !line.contains("Srpski"))
                .findFirst().orElse("").trim();
    }

    private Properties load(String file) throws Exception {
        Properties properties = new Properties();
        try (var reader = new java.io.InputStreamReader(new ClassPathResource(file).getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private String lineupUrl(Match currentMatch, NationalTeam currentTeam) {
        return "/matches/" + currentMatch.getId() + "/lineups/" + currentTeam.getId();
    }

    private String tacticsUrl(Match currentMatch, NationalTeam currentTeam) {
        return lineupUrl(currentMatch, currentTeam) + "/tactics";
    }

    private MockHttpServletRequestBuilder tacticsPost(Match currentMatch, NationalTeam currentTeam) {
        return post(tacticsUrl(currentMatch, currentTeam)).with(user(principal)).with(csrf());
    }

    private MockHttpServletRequestBuilder tacticsRequest(MockHttpServletRequestBuilder request, String formation) {
        return tacticsRequest(request, formation, "POSSESSION");
    }

    private MockHttpServletRequestBuilder tacticsRequest(MockHttpServletRequestBuilder request, String formation,
                                                         String playStyle) {
        return request.param("formation", formation).param("playStyle", playStyle);
    }

    /** Ispravan obrazac table: početnih 11, dvije rezerve i neutralne postavke. */
    private MockHttpServletRequestBuilder validParams(MockHttpServletRequestBuilder request) {
        for (int slot = 0; slot < 11; slot++) {
            request.param("starterIds[" + slot + "]", squad.get(slot).getId().toString());
        }
        request.param("substituteIds", squad.get(11).getId().toString(), squad.get(12).getId().toString());
        for (String[] setting : new String[][]{{"mentality", "BALANCED"}, {"playWidth", "NORMAL"}, {"playTempo", "NORMAL"},
                {"pressing", "MEDIUM"}, {"defensiveLine", "NORMAL"}, {"passingStyle", "MIXED"},
                {"attackDirection", "THROUGH_MIDDLE"}}) {
            request.param(setting[0], setting[1]);
        }
        return request;
    }

    private void replaceParam(MockHttpServletRequestBuilder request, String name, Long value) {
        request.with(mock -> {
            mock.setParameter(name, value == null ? "" : value.toString());
            return mock;
        });
    }

    private void replaceSubstitutes(MockHttpServletRequestBuilder request, Long... ids) {
        request.with(mock -> {
            mock.setParameter("substituteIds", java.util.Arrays.stream(ids).map(String::valueOf).toArray(String[]::new));
            return mock;
        });
    }

    private void expectBoardError(java.util.function.Consumer<MockHttpServletRequestBuilder> change, String message) throws Exception {
        MockHttpServletRequestBuilder request = tacticsRequest(validParams(tacticsPost(match, team)), "F_5_3_2")
                .param("captainId", squad.get(5).getId().toString());
        change.accept(request);
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(containsString(message)));
    }

    private MockHttpServletRequestBuilder coachRequest(MockMultipartFile file, boolean remove) {
        var request = multipart("/teams/" + team.getId()).file(file).with(user(principal)).with(csrf())
                .param("countryName", team.getCountryName()).param("teamName", team.getTeamName())
                .param("confederation", "UEFA").param("coach", team.getCoach()).param("foundedYear", "1950");
        if (remove) {
            request.param("removeCoachPhoto", "true");
        }
        return request;
    }

    private User registerUser() {
        int id = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Test User " + id);
        form.setUsername("tester" + id);
        form.setEmail("tester" + id + "@example.com");
        form.setPassword("Lozinka123");
        form.setConfirmPassword("Lozinka123");
        return userService.register(form);
    }

    private NationalTeam createTeam(User teamOwner) {
        int id = SEQUENCE.incrementAndGet();
        NationalTeamForm form = new NationalTeamForm();
        form.setCountryName("Country" + id);
        form.setTeamName("Team" + id);
        form.setConfederation(Confederation.UEFA);
        form.setCoach("Coach" + id);
        form.setFoundedYear(1950);
        return teamService.create(form, teamOwner.getId());
    }

    /** Indeks 0 je golman, 1-10 igrači iz polja, 11 rezervni golman, a 12 i 13 rezervni igrači. */
    private List<Player> createSquad(NationalTeam squadTeam) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            PlayerPosition position = i == 0 || i == 11 ? PlayerPosition.GOALKEEPER
                    : i <= 4 ? PlayerPosition.DEFENDER : i <= 7 || i == 12 ? PlayerPosition.MIDFIELDER : PlayerPosition.FORWARD;
            PlayerForm form = new PlayerForm();
            form.setFirstName("First" + (i + 1));
            form.setLastName("Last" + (i + 1));
            form.setBirthDate(LocalDate.of(1998, 5, 20));
            form.setNationality("Nation");
            form.setPosition(position);
            form.setJerseyNumber(i + 1);
            form.setHeightCm(180);
            form.setClub("Club");
            players.add(playerService.create(squadTeam.getId(), form, squadTeam.getOwner().getId()));
        }
        return players;
    }

    private Match createMatch(NationalTeam home, NationalTeam away) {
        MatchForm form = new MatchForm();
        form.setHomeTeamId(home.getId());
        form.setAwayTeamId(away.getId());
        form.setKickoff(LocalDateTime.now().plusDays(100L + SEQUENCE.incrementAndGet()));
        form.setStadium("Stadium");
        form.setStatus(MatchStatus.SCHEDULED);
        return matchService.create(form, home.getOwner().getId());
    }
}
