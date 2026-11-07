package com.nationalteammanager;

import com.nationalteammanager.dto.CompetitionForm;
import com.nationalteammanager.dto.LineupForm;
import com.nationalteammanager.dto.MatchEventForm;
import com.nationalteammanager.dto.MatchForm;
import com.nationalteammanager.dto.NationalTeamForm;
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
import com.nationalteammanager.service.CompetitionService;
import com.nationalteammanager.service.LineupService;
import com.nationalteammanager.service.MatchEventService;
import com.nationalteammanager.service.MatchService;
import com.nationalteammanager.service.NationalTeamService;
import com.nationalteammanager.service.PlayerService;
import com.nationalteammanager.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Puni bazu u memoriji za ručni pregled interfejsa (profil ui-demo); nije dio aplikacije. */
@Component
@Profile("ui-demo")
class UiDemoDataLoader implements CommandLineRunner {

    private static final String[] LAST_NAMES = {"Hadžić", "Kovačević", "Marić", "Šarić", "Delić", "Begić", "Tomić",
            "Jurić", "Ćosić", "Lukić", "Petrović", "Zukić", "Babić", "Đurić", "Mehić", "Ilić"};

    private final UserService userService;
    private final NationalTeamService teamService;
    private final PlayerService playerService;
    private final CompetitionService competitionService;
    private final MatchService matchService;
    private final LineupService lineupService;
    private final MatchEventService eventService;

    UiDemoDataLoader(UserService userService, NationalTeamService teamService, PlayerService playerService,
                     CompetitionService competitionService, MatchService matchService, LineupService lineupService,
                     MatchEventService eventService) {
        this.userService = userService;
        this.teamService = teamService;
        this.playerService = playerService;
        this.competitionService = competitionService;
        this.matchService = matchService;
        this.lineupService = lineupService;
        this.eventService = eventService;
    }

    @Override
    public void run(String... args) {
        RegistrationForm registration = new RegistrationForm();
        registration.setFullName("Demo Korisnik");
        registration.setUsername("demo");
        registration.setEmail("demo@example.com");
        registration.setPassword("Demo1234!");
        registration.setConfirmPassword("Demo1234!");
        User user = userService.register(registration);
        Long ownerId = user.getId();

        NationalTeam bih = team("Bosna i Hercegovina", "Zmajevi", "Anel Selimović", ownerId);
        NationalTeam hrv = team("Hrvatska", "Vatreni", "Ivan Horvat", ownerId);
        NationalTeam srb = team("Srbija", "Orlovi", "Marko Stanković", ownerId);
        List<Player> bihPlayers = squad(bih, ownerId);
        List<Player> hrvPlayers = squad(hrv, ownerId);
        squad(srb, ownerId);

        CompetitionForm cup = new CompetitionForm();
        cup.setName("Balkanski kup");
        cup.setSeason("2026/27");
        cup.setType(CompetitionType.FRIENDLY_TOURNAMENT);
        cup.setStartDate(LocalDate.now().minusMonths(2));
        cup.setEndDate(LocalDate.now().plusMonths(6));
        Competition competition = competitionService.create(cup, ownerId);

        Match finished = match(bih, hrv, competition, LocalDateTime.now().minusDays(20), MatchStatus.FINISHED, 1, 0, ownerId);
        lineup(finished, bih, bihPlayers, Formation.F_4_3_3, ownerId);
        lineup(finished, hrv, hrvPlayers, Formation.F_4_4_2, ownerId);
        event(finished, bihPlayers.get(10), 23, EventType.GOAL, ownerId);
        event(finished, bihPlayers.get(9), 23, EventType.ASSIST, ownerId);
        event(finished, hrvPlayers.get(5), 41, EventType.YELLOW_CARD, ownerId);
        event(finished, hrvPlayers.get(11), 78, EventType.GOAL, ownerId);
        event(finished, bihPlayers.get(12), 80, EventType.SUBSTITUTION, ownerId);

        match(hrv, srb, null, LocalDateTime.now().minusDays(10), MatchStatus.FINISHED, 1, 1, ownerId);
        match(srb, bih, competition, LocalDateTime.now().plusDays(12), MatchStatus.SCHEDULED, null, null, ownerId);
        match(bih, hrv, null, LocalDateTime.now().plusDays(40), MatchStatus.SCHEDULED, null, null, ownerId);
    }

    private NationalTeam team(String country, String name, String coach, Long ownerId) {
        NationalTeamForm form = new NationalTeamForm();
        form.setCountryName(country);
        form.setTeamName(name);
        form.setConfederation(Confederation.UEFA);
        form.setCoach(coach);
        form.setFoundedYear(1992);
        form.setDescription("Demo reprezentacija za pregled interfejsa.");
        return teamService.create(form, ownerId);
    }

    private List<Player> squad(NationalTeam team, Long ownerId) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < LAST_NAMES.length; i++) {
            PlayerPosition position = i == 0 || i == 11 ? PlayerPosition.GOALKEEPER
                    : i <= 4 || i == 12 ? PlayerPosition.DEFENDER
                    : i <= 7 || i == 13 ? PlayerPosition.MIDFIELDER : PlayerPosition.FORWARD;
            PlayerForm form = new PlayerForm();
            form.setFirstName("Igrač" + (i + 1));
            form.setLastName(LAST_NAMES[i]);
            form.setBirthDate(LocalDate.of(1995 + i % 8, 1 + i % 12, 10));
            form.setNationality(team.getCountryName());
            form.setPosition(position);
            form.setJerseyNumber(i + 1);
            form.setHeightCm(175 + i);
            form.setClub("FK Primjer");
            players.add(playerService.create(team.getId(), form, ownerId));
        }
        return players;
    }

    private Match match(NationalTeam home, NationalTeam away, Competition competition, LocalDateTime kickoff,
                        MatchStatus status, Integer homeGoals, Integer awayGoals, Long ownerId) {
        MatchForm form = new MatchForm();
        form.setHomeTeamId(home.getId());
        form.setAwayTeamId(away.getId());
        form.setCompetitionId(competition == null ? null : competition.getId());
        form.setKickoff(kickoff);
        form.setStadium("Stadion " + home.getCountryName());
        form.setStatus(status);
        form.setHomeGoals(homeGoals);
        form.setAwayGoals(awayGoals);
        return matchService.create(form, ownerId);
    }

    private void lineup(Match match, NationalTeam team, List<Player> players, Formation formation, Long ownerId) {
        LineupForm form = new LineupForm();
        form.setFormation(formation);
        form.setTactic("Visok presing i brza tranzicija preko krila.");
        form.getStarterIds().set(0, players.get(0).getId());
        for (int i = 1; i <= 10; i++) {
            form.getStarterIds().set(i, players.get(i).getId());
        }
        form.setCaptainId(players.get(5).getId());
        form.setSubstituteIds(new ArrayList<>(players.subList(11, players.size()).stream().map(Player::getId).toList()));
        lineupService.save(match.getId(), team.getId(), form, ownerId);
    }

    private void event(Match match, Player player, int minute, EventType type, Long ownerId) {
        MatchEventForm form = new MatchEventForm();
        form.setPlayerId(player.getId());
        form.setMinute(minute);
        form.setType(type);
        eventService.add(match.getId(), form, ownerId);
    }
}
