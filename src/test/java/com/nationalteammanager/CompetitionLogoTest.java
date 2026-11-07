package com.nationalteammanager;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.CompetitionType;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.repository.CompetitionRepository;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompetitionLogoTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(30000);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\"/></svg>";
    private static final String DEFAULT_LOGO = "/img/competition-default.svg";

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;
    @Autowired private NationalTeamService teamService;
    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private MatchService matchService;
    @Autowired private JdbcTemplate jdbc;
    @Value("${app.upload-root}") private String uploadRoot;

    private AppUserPrincipal admin;
    private AppUserPrincipal regular;
    private User regularUser;

    @BeforeEach
    void createUsers() {
        User adminUser = registerUser();
        jdbc.update("update Users set role = 'ADMIN' where id = ?", adminUser.getId());
        admin = new AppUserPrincipal(userService.getById(adminUser.getId()));
        regularUser = registerUser();
        regular = new AppUserPrincipal(regularUser);
    }

    @Test
    void adminCreatesOfficialCompetitionWithLogoVisibleToEveryone() throws Exception {
        String url = createAsAdmin("Liga logo " + SEQUENCE.incrementAndGet(), png("logo.png"));
        Competition competition = competition(url);

        assertThat(competition.isShared()).isTrue();
        assertThat(competition.getLogoPath()).matches("competitions/[0-9a-f-]+\\.png");
        assertThat(Files.exists(Path.of(uploadRoot).resolve(competition.getLogoPath()))).isTrue();

        // Običan korisnik vidi takmičenje i logo, ali bez akcija izmjene
        String list = page("/competitions", regular);
        assertThat(list).contains(competition.getName(), url + "/logo").doesNotContain(url + "/edit");
        assertThat(page(url, regular)).contains(url + "/logo").doesNotContain(url + "/edit");
        byte[] logo = mvc.perform(get(url + "/logo").with(user(regular))).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andReturn().getResponse().getContentAsByteArray();
        assertThat(logo).isEqualTo(PNG);
    }

    @Test
    void editingWithoutNewFileKeepsLogoAndNewFileReplacesIt() throws Exception {
        String url = createAsAdmin("Kup " + SEQUENCE.incrementAndGet(), png("a.png"));
        String original = competition(url).getLogoPath();

        mvc.perform(competitionRequest(url, "Promijenjen naziv", null, false, admin)).andExpect(status().is3xxRedirection());
        Competition renamed = competition(url);
        assertThat(renamed.getName()).isEqualTo("Promijenjen naziv");
        assertThat(renamed.getLogoPath()).isEqualTo(original);

        MockMultipartFile svg = new MockMultipartFile("logo", "novi.SVG", "image/svg+xml", SVG.getBytes(StandardCharsets.UTF_8));
        mvc.perform(competitionRequest(url, "Promijenjen naziv", svg, false, admin)).andExpect(status().is3xxRedirection());
        String replaced = competition(url).getLogoPath();
        assertThat(replaced).endsWith(".svg").isNotEqualTo(original);
        assertThat(Files.exists(Path.of(uploadRoot).resolve(original))).isFalse();
        mvc.perform(get(url + "/logo").with(user(regular))).andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")));

        MockMultipartFile jpeg = new MockMultipartFile("logo", "foto.jpeg", "image/jpeg", JPG);
        mvc.perform(competitionRequest(url, "Promijenjen naziv", jpeg, false, admin)).andExpect(status().is3xxRedirection());
        assertThat(competition(url).getLogoPath()).endsWith(".jpg");
    }

    @Test
    void adminRemovesLogoAndDefaultImageIsShown() throws Exception {
        String url = createAsAdmin("Turnir " + SEQUENCE.incrementAndGet(), png("t.png"));
        String path = competition(url).getLogoPath();

        mvc.perform(post(url + "/logo/delete").with(user(admin)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(competition(url).getLogoPath()).isNull();
        assertThat(Files.exists(Path.of(uploadRoot).resolve(path))).isFalse();
        assertThat(page(url, regular)).contains(DEFAULT_LOGO);
        mvc.perform(get(url + "/logo").with(user(regular))).andExpect(status().isNotFound());

        // Brisanje preko obrasca (oznaka removeLogo)
        mvc.perform(competitionRequest(url, "Turnir", png("n.png"), false, admin)).andExpect(status().is3xxRedirection());
        assertThat(competition(url).getLogoPath()).isNotNull();
        mvc.perform(competitionRequest(url, "Turnir", null, true, admin)).andExpect(status().is3xxRedirection());
        assertThat(competition(url).getLogoPath()).isNull();
    }

    @Test
    void invalidFilesAreRejected() throws Exception {
        String url = createAsAdmin("Provjera " + SEQUENCE.incrementAndGet(), null);
        byte[] tooLarge = Arrays.copyOf(PNG, 5 * 1024 * 1024 + 1);
        MockMultipartFile[] invalid = {
                new MockMultipartFile("logo", "veliki.png", "image/png", tooLarge),
                new MockMultipartFile("logo", "slika.gif", "image/gif", "GIF89a".getBytes(StandardCharsets.US_ASCII)),
                new MockMultipartFile("logo", "lazni.png", "image/png", "nije slika".getBytes(StandardCharsets.UTF_8)),
                new MockMultipartFile("logo", "pogresno.png", "image/png", JPG),
                new MockMultipartFile("logo", "opasan.svg", "image/svg+xml",
                        "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8)),
                new MockMultipartFile("logo", "dogadjaj.svg", "image/svg+xml",
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>".getBytes(StandardCharsets.UTF_8))
        };
        String[] messages = {"5 MB", "PNG, JPG, JPEG ili SVG", "PNG, JPG, JPEG ili SVG", "PNG, JPG, JPEG ili SVG", "skriptu", "skriptu"};
        for (int i = 0; i < invalid.length; i++) {
            String html = mvc.perform(competitionRequest(url, "Provjera", invalid[i], false, admin))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(html).as(invalid[i].getOriginalFilename()).contains(messages[i]);
            assertThat(competition(url).getLogoPath()).isNull();
        }
    }

    @Test
    void regularUserCannotChangeOfficialCompetitionOrUploadLogos() throws Exception {
        String url = createAsAdmin("Zvanicno " + SEQUENCE.incrementAndGet(), png("z.png"));
        String path = competition(url).getLogoPath();

        mvc.perform(get(url + "/edit").with(user(regular))).andExpect(status().isForbidden());
        mvc.perform(competitionRequest(url, "Hak", null, false, regular)).andExpect(status().isForbidden());
        mvc.perform(post(url + "/logo/delete").with(user(regular)).with(csrf())).andExpect(status().isForbidden())
                .andExpect(content().string(containsString("samo administrator")));
        mvc.perform(post(url + "/delete").with(user(regular)).with(csrf())).andExpect(status().isForbidden());
        assertThat(competition(url).getLogoPath()).isEqualTo(path);
        assertThat(competition(url).getName()).startsWith("Zvanicno");

        // Ne može napraviti zajedničko takmičenje niti dodati logo svom takmičenju
        MockMultipartHttpServletRequestBuilder shared = competitionRequest("/competitions", "Moje", null, false, regular);
        shared.param("shared", "true");
        mvc.perform(shared).andExpect(status().isForbidden());
        mvc.perform(competitionRequest("/competitions", "Moje", png("m.png"), false, regular)).andExpect(status().isForbidden());

        // Lično takmičenje bez logotipa i dalje radi, uz podrazumijevanu sliku
        String own = mvc.perform(competitionRequest("/competitions", "Moje", null, false, regular))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(competition(own).isShared()).isFalse();
        assertThat(page(own, regular)).contains(DEFAULT_LOGO, own + "/edit");
        assertThat(page("/competitions/new", regular)).contains("administrator").doesNotContain("name=\"logo\"");

        // Tuđe lično takmičenje se ne vidi
        mvc.perform(get(own).with(user(admin))).andExpect(status().isNotFound());
        mvc.perform(get(own + "/logo").with(user(admin))).andExpect(status().isNotFound());
    }

    @Test
    void matchPagesShowLogoOfOfficialCompetition() throws Exception {
        String url = createAsAdmin("Prvenstvo " + SEQUENCE.incrementAndGet(), png("p.png"));
        Competition competition = competition(url);
        NationalTeam home = createTeam(regularUser);
        NationalTeam away = createTeam(regularUser);
        MatchForm form = new MatchForm();
        form.setHomeTeamId(home.getId());
        form.setAwayTeamId(away.getId());
        form.setCompetitionId(competition.getId());
        form.setKickoff(LocalDateTime.of(2030, 6, 15, 18, 0));
        form.setStatus(MatchStatus.SCHEDULED);
        form.setStadium("Bilino polje");
        Match match = matchService.create(form, regularUser.getId());

        String logoUrl = url + "/logo?v=";
        assertThat(page("/matches/" + match.getId(), regular)).contains(competition.getName(), logoUrl);
        assertThat(page("/matches", regular)).contains(logoUrl);
        assertThat(page("/matches/new", regular)).contains("competitionLogoPreview", "data-logo-url=\"" + logoUrl);
        assertThat(page(url, regular)).contains("/matches/" + match.getId());
        // Admin ne vidi utakmice drugog korisnika u zajedničkom takmičenju
        assertThat(page(url, admin)).doesNotContain("/matches/" + match.getId());
    }

    @Test
    void deletingCompetitionRemovesLogoFile() throws Exception {
        String url = createAsAdmin("Brisanje " + SEQUENCE.incrementAndGet(), png("b.png"));
        Path file = Path.of(uploadRoot).resolve(competition(url).getLogoPath());

        mvc.perform(post(url + "/delete").with(user(admin)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(competitionRepository.findById(id(url))).isEmpty();
        assertThat(Files.exists(file)).isFalse();
    }

    private String createAsAdmin(String name, MockMultipartFile logo) throws Exception {
        MockMultipartHttpServletRequestBuilder request = competitionRequest("/competitions", name, logo, false, admin);
        request.param("shared", "true");
        return mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    }

    private MockMultipartHttpServletRequestBuilder competitionRequest(String url, String name, MockMultipartFile logo,
                                                                       boolean removeLogo, AppUserPrincipal principal) {
        MockMultipartHttpServletRequestBuilder request = multipart(url);
        if (logo != null) {
            request.file(logo);
        }
        request.param("name", name).param("season", "2030").param("type", CompetitionType.WORLD_CUP.name())
                .param("startDate", "2030-01-01").param("endDate", "2030-12-31");
        if (removeLogo) {
            request.param("removeLogo", "true");
        }
        request.with(user(principal)).with(csrf());
        return request;
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("logo", name, "image/png", PNG);
    }

    private Competition competition(String url) {
        return competitionRepository.findById(id(url)).orElseThrow();
    }

    private Long id(String url) {
        return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    }

    private String page(String url, AppUserPrincipal principal) throws Exception {
        return mvc.perform(get(url).with(user(principal))).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private User registerUser() {
        int id = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Logo Tester " + id);
        form.setUsername("logo" + id);
        form.setEmail("logo" + id + "@example.com");
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
