package com.nationalteammanager;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.dto.StadiumForm;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Stadium;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.enums.StadiumSurface;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.StadiumRepository;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.StadiumService;
import com.nationalteammanager.service.UserService;
import jakarta.servlet.http.Cookie;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StadiumFlowTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(20000);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    private static final String DEFAULT_PHOTO = "/img/stadium-default.svg";

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;
    @Autowired private NationalTeamService teamService;
    @Autowired private StadiumService stadiumService;
    @Autowired private StadiumRepository stadiumRepository;
    @Autowired private MatchService matchService;

    private User owner;
    private AppUserPrincipal principal;
    private NationalTeam home;
    private NationalTeam away;

    @BeforeEach
    void createData() {
        owner = registerUser();
        principal = new AppUserPrincipal(owner);
        home = createTeam(owner);
        away = createTeam(owner);
    }

    // Upravljanje stadionima

    @Test
    void stadiumCanBeCreatedViewedEditedAndDeleted() throws Exception {
        String created = mvc.perform(stadiumRequest("/stadiums", home, "Koševo", false, null))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(created).matches("/stadiums/\\d+");

        assertThat(page(created)).contains("Koševo", "Sarajevo", "Bosna", "34.500", "1947", "Prirodna trava", "Patriotske lige", DEFAULT_PHOTO);
        assertThat(page("/stadiums")).contains("Koševo", "34.500", created);
        assertThat(page("/teams/" + home.getId())).contains("Koševo", created);
        assertThat(page("/stadiums?teamId=" + home.getId())).contains("Koševo");
        assertThat(page("/stadiums?teamId=" + away.getId())).doesNotContain("Koševo");
        assertThat(page(created + "/edit")).contains("value=\"34500\"");

        mvc.perform(stadiumRequest(created, home, "Novi naziv", false, null)).andExpect(redirectedUrl(created));
        assertThat(page(created)).contains("Novi naziv").doesNotContain("Koševo");

        mvc.perform(post(created + "/delete").with(user(principal)).with(csrf())).andExpect(redirectedUrl("/stadiums"));
        mvc.perform(get(created).with(user(principal))).andExpect(status().isNotFound());
    }

    @Test
    void invalidStadiumDataIsRejected() throws Exception {
        expectError(request -> request.param("name", ""), "Unesite naziv stadiona");
        expectError(request -> request.param("capacity", "0"), "Kapacitet mora biti");
        expectError(request -> request.param("capacity", "abc"), "Unesena vrijednost nije ispravna");
        expectError(request -> request.param("builtYear", "1700"), "prije 1800");
        expectError(request -> request.param("builtYear", "2999"), "u budućnosti");
        expectError(request -> request.param("surface", ""), "vrstu podloge");
        expectError(request -> request.param("address", ""), "Unesite adresu");

        createStadium(home, "Isti naziv", false);
        expectError(request -> request.param("name", "isti NAZIV"), "već ima stadion");
    }

    // Glavni stadion

    @Test
    void teamHasOnlyOneMainStadiumButTeamsAreIndependent() {
        Stadium first = createStadium(home, "Prvi", true);
        Stadium second = createStadium(home, "Drugi", false);
        Stadium otherTeam = createStadium(away, "Tudji glavni", true);
        assertThat(mainStadiums(home)).containsExactly(first.getId());

        Stadium third = createStadium(home, "Treci", true);
        assertThat(mainStadiums(home)).containsExactly(third.getId());

        StadiumForm form = StadiumForm.from(stadiumService.getOwned(second.getId(), owner.getId()));
        form.setMainStadium(true);
        stadiumService.update(second.getId(), form, owner.getId());
        assertThat(mainStadiums(home)).containsExactly(second.getId());

        // Čuvanje postojećeg glavnog stadiona ne smije ukloniti njegovu oznaku
        stadiumService.update(second.getId(), StadiumForm.from(stadiumService.getOwned(second.getId(), owner.getId())), owner.getId());
        assertThat(mainStadiums(home)).containsExactly(second.getId());

        assertThat(mainStadiums(away)).containsExactly(otherTeam.getId());
    }

    // Fotografija

    @Test
    void photoCanBeAddedChangedRemovedAndIsPrivate() throws Exception {
        String created = mvc.perform(stadiumRequest("/stadiums", home, "Foto", false, new MockMultipartFile("photo", "s.png", "image/png", PNG)))
                .andReturn().getResponse().getRedirectedUrl();
        Long id = Long.valueOf(created.substring(created.lastIndexOf('/') + 1));
        String photoUrl = created + "/photo";

        assertThat(page(created)).contains(photoUrl);
        assertThat(page("/stadiums")).contains(photoUrl);
        mvc.perform(get(photoUrl).with(user(principal))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));

        String firstFile = stadiumService.getOwned(id, owner.getId()).getPhotoFileName();
        mvc.perform(stadiumRequest(created, home, "Foto", false, new MockMultipartFile("photo", "t.png", "image/png", PNG)));
        assertThat(stadiumService.getOwned(id, owner.getId()).getPhotoFileName()).isNotNull().isNotEqualTo(firstFile);

        mvc.perform(stadiumRequest(created, home, "Foto", false, new MockMultipartFile("photo", "x.png", "image/png", "nije slika".getBytes())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("JPG, PNG ili WEBP")));

        mvc.perform(get(photoUrl).with(user(new AppUserPrincipal(registerUser())))).andExpect(status().isNotFound());

        mvc.perform(stadiumRequest(created, home, "Foto", false, null).param("removePhoto", "true"));
        assertThat(page(created)).contains(DEFAULT_PHOTO).doesNotContain(photoUrl);
        mvc.perform(get(photoUrl).with(user(principal))).andExpect(status().isNotFound());
        mvc.perform(get(DEFAULT_PHOTO)).andExpect(status().isOk());
    }

    // Vlasništvo

    @Test
    void userCannotAccessAnotherUsersStadiums() throws Exception {
        Stadium stadium = createStadium(home, "Privatni", true);
        AppUserPrincipal stranger = new AppUserPrincipal(registerUser());
        String url = "/stadiums/" + stadium.getId();

        mvc.perform(get(url).with(user(stranger))).andExpect(status().isNotFound());
        mvc.perform(get(url + "/edit").with(user(stranger))).andExpect(status().isNotFound());
        mvc.perform(post(url + "/delete").with(user(stranger)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/stadiums?teamId=" + home.getId()).with(user(stranger))).andExpect(status().isNotFound());
        mvc.perform(get("/stadiums").with(user(stranger)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Privatni"))));

        NationalTeam strangerTeam = createTeam(userService.getById(stranger.getId()));
        StadiumForm foreignTeam = stadiumForm(home, "Tudji", false);
        assertThatThrownBy(() -> stadiumService.create(foreignTeam, stranger.getId())).isInstanceOf(ResourceNotFoundException.class);

        // Tuđi stadion se ne može izabrati ni za svoju utakmicu
        MatchForm match = matchForm(strangerTeam, createTeam(userService.getById(stranger.getId())));
        match.setStadiumId(stadium.getId());
        assertThatThrownBy(() -> matchService.create(match, stranger.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    // Utakmice

    @Test
    void matchUsesStadiumOfHomeTeamEvenIfNotMain() throws Exception {
        createStadium(home, "Glavni domacina", true);
        Stadium secondary = createStadium(home, "Sporedni domacina", false);
        MatchForm form = matchForm(home, away);
        form.setStadiumId(secondary.getId());
        form.setStadium(null);

        Match match = matchService.create(form, owner.getId());

        String details = page("/matches/" + match.getId());
        assertThat(details).contains("Sporedni domacina", "Sarajevo", DEFAULT_PHOTO, "/stadiums/" + secondary.getId());
        assertThat(page("/matches")).contains("Sporedni domacina");
        assertThat(page("/matches/" + match.getId() + "/edit")).contains("Sporedni domacina", "data-team-id=\"" + home.getId() + "\"");

        // Promjena naziva stadiona važi i na utakmici
        StadiumForm renamed = StadiumForm.from(stadiumService.getOwned(secondary.getId(), owner.getId()));
        renamed.setName("Preimenovan");
        stadiumService.update(secondary.getId(), renamed, owner.getId());
        assertThat(page("/matches/" + match.getId())).contains("Preimenovan").doesNotContain("Sporedni domacina");
    }

    @Test
    void stadiumMustBelongToHomeTeam() {
        Stadium awayStadium = createStadium(away, "Stadion gosta", true);
        MatchForm form = matchForm(home, away);
        form.setStadiumId(awayStadium.getId());
        assertThatThrownBy(() -> matchService.create(form, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.venueNotHome");

        // Zamjena domaćina na postojećoj utakmici prolazi kroz istu provjeru
        Stadium homeStadium = createStadium(home, "Stadion domacina", true);
        MatchForm valid = matchForm(home, away);
        valid.setStadiumId(homeStadium.getId());
        Match match = matchService.create(valid, owner.getId());
        MatchForm swapped = MatchForm.from(matchService.getOwned(match.getId(), owner.getId()));
        swapped.setHomeTeamId(away.getId());
        swapped.setAwayTeamId(home.getId());
        assertThatThrownBy(() -> matchService.update(match.getId(), swapped, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.venueNotHome");
    }

    @Test
    void matchWithoutStadiumFromListStillWorksAndNameIsRequired() throws Exception {
        MatchForm custom = matchForm(home, away);
        custom.setStadium("Neutralni teren");
        Match match = matchService.create(custom, owner.getId());
        assertThat(page("/matches/" + match.getId())).contains("Neutralni teren");
        assertThat(page("/matches/" + match.getId() + "/edit")).contains("Neutralni teren");

        MatchForm none = matchForm(home, away);
        none.setStadium("   ");
        assertThatThrownBy(() -> matchService.create(none, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.stadiumRequired");
    }

    @Test
    void matchFormThroughHttpSavesSelectedStadium() throws Exception {
        Stadium stadium = createStadium(home, "Http stadion", true);
        String kickoff = LocalDateTime.now().plusDays(70L + SEQUENCE.incrementAndGet()).withSecond(0).withNano(0).toString().substring(0, 16);
        String url = mvc.perform(post("/matches").with(user(principal)).with(csrf())
                        .param("homeTeamId", home.getId().toString()).param("awayTeamId", away.getId().toString())
                        .param("kickoff", kickoff).param("status", "SCHEDULED").param("stadiumId", stadium.getId().toString()))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(page(url)).contains("Http stadion");

        mvc.perform(post("/matches").with(user(principal)).with(csrf())
                        .param("homeTeamId", home.getId().toString()).param("awayTeamId", away.getId().toString())
                        .param("kickoff", LocalDateTime.now().plusDays(70L + SEQUENCE.incrementAndGet()).withSecond(0).withNano(0).toString().substring(0, 16))
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Izaberite stadion")));
    }

    // Brisanje

    @Test
    void stadiumWithMatchesIsProtectedAndTeamDeletionRemovesStadiums() throws Exception {
        Stadium used = createStadium(home, "Koristen", true);
        MatchForm form = matchForm(home, away);
        form.setStadiumId(used.getId());
        Match match = matchService.create(form, owner.getId());

        mvc.perform(post("/stadiums/" + used.getId() + "/delete").with(user(principal)).with(csrf())
                        .header("Referer", "http://localhost/stadiums/" + used.getId()))
                .andExpect(redirectedUrl("/stadiums/" + used.getId()))
                .andExpect(flash().attribute("errorMessage", containsString("postoje utakmice")));

        StadiumForm moved = StadiumForm.from(stadiumService.getOwned(used.getId(), owner.getId()));
        moved.setTeamId(away.getId());
        assertThatThrownBy(() -> stadiumService.update(used.getId(), moved, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.stadium.teamLocked");

        matchService.delete(match.getId(), owner.getId());
        Stadium free = createStadium(home, "Slobodan", false);
        teamService.delete(home.getId(), owner.getId());
        assertThat(stadiumRepository.findById(used.getId())).isEmpty();
        assertThat(stadiumRepository.findById(free.getId())).isEmpty();
    }

    // Jezik

    @Test
    void stadiumPagesAreTranslated() throws Exception {
        Stadium stadium = createStadium(home, "Arena", true);
        Cookie english = new Cookie("ntm_language", "en");
        for (String url : List.of("/stadiums", "/stadiums/new", "/stadiums/" + stadium.getId(), "/stadiums/" + stadium.getId() + "/edit")) {
            String html = mvc.perform(get(url).with(user(principal)).cookie(english)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).as(url).doesNotContain("??");
            assertThat(html.replace("Srpski", "")).as(url).doesNotContainPattern("[čćšđžČĆŠĐŽ]");
            assertThat(html).contains("Stadiums");
        }
        String details = mvc.perform(get("/stadiums/" + stadium.getId()).with(user(principal)).cookie(english))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(details).contains("Natural grass", "Main stadium", "Year built", "Arena");
        assertThat(page("/stadiums/" + stadium.getId())).contains("Prirodna trava", "Glavni stadion", "Godina izgradnje");
    }

    // Pomoćne metode

    private String page(String url) throws Exception {
        return mvc.perform(get(url).with(user(principal))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<Long> mainStadiums(NationalTeam team) {
        return stadiumRepository.findByTeamIdOrderByMainStadiumDescNameAsc(team.getId()).stream()
                .filter(Stadium::isMainStadium).map(Stadium::getId).toList();
    }

    private void expectError(java.util.function.Consumer<MockHttpServletRequestBuilder> change, String message) throws Exception {
        MockHttpServletRequestBuilder request = stadiumRequest("/stadiums", home, "Stadion" + SEQUENCE.incrementAndGet(), false, null);
        change.accept(request);
        // Parametar koji se mijenja zamjenjuje ispravnu vrijednost iz osnovnog zahtjeva
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(containsString(message)));
    }

    private MockHttpServletRequestBuilder stadiumRequest(String url, NationalTeam team, String name, boolean main, MockMultipartFile photo) {
        var request = multipart(url);
        if (photo != null) {
            request.file(photo);
        }
        request.with(user(principal)).with(csrf());
        request.with(mock -> {
            setDefault(mock, "teamId", team.getId().toString());
            setDefault(mock, "name", name);
            setDefault(mock, "city", "Sarajevo");
            setDefault(mock, "country", "Bosna");
            setDefault(mock, "capacity", "34500");
            setDefault(mock, "builtYear", "1947");
            setDefault(mock, "surface", "NATURAL_GRASS");
            setDefault(mock, "address", "Patriotske lige 35");
            setDefault(mock, "mainStadium", String.valueOf(main));
            return mock;
        });
        return request;
    }

    private void setDefault(org.springframework.mock.web.MockHttpServletRequest mock, String name, String value) {
        if (mock.getParameter(name) == null) {
            mock.setParameter(name, value);
        }
    }

    private Stadium createStadium(NationalTeam team, String name, boolean main) {
        return stadiumService.create(stadiumForm(team, name, main), team.getOwner().getId());
    }

    private StadiumForm stadiumForm(NationalTeam team, String name, boolean main) {
        StadiumForm form = new StadiumForm();
        form.setTeamId(team.getId());
        form.setName(name);
        form.setCity("Sarajevo");
        form.setCountry("Bosna i Hercegovina");
        form.setCapacity(34500);
        form.setBuiltYear(1947);
        form.setSurface(StadiumSurface.NATURAL_GRASS);
        form.setAddress("Patriotske lige 35");
        form.setMainStadium(main);
        return form;
    }

    private MatchForm matchForm(NationalTeam homeTeam, NationalTeam awayTeam) {
        MatchForm form = new MatchForm();
        form.setHomeTeamId(homeTeam.getId());
        form.setAwayTeamId(awayTeam.getId());
        form.setKickoff(LocalDateTime.now().plusDays(100L + SEQUENCE.incrementAndGet()));
        form.setStatus(MatchStatus.SCHEDULED);
        return form;
    }

    private User registerUser() {
        int id = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Stadium Tester " + id);
        form.setUsername("stadium" + id);
        form.setEmail("stadium" + id + "@example.com");
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
}
