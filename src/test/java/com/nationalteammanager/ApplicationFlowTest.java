package com.nationalteammanager;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.dto.CompetitionForm;
import com.nationalteammanager.dto.LineupForm;
import com.nationalteammanager.dto.MatchEventForm;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
import com.nationalteammanager.dto.OverallStatistics;
import com.nationalteammanager.dto.PlayerForm;
import com.nationalteammanager.dto.RegistrationForm;
import com.nationalteammanager.entity.Competition;
import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.entity.User;
import com.nationalteammanager.enums.CompetitionType;
import com.nationalteammanager.enums.Confederation;
import com.nationalteammanager.enums.EventType;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.enums.PlayerPosition;
import com.nationalteammanager.exception.BusinessRuleException;
import com.nationalteammanager.exception.ResourceNotFoundException;
import com.nationalteammanager.repository.MatchEventRepository;
import com.nationalteammanager.repository.MatchRepository;
import com.nationalteammanager.repository.PlayerRepository;
import com.nationalteammanager.repository.UserRepository;
import com.nationalteammanager.service.CompetitionService;
import com.nationalteammanager.service.LineupService;
import com.nationalteammanager.service.MatchEventService;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.PlayerService;
import com.nationalteammanager.service.StatisticsService;
import com.nationalteammanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testovi nisu @Transactional, pa svaki servis i stranica rade kao u stvarnoj aplikaciji,
 * uključujući i greške pri pristupu lijeno učitanim vezama (open-in-view je isključen).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationFlowTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private NationalTeamService teamService;
    @Autowired private PlayerService playerService;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private MatchService matchService;
    @Autowired private MatchRepository matchRepository;
    @Autowired private MatchEventRepository eventRepository;
    @Autowired private MatchEventService eventService;
    @Autowired private LineupService lineupService;
    @Autowired private CompetitionService competitionService;
    @Autowired private StatisticsService statisticsService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User owner;
    private AppUserPrincipal principal;

    @BeforeEach
    void createOwner() {
        owner = registerUser();
        principal = new AppUserPrincipal(owner);
    }

    // Registracija, prijava i zaštita stranica

    @Test
    void anonymousVisitorIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void registrationHashesPasswordAndAllowsLogin() throws Exception {
        String username = "novi" + SEQUENCE.incrementAndGet();
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Novi Korisnik").param("username", username)
                        .param("email", username + "@example.com")
                        .param("password", "Lozinka123").param("confirmPassword", "Lozinka123"))
                .andExpect(redirectedUrl("/login"));

        User saved = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Lozinka123").startsWith("$2");
        assertThat(passwordEncoder.matches("Lozinka123", saved.getPasswordHash())).isTrue();

        mvc.perform(formLogin("/login").user(username).password("Lozinka123")).andExpect(redirectedUrl("/dashboard"));
        mvc.perform(formLogin("/login").user(username).password("pogresna")).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void registrationRejectsInvalidAndDuplicateData() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Neko").param("username", owner.getUsername())
                        .param("email", "drugi" + SEQUENCE.incrementAndGet() + "@example.com")
                        .param("password", "Lozinka123").param("confirmPassword", "Lozinka123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("zauzeto")));

        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "").param("username", "ab").param("email", "nije-email")
                        .param("password", "kratka").param("confirmPassword", "druga"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lozinke se ne podudaraju")));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/teams").with(user(principal)).param("teamName", "X")).andExpect(status().isForbidden());
    }

    // Izolacija podataka

    @Test
    void userCannotSeeOrChangeAnotherUsersData() throws Exception {
        NationalTeam foreignTeam = createTeam(owner);
        Player foreignPlayer = createPlayer(foreignTeam, 1, PlayerPosition.GOALKEEPER);
        Match foreignMatch = createScheduledMatch(foreignTeam, createTeam(owner));
        Competition foreignCompetition = competitionService.create(competitionForm(), owner.getId());

        AppUserPrincipal stranger = new AppUserPrincipal(registerUser());

        for (String url : List.of("/teams/" + foreignTeam.getId(), "/teams/" + foreignTeam.getId() + "/edit",
                "/teams/" + foreignTeam.getId() + "/players/new", "/players/" + foreignPlayer.getId(),
                "/players/" + foreignPlayer.getId() + "/edit", "/matches/" + foreignMatch.getId(),
                "/matches/" + foreignMatch.getId() + "/edit",
                "/matches/" + foreignMatch.getId() + "/lineups/" + foreignTeam.getId() + "/edit",
                "/competitions/" + foreignCompetition.getId())) {
            mvc.perform(get(url).with(user(stranger))).andExpect(status().isNotFound());
        }
        for (String url : List.of("/teams/" + foreignTeam.getId() + "/delete", "/players/" + foreignPlayer.getId() + "/delete",
                "/matches/" + foreignMatch.getId() + "/delete", "/competitions/" + foreignCompetition.getId() + "/delete")) {
            mvc.perform(post(url).with(user(stranger)).with(csrf())).andExpect(status().isNotFound());
        }
        assertThat(teamService.findAll(owner.getId())).isNotEmpty();
        assertThat(playerRepository.findById(foreignPlayer.getId())).isPresent();
        assertThat(matchRepository.findById(foreignMatch.getId())).isPresent();
    }

    @Test
    void listsContainOnlyOwnData() throws Exception {
        NationalTeam team = createTeam(owner);
        createPlayer(team, 7, PlayerPosition.FORWARD);
        AppUserPrincipal stranger = new AppUserPrincipal(registerUser());

        mvc.perform(get("/teams").with(user(stranger)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(team.getTeamName()))));
        mvc.perform(get("/players").with(user(stranger)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(team.getTeamName()))));
        assertThat(statisticsService.getOverallStatistics(stranger.getId()).teamCount()).isZero();
    }

    // Igrači

    @Test
    void playerRulesAreEnforced() throws Exception {
        NationalTeam team = createTeam(owner);
        createPlayer(team, 10, PlayerPosition.FORWARD);

        assertThatThrownBy(() -> createPlayer(team, 10, PlayerPosition.DEFENDER))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.player.jerseyTaken");

        String url = "/teams/" + team.getId() + "/players";
        expectFormError(playerRequest(url, "0", "1990-01-01"), "između 1 i 99");
        expectFormError(playerRequest(url, "100", "1990-01-01"), "između 1 i 99");
        expectFormError(playerRequest(url, "11", LocalDate.now().plusDays(1).toString()), "u prošlosti");
        assertThat(playerRepository.countByTeamId(team.getId())).isEqualTo(1);
    }

    @Test
    void playerIsSavedWithoutWeightAndKeepsOtherData() throws Exception {
        NationalTeam team = createTeam(owner);
        mvc.perform(playerRequest("/teams/" + team.getId() + "/players", "7", "1995-03-01").param("club", "Klub Test"))
                .andExpect(redirectedUrl("/teams/" + team.getId()));

        Player player = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId()).get(0);
        assertThat(player.getHeightCm()).isEqualTo(180);
        assertThat(player.getJerseyNumber()).isEqualTo(7);
        assertThat(player.getPosition()).isEqualTo(PlayerPosition.DEFENDER);
        assertThat(player.getClub()).isEqualTo("Klub Test");

        for (String url : List.of("/teams/" + team.getId() + "/players/new", "/players/" + player.getId() + "/edit",
                "/players/" + player.getId(), "/players", "/teams/" + team.getId())) {
            mvc.perform(get(url).with(user(principal))).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("weightKg"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Težina"))));
        }
    }

    @Test
    void teamCannotHaveMoreThan30Players() {
        NationalTeam team = createTeam(owner);
        for (int number = 1; number <= 30; number++) {
            createPlayer(team, number, PlayerPosition.DEFENDER);
        }
        assertThat(playerRepository.countByTeamId(team.getId())).isEqualTo(30);
        assertThatThrownBy(() -> createPlayer(team, 31, PlayerPosition.DEFENDER))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.player.limit");
        assertThat(playerRepository.countByTeamId(team.getId())).isEqualTo(30);
    }

    @Test
    void thirtyFirstPlayerIsRejectedThroughHttpAndDeletionFreesPlace() throws Exception {
        NationalTeam team = createTeam(owner);
        for (int number = 1; number <= 29; number++) {
            createPlayer(team, number, PlayerPosition.DEFENDER);
        }
        String details = "/teams/" + team.getId();
        mvc.perform(get(details).with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29/30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(details + "/players/new")));

        // 30. igrač još ulazi
        mvc.perform(playerRequest(details + "/players", "30", "1995-03-01")).andExpect(redirectedUrl(details));

        mvc.perform(get(details).with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("30/30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reprezentacija može imati maksimalno 30 igrača.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"" + details + "/players/new\""))));
        mvc.perform(get("/players").with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(team.getTeamName() + " (30/30)")));

        // 31. igrač se odbija i u servisu, bez obzira na interfejs
        mvc.perform(playerRequest(details + "/players", "31", "1995-03-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reprezentacija može imati maksimalno 30 igrača.")));
        mvc.perform(get(details + "/players/new").with(user(principal)).header("Referer", "http://localhost" + details))
                .andExpect(redirectedUrl(details))
                .andExpect(flash().attribute("errorMessage", "Reprezentacija može imati maksimalno 30 igrača."));
        assertThat(playerRepository.countByTeamId(team.getId())).isEqualTo(30);

        // Brisanje igrača oslobađa mjesto
        Player removable = playerRepository.findByTeamIdOrderByJerseyNumberAsc(team.getId()).get(0);
        mvc.perform(post("/players/" + removable.getId() + "/delete").with(user(principal)).with(csrf()))
                .andExpect(redirectedUrl(details));
        mvc.perform(get(details).with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("29/30")));
        mvc.perform(playerRequest(details + "/players", "31", "1995-03-01")).andExpect(redirectedUrl(details));
        assertThat(playerRepository.countByTeamId(team.getId())).isEqualTo(30);
    }

    @Test
    void playerInLineupStillCannotBeDeletedToFreePlace() {
        Squad squad = createSquad(createTeam(owner));
        Match match = createScheduledMatch(squad.team, createTeam(owner));
        lineupService.save(match.getId(), squad.team.getId(), lineupForm(squad), owner.getId());
        assertThatThrownBy(() -> playerService.delete(squad.keeper.getId(), owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.player.inUse");
    }

    @Test
    void jerseyNumberCanRepeatInDifferentTeamsAndStayOnEdit() {
        NationalTeam first = createTeam(owner);
        NationalTeam second = createTeam(owner);
        Player player = createPlayer(first, 9, PlayerPosition.FORWARD);
        createPlayer(second, 9, PlayerPosition.FORWARD);

        PlayerForm form = PlayerForm.from(player);
        form.setClub("Novi klub");
        playerService.update(player.getId(), form, owner.getId());
        assertThat(playerRepository.findById(player.getId()).orElseThrow().getClub()).isEqualTo("Novi klub");
    }

    @Test
    void playerSearchFiltersByEveryCriterion() throws Exception {
        NationalTeam team = createTeam(owner);
        Player keeper = createPlayer(team, 1, PlayerPosition.GOALKEEPER);
        createPlayer(team, 9, PlayerPosition.FORWARD);

        mvc.perform(get("/players").param("jerseyNumber", "1").with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/players/" + keeper.getId() + "/edit")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">9<"))));
        mvc.perform(get("/players").param("position", "FORWARD").with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/players/" + keeper.getId() + "/edit"))));
        mvc.perform(get("/players").param("firstName", "IME").param("lastName", "prez").with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/players/" + keeper.getId() + "/edit")));
        mvc.perform(get("/players").param("jerseyNumber", "nije-broj").with(user(principal))).andExpect(status().isOk());
    }

    @Test
    void photoIsValidatedStoredAndServedOnlyToOwner() throws Exception {
        NationalTeam team = createTeam(owner);
        Player player = createPlayer(team, 5, PlayerPosition.MIDFIELDER);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13};

        mvc.perform(playerPhotoRequest(player, new MockMultipartFile("photo", "x.png", "image/png", "nije slika".getBytes())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JPG, PNG ili WEBP")));

        mvc.perform(playerPhotoRequest(player, new MockMultipartFile("photo", "x.png", "image/png", png)))
                .andExpect(redirectedUrl("/players/" + player.getId()));

        mvc.perform(get("/players/" + player.getId() + "/photo").with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/players/" + player.getId() + "/photo").with(user(new AppUserPrincipal(registerUser()))))
                .andExpect(status().isNotFound());
    }

    // Utakmice

    @Test
    void matchRulesAreEnforced() throws Exception {
        NationalTeam home = createTeam(owner);
        NationalTeam away = createTeam(owner);
        String future = LocalDateTime.now().plusDays(10).withNano(0).withSecond(0).toString();
        String past = LocalDateTime.now().minusDays(10).withNano(0).withSecond(0).toString();

        expectFormError(matchRequest(home, home, future, "SCHEDULED", "", ""), "protiv same sebe");
        expectFormError(matchRequest(home, away, future, "FINISHED", "1", "0"), "još nije počela");
        expectFormError(matchRequest(home, away, past, "SCHEDULED", "", ""), "u budućnosti");
        expectFormError(matchRequest(home, away, past, "FINISHED", "-1", "0"), "negativan");
        expectFormError(matchRequest(home, away, past, "FINISHED", "", ""), "mora imati unesen rezultat");
        expectFormError(matchRequest(home, away, "", "SCHEDULED", "", ""), "Unesite datum");

        NationalTeam foreign = createTeam(registerUser());
        mvc.perform(matchRequest(home, foreign, future, "SCHEDULED", "", "")).andExpect(status().isNotFound());
        assertThat(matchRepository.existsByTeam(home.getId())).isFalse();
    }

    @Test
    void teamCannotPlayTwiceOnSameDayAndMatchStaysInCompetitionPeriod() {
        NationalTeam first = createTeam(owner);
        NationalTeam second = createTeam(owner);
        NationalTeam third = createTeam(owner);
        LocalDateTime kickoff = LocalDateTime.now().plusDays(30).withHour(12);
        matchService.create(matchForm(first, second, kickoff, MatchStatus.SCHEDULED, null, null), owner.getId());

        assertThatThrownBy(() -> matchService.create(
                matchForm(first, third, kickoff.plusHours(3), MatchStatus.SCHEDULED, null, null), owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.dayConflict");

        Competition competition = competitionService.create(competitionForm(), owner.getId());
        MatchForm outside = matchForm(first, third, LocalDateTime.now().plusYears(5), MatchStatus.SCHEDULED, null, null);
        outside.setCompetitionId(competition.getId());
        assertThatThrownBy(() -> matchService.create(outside, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.outsideCompetition");
    }

    @Test
    void matchListFiltersByTeamStatusAndView() throws Exception {
        NationalTeam home = createTeam(owner);
        NationalTeam away = createTeam(owner);
        NationalTeam other = createTeam(owner);
        Match finished = createFinishedMatch(home, away, 2, 1);
        Match scheduled = createScheduledMatch(other, away);

        String finishedLink = "/matches/" + finished.getId() + "\"";
        String scheduledLink = "/matches/" + scheduled.getId() + "\"";

        expectMatchList("view", "finished", finishedLink, scheduledLink);
        expectMatchList("view", "upcoming", scheduledLink, finishedLink);
        expectMatchList("status", "FINISHED", finishedLink, scheduledLink);
        expectMatchList("teamId", home.getId().toString(), finishedLink, scheduledLink);
    }

    // Sastavi

    @Test
    void validLineupIsSavedAndCanBeResavedWithSamePlayers() throws Exception {
        Squad squad = createSquad(createTeam(owner));
        Match match = createScheduledMatch(squad.team, createTeam(owner));
        LineupForm form = lineupForm(squad);
        form.setSubstituteIds(new ArrayList<>(List.of(squad.bench().get(0).getId())));

        lineupService.save(match.getId(), squad.team.getId(), form, owner.getId());

        // Ponovno čuvanje ne smije narušiti jedinstveni ključ (lineupId, playerId)
        form.setFormation(Formation.F_3_5_2);
        form.setTactic("Pritisak na protivničkoj polovini");
        lineupService.save(match.getId(), squad.team.getId(), form, owner.getId());

        mvc.perform(get("/matches/" + match.getId() + "/lineups/" + squad.team.getId()).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("3-5-2")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pitch-captain")));
    }

    @Test
    void lineupRulesAreEnforced() {
        Squad squad = createSquad(createTeam(owner));
        Squad otherSquad = createSquad(createTeam(owner));
        Match match = createScheduledMatch(squad.team, createTeam(owner));
        Long teamId = squad.team.getId();

        LineupForm tenPlayers = lineupForm(squad);
        tenPlayers.getStarterIds().set(10, null);
        expectLineupError(match, teamId, tenPlayers, "error.lineup.startersCount");

        LineupForm duplicate = lineupForm(squad);
        duplicate.getStarterIds().set(10, duplicate.getStarterIds().get(9));
        expectLineupError(match, teamId, duplicate, "error.lineup.duplicate");

        LineupForm benchAndStarter = lineupForm(squad);
        benchAndStarter.setSubstituteIds(new ArrayList<>(List.of(benchAndStarter.getStarterIds().get(5))));
        expectLineupError(match, teamId, benchAndStarter, "error.lineup.duplicate");

        LineupForm outfieldInGoal = lineupForm(squad);
        java.util.Collections.swap(outfieldInGoal.getStarterIds(), 0, 1);
        expectLineupError(match, teamId, outfieldInGoal, "error.lineup.goalkeeper");

        LineupForm secondKeeperInField = lineupForm(squad);
        secondKeeperInField.getStarterIds().set(10, squad.secondKeeper.getId());
        expectLineupError(match, teamId, secondKeeperInField, "error.lineup.goalkeeper");

        LineupForm foreignPlayer = lineupForm(squad);
        foreignPlayer.getStarterIds().set(10, otherSquad.outfield.get(0).getId());
        expectLineupError(match, teamId, foreignPlayer, "error.lineup.foreignPlayer");

        LineupForm captainOnBench = lineupForm(squad);
        captainOnBench.setCaptainId(squad.secondKeeper.getId());
        expectLineupError(match, teamId, captainOnBench, "error.lineup.captain");

        LineupForm tooManySubstitutes = lineupForm(squad);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            ids.add((long) i + 1);
        }
        tooManySubstitutes.setSubstituteIds(ids);
        expectLineupError(match, teamId, tooManySubstitutes, "error.lineup.maxSubstitutes");

        assertThatThrownBy(() -> lineupService.save(match.getId(), otherSquad.team.getId(), lineupForm(otherSquad), owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lineupFormRendersAndExplainsTooSmallSquad() throws Exception {
        Squad squad = createSquad(createTeam(owner));
        NationalTeam small = createTeam(owner);
        Match match = createScheduledMatch(squad.team, small);

        mvc.perform(get("/matches/" + match.getId() + "/lineups/" + squad.team.getId() + "/edit").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FORMATION_SLOTS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("F_4_2_3_1")));
        mvc.perform(get("/matches/" + match.getId() + "/lineups/" + small.getId() + "/edit").with(user(principal)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("najmanje jedan golman")));
    }

    // Događaji

    @Test
    void goalEventsKeepScoreConsistent() throws Exception {
        Squad home = createSquad(createTeam(owner));
        NationalTeam away = createTeam(owner);
        Player awayPlayer = createPlayer(away, 9, PlayerPosition.FORWARD);
        Match match = createFinishedMatch(home.team, away, 0, 0);
        Player scorer = home.outfield.get(9);
        Player assistant = home.outfield.get(8);

        assertThatThrownBy(() -> addEvent(match, assistant, 5, EventType.ASSIST))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.event.assistBeforeGoal");

        addEvent(match, scorer, 40, EventType.GOAL);
        addEvent(match, assistant, 40, EventType.ASSIST);
        addEvent(match, awayPlayer, 10, EventType.YELLOW_CARD);
        assertScore(match, 1, 0);

        mvc.perform(post("/matches/" + match.getId() + "/result").with(user(principal)).with(csrf())
                        .param("homeGoals", "0").param("awayGoals", "0").param("finished", "true"))
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("evidentiranih golova")));
        assertScore(match, 1, 0);

        var events = eventRepository.findByMatchIdOrderByMinuteAscIdAsc(match.getId());
        assertThat(events).extracting(event -> event.getMinute()).containsExactly(10, 40, 40);

        var goal = events.stream().filter(event -> event.getType() == EventType.GOAL).findFirst().orElseThrow();
        assertThatThrownBy(() -> eventService.delete(match.getId(), goal.getId(), owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.event.assistWithGoal");

        var assist = events.stream().filter(event -> event.getType() == EventType.ASSIST).findFirst().orElseThrow();
        eventService.delete(match.getId(), assist.getId(), owner.getId());
        eventService.delete(match.getId(), goal.getId(), owner.getId());
        assertScore(match, 0, 0);
    }

    @Test
    void eventRulesAreEnforced() {
        NationalTeam home = createTeam(owner);
        NationalTeam away = createTeam(owner);
        Player player = createPlayer(home, 8, PlayerPosition.MIDFIELDER);
        Player outsider = createPlayer(createTeam(owner), 8, PlayerPosition.MIDFIELDER);

        Match scheduled = createScheduledMatch(home, away);
        assertThatThrownBy(() -> addEvent(scheduled, player, 10, EventType.GOAL))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.event.matchNotStarted");

        Match finished = createFinishedMatch(home, away, 0, 0);
        assertThatThrownBy(() -> addEvent(finished, outsider, 10, EventType.GOAL))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.event.playerNotInMatch");

        addEvent(finished, player, 10, EventType.YELLOW_CARD);
        addEvent(finished, player, 20, EventType.YELLOW_CARD);
        assertThatThrownBy(() -> addEvent(finished, player, 30, EventType.YELLOW_CARD))
                .isInstanceOf(BusinessRuleException.class);
        addEvent(finished, player, 21, EventType.RED_CARD);
        assertThatThrownBy(() -> addEvent(finished, player, 22, EventType.RED_CARD))
                .isInstanceOf(BusinessRuleException.class);

        MatchForm backToScheduled = MatchForm.from(matchService.getOwned(finished.getId(), owner.getId()));
        backToScheduled.setKickoff(LocalDateTime.now().plusDays(200));
        backToScheduled.setStatus(MatchStatus.SCHEDULED);
        assertThatThrownBy(() -> matchService.update(finished.getId(), backToScheduled, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.match.hasEvents");
    }

    // Statistika

    @Test
    void statisticsAreCalculatedFromStoredData() {
        Squad home = createSquad(createTeam(owner));
        Squad away = createSquad(createTeam(owner));
        Player scorer = home.outfield.get(9);

        Match first = createFinishedMatch(home.team, away.team, 0, 0);
        addEvent(first, scorer, 15, EventType.GOAL);
        addEvent(first, scorer, 60, EventType.GOAL);
        lineupService.save(first.getId(), home.team.getId(), lineupForm(home), owner.getId());
        LineupForm withBench = lineupForm(away);
        withBench.setSubstituteIds(new ArrayList<>(List.of(away.secondKeeper.getId())));
        lineupService.save(first.getId(), away.team.getId(), withBench, owner.getId());
        addEvent(first, away.secondKeeper, 70, EventType.SUBSTITUTION);

        Match second = createFinishedMatch(home.team, away.team, 1, 1);
        lineupService.save(second.getId(), home.team.getId(), lineupForm(home), owner.getId());
        createScheduledMatch(home.team, away.team);

        OverallStatistics stats = statisticsService.getOverallStatistics(owner.getId());

        assertThat(stats.teamCount()).isEqualTo(2);
        assertThat(stats.playerCount()).isEqualTo(24);
        assertThat(stats.matchCount()).isEqualTo(3);
        assertThat(stats.playedCount()).isEqualTo(2);
        assertThat(stats.wins()).isEqualTo(1);
        assertThat(stats.losses()).isEqualTo(1);
        assertThat(stats.draws()).isEqualTo(1);
        assertThat(stats.goalsScored()).isEqualTo(4);
        assertThat(stats.bestScorer().playerId()).isEqualTo(scorer.getId());
        assertThat(stats.bestScorer().goals()).isEqualTo(2);
        assertThat(stats.mostCapped().appearances()).isEqualTo(2);

        var homeStats = stats.teams().get(0);
        assertThat(homeStats.teamId()).isEqualTo(home.team.getId());
        assertThat(homeStats.points()).isEqualTo(4);
        assertThat(homeStats.goalsFor()).isEqualTo(3);
        assertThat(homeStats.goalsAgainst()).isEqualTo(1);

        assertThat(stats.players().stream().filter(p -> p.playerId().equals(away.secondKeeper.getId())).findFirst().orElseThrow()
                .appearances()).isEqualTo(1);
    }

    // Brisanje povezanih podataka

    @Test
    void deletionKeepsDataConsistent() throws Exception {
        Squad squad = createSquad(createTeam(owner));
        NationalTeam away = createTeam(owner);
        Match match = createFinishedMatch(squad.team, away, 0, 0);
        lineupService.save(match.getId(), squad.team.getId(), lineupForm(squad), owner.getId());
        Player starter = squad.outfield.get(0);
        addEvent(match, starter, 12, EventType.GOAL);

        mvc.perform(post("/teams/" + squad.team.getId() + "/delete").with(user(principal)).with(csrf())
                        .header("Referer", "http://localhost/teams"))
                .andExpect(redirectedUrl("/teams"))
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("utakmice")));

        mvc.perform(post("/players/" + starter.getId() + "/delete").with(user(principal)).with(csrf()))
                .andExpect(flash().attribute("errorMessage", org.hamcrest.Matchers.containsString("ne može se obrisati")));
        assertThat(playerRepository.findById(starter.getId())).isPresent();

        matchService.delete(match.getId(), owner.getId());
        assertThat(eventRepository.findByMatchIdOrderByMinuteAscIdAsc(match.getId())).isEmpty();

        playerService.delete(starter.getId(), owner.getId());
        teamService.delete(squad.team.getId(), owner.getId());
        assertThat(playerRepository.countByTeamId(squad.team.getId())).isZero();
    }

    @Test
    void deletingCompetitionTurnsMatchesIntoFriendlies() {
        NationalTeam home = createTeam(owner);
        NationalTeam away = createTeam(owner);
        Competition competition = competitionService.create(competitionForm(), owner.getId());
        MatchForm form = matchForm(home, away, LocalDateTime.now().plusDays(20), MatchStatus.SCHEDULED, null, null);
        form.setCompetitionId(competition.getId());
        Match match = matchService.create(form, owner.getId());

        CompetitionForm tooShort = competitionForm();
        tooShort.setEndDate(tooShort.getStartDate());
        assertThatThrownBy(() -> competitionService.update(competition.getId(), tooShort, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("error.competition.matchesOutside");

        competitionService.delete(competition.getId(), owner.getId());
        assertThat(matchService.getOwned(match.getId(), owner.getId()).getCompetition()).isNull();
    }

    // Prikaz stranica

    @Test
    void everyPageRendersWithRealData() throws Exception {
        Squad home = createSquad(createTeam(owner));
        Squad away = createSquad(createTeam(owner));
        Competition competition = competitionService.create(competitionForm(), owner.getId());
        Match finished = createFinishedMatch(home.team, away.team, 2, 1);
        lineupService.save(finished.getId(), home.team.getId(), lineupForm(home), owner.getId());
        addEvent(finished, home.outfield.get(9), 33, EventType.YELLOW_CARD);
        Match scheduled = createScheduledMatch(home.team, away.team);
        Player player = home.outfield.get(0);

        for (String url : List.of("/dashboard", "/statistics", "/teams", "/teams/new", "/teams/" + home.team.getId(),
                "/teams/" + home.team.getId() + "/edit", "/teams/" + home.team.getId() + "/players/new", "/players",
                "/players/" + player.getId(), "/players/" + player.getId() + "/edit", "/matches",
                "/matches?view=upcoming", "/matches?view=finished", "/matches/new", "/matches/" + finished.getId(),
                "/matches/" + finished.getId() + "/edit", "/matches/" + scheduled.getId(),
                "/matches/" + finished.getId() + "/lineups/" + home.team.getId(),
                "/matches/" + finished.getId() + "/lineups/" + home.team.getId() + "/edit", "/competitions",
                "/competitions/new", "/competitions/" + competition.getId(), "/competitions/" + competition.getId() + "/edit")) {
            mvc.perform(get(url).with(user(principal))).andExpect(status().isOk());
        }
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/teams/abc").with(user(principal))).andExpect(status().isBadRequest());
        mvc.perform(get("/nepostojeca-stranica").with(user(principal))).andExpect(status().isNotFound());
    }

    @Test
    void emptyAccountSeesHelpfulEmptyPages() throws Exception {
        for (String url : List.of("/dashboard", "/statistics", "/teams", "/players", "/matches", "/matches/new", "/competitions")) {
            mvc.perform(get(url).with(user(principal))).andExpect(status().isOk());
        }
    }

    // Pomoćne metode

    private record Squad(NationalTeam team, Player keeper, Player secondKeeper, List<Player> outfield) {
        List<Player> bench() {
            return List.of(secondKeeper);
        }
    }

    private User registerUser() {
        int id = SEQUENCE.incrementAndGet();
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Korisnik " + id);
        form.setUsername("korisnik" + id);
        form.setEmail("korisnik" + id + "@example.com");
        form.setPassword("Lozinka123");
        form.setConfirmPassword("Lozinka123");
        return userService.register(form);
    }

    private NationalTeam createTeam(User teamOwner) {
        int id = SEQUENCE.incrementAndGet();
        NationalTeamForm form = new NationalTeamForm();
        form.setCountryName("Država " + id);
        form.setTeamName("Reprezentacija " + id);
        form.setConfederation(Confederation.UEFA);
        form.setCoach("Selektor " + id);
        form.setFoundedYear(1950);
        return teamService.create(form, teamOwner.getId());
    }

    private Player createPlayer(NationalTeam team, int jersey, PlayerPosition position) {
        PlayerForm form = new PlayerForm();
        form.setFirstName("Ime" + jersey);
        form.setLastName("Prezime" + jersey);
        form.setBirthDate(LocalDate.of(1998, 5, 20));
        form.setNationality("BiH");
        form.setPosition(position);
        form.setJerseyNumber(jersey);
        form.setHeightCm(180);
        form.setClub("Klub");
        return playerService.create(team.getId(), form, team.getOwner().getId());
    }

    private Squad createSquad(NationalTeam team) {
        Player keeper = createPlayer(team, 1, PlayerPosition.GOALKEEPER);
        Player secondKeeper = createPlayer(team, 12, PlayerPosition.GOALKEEPER);
        List<Player> outfield = new ArrayList<>();
        for (int number = 2; number <= 11; number++) {
            PlayerPosition position = number <= 5 ? PlayerPosition.DEFENDER
                    : number <= 8 ? PlayerPosition.MIDFIELDER : PlayerPosition.FORWARD;
            outfield.add(createPlayer(team, number, position));
        }
        return new Squad(team, keeper, secondKeeper, outfield);
    }

    private LineupForm lineupForm(Squad squad) {
        LineupForm form = new LineupForm();
        form.getStarterIds().set(0, squad.keeper.getId());
        for (int i = 0; i < squad.outfield.size(); i++) {
            form.getStarterIds().set(i + 1, squad.outfield.get(i).getId());
        }
        form.setCaptainId(squad.outfield.get(0).getId());
        return form;
    }

    private MatchForm matchForm(NationalTeam home, NationalTeam away, LocalDateTime kickoff, MatchStatus status,
                                Integer homeGoals, Integer awayGoals) {
        MatchForm form = new MatchForm();
        form.setHomeTeamId(home.getId());
        form.setAwayTeamId(away.getId());
        form.setKickoff(kickoff);
        form.setStadium("Stadion");
        form.setStatus(status);
        form.setHomeGoals(homeGoals);
        form.setAwayGoals(awayGoals);
        return form;
    }

    private Match createScheduledMatch(NationalTeam home, NationalTeam away) {
        LocalDateTime kickoff = LocalDateTime.now().plusDays(100L + SEQUENCE.incrementAndGet());
        return matchService.create(matchForm(home, away, kickoff, MatchStatus.SCHEDULED, null, null), home.getOwner().getId());
    }

    private Match createFinishedMatch(NationalTeam home, NationalTeam away, int homeGoals, int awayGoals) {
        LocalDateTime kickoff = LocalDateTime.now().minusDays(100L + SEQUENCE.incrementAndGet());
        return matchService.create(matchForm(home, away, kickoff, MatchStatus.FINISHED, homeGoals, awayGoals), home.getOwner().getId());
    }

    private CompetitionForm competitionForm() {
        CompetitionForm form = new CompetitionForm();
        form.setName("Kup");
        form.setSeason("2026/27");
        form.setType(CompetitionType.FRIENDLY_TOURNAMENT);
        form.setStartDate(LocalDate.now());
        form.setEndDate(LocalDate.now().plusYears(1));
        return form;
    }

    private void addEvent(Match match, Player player, int minute, EventType type) {
        MatchEventForm form = new MatchEventForm();
        form.setPlayerId(player.getId());
        form.setMinute(minute);
        form.setType(type);
        eventService.add(match.getId(), form, owner.getId());
    }

    private void assertScore(Match match, int home, int away) {
        Match stored = matchService.getOwned(match.getId(), owner.getId());
        assertThat(stored.getHomeGoals()).isEqualTo(home);
        assertThat(stored.getAwayGoals()).isEqualTo(away);
    }

    private void expectLineupError(Match match, Long teamId, LineupForm form, String message) {
        assertThatThrownBy(() -> lineupService.save(match.getId(), teamId, form, owner.getId()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining(message);
    }

    private void expectMatchList(String param, String value, String expected, String unexpected) throws Exception {
        ResultActions result = mvc.perform(get("/matches").param(param, value).with(user(principal)));
        result.andExpect(content().string(org.hamcrest.Matchers.containsString(expected)))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(unexpected))));
    }

    private void expectFormError(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                 String message) throws Exception {
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(message)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder playerRequest(
            String url, String jersey, String birthDate) {
        return post(url).with(user(principal)).with(csrf())
                .param("firstName", "Ana").param("lastName", "Anić").param("birthDate", birthDate)
                .param("nationality", "BiH").param("position", "DEFENDER").param("jerseyNumber", jersey)
                .param("heightCm", "180");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder playerPhotoRequest(
            Player player, MockMultipartFile file) {
        return multipart("/players/" + player.getId()).file(file).with(user(principal)).with(csrf())
                .param("firstName", player.getFirstName()).param("lastName", player.getLastName())
                .param("birthDate", player.getBirthDate().toString()).param("nationality", player.getNationality())
                .param("position", player.getPosition().name()).param("jerseyNumber", player.getJerseyNumber().toString())
                .param("heightCm", "180");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder matchRequest(
            NationalTeam home, NationalTeam away, String kickoff, String status, String homeGoals, String awayGoals) {
        return post("/matches").with(user(principal)).with(csrf())
                .param("homeTeamId", home.getId().toString()).param("awayTeamId", away.getId().toString())
                .param("kickoff", kickoff.length() > 16 ? kickoff.substring(0, 16) : kickoff)
                .param("stadium", "Koševo").param("status", status)
                .param("homeGoals", homeGoals).param("awayGoals", awayGoals);
    }
}
