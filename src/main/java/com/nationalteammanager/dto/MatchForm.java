package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.enums.MatchStatus;
import com.nationalteammanager.validation.DifferentTeams;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@DifferentTeams
public class MatchForm {

    @NotNull(message = "{validation.match.home.required}")
    private Long homeTeamId;

    @NotNull(message = "{validation.match.away.required}")
    private Long awayTeamId;

    private Long competitionId;

    @NotNull(message = "{validation.match.kickoff.required}")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime kickoff;

    private Long stadiumId;

    @Size(max = 100, message = "{validation.match.stadium.size}")
    private String stadium;

    @NotNull(message = "{validation.match.status.required}")
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Min(value = 0, message = "{validation.match.goals.negative}")
    private Integer homeGoals;

    @Min(value = 0, message = "{validation.match.goals.negative}")
    private Integer awayGoals;

    public static MatchForm from(Match match) {
        MatchForm form = new MatchForm();
        form.homeTeamId = match.getHomeTeam().getId();
        form.awayTeamId = match.getAwayTeam().getId();
        form.competitionId = match.getCompetition() == null ? null : match.getCompetition().getId();
        form.kickoff = match.getKickoff();
        form.stadiumId = match.getVenue() == null ? null : match.getVenue().getId();
        form.stadium = match.getVenue() == null ? match.getStadium() : null;
        form.status = match.getStatus();
        form.homeGoals = match.getHomeGoals();
        form.awayGoals = match.getAwayGoals();
        return form;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(Long homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(Long awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public LocalDateTime getKickoff() {
        return kickoff;
    }

    public void setKickoff(LocalDateTime kickoff) {
        this.kickoff = kickoff;
    }

    public Long getStadiumId() {
        return stadiumId;
    }

    public void setStadiumId(Long stadiumId) {
        this.stadiumId = stadiumId;
    }

    public String getStadium() {
        return stadium;
    }

    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }
}
