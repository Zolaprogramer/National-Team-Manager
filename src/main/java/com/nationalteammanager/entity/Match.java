package com.nationalteammanager.entity;

import com.nationalteammanager.enums.MatchStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "homeTeamId", nullable = false, foreignKey = @ForeignKey(name = "FK_Matches_HomeTeam"))
    private NationalTeam homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "awayTeamId", nullable = false, foreignKey = @ForeignKey(name = "FK_Matches_AwayTeam"))
    private NationalTeam awayTeam;

    /** Utakmica bez takmičenja je prijateljska. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitionId", foreignKey = @ForeignKey(name = "FK_Matches_Competitions"))
    private Competition competition;

    /** Stadion iz evidencije; utakmica može imati i samo upisan naziv, npr. za gostovanje na drugom stadionu. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadiumId", foreignKey = @ForeignKey(name = "FK_Matches_Stadiums"))
    private Stadium venue;

    @Column(nullable = false)
    private LocalDateTime kickoff;

    @Column(nullable = false, length = 100)
    private String stadium;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    private Integer homeGoals;

    private Integer awayGoals;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lineup> lineups = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchEvent> events = new ArrayList<>();

    public void updateDetails(NationalTeam homeTeam, NationalTeam awayTeam, Competition competition,
                              LocalDateTime kickoff, Stadium venue, String stadium) {
        this.venue = venue;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.competition = competition;
        this.kickoff = kickoff;
        this.stadium = stadium;
    }

    public void updateResult(MatchStatus status, Integer homeGoals, Integer awayGoals) {
        this.status = status;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    public void addGoalFor(NationalTeam team) {
        if (isHomeTeam(team)) {
            homeGoals = homeGoals + 1;
        } else {
            awayGoals = awayGoals + 1;
        }
    }

    public void removeGoalFor(NationalTeam team) {
        if (isHomeTeam(team)) {
            homeGoals = homeGoals - 1;
        } else {
            awayGoals = awayGoals - 1;
        }
    }

    public boolean involves(NationalTeam team) {
        return isHomeTeam(team) || awayTeam.getId().equals(team.getId());
    }

    public boolean isHomeTeam(NationalTeam team) {
        return homeTeam.getId().equals(team.getId());
    }

    public boolean isFinished() {
        return status == MatchStatus.FINISHED;
    }

    public void detachFromCompetition() {
        this.competition = null;
    }

    public Long getId() {
        return id;
    }

    public NationalTeam getHomeTeam() {
        return homeTeam;
    }

    public NationalTeam getAwayTeam() {
        return awayTeam;
    }

    public Competition getCompetition() {
        return competition;
    }

    public LocalDateTime getKickoff() {
        return kickoff;
    }

    /** Naziv za prikaz; stadion iz evidencije ima prednost, pa promjena njegovog naziva važi i ovdje. */
    public String getStadiumName() {
        return venue != null ? venue.getName() : stadium;
    }

    public Stadium getVenue() {
        return venue;
    }

    public String getStadium() {
        return stadium;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public List<Lineup> getLineups() {
        return lineups;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }
}
