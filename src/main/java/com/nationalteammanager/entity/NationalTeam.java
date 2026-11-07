package com.nationalteammanager.entity;

import com.nationalteammanager.enums.Confederation;
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
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "NationalTeams", uniqueConstraints =
        @UniqueConstraint(name = "UQ_NationalTeams_owner_name", columnNames = {"ownerId", "teamName"}))
public class NationalTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ownerId", nullable = false, foreignKey = @ForeignKey(name = "FK_NationalTeams_Users"))
    private User owner;

    @Column(nullable = false, length = 100)
    private String countryName;

    @Column(nullable = false, length = 100)
    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Confederation confederation;

    @Column(nullable = false, length = 100)
    private String coach;

    @Column(length = 100)
    private String coachPhotoFileName;

    /** Vlastita slika zastave; ako je nema, koristi se zastava države iz lokalne biblioteke. */
    @Column(length = 100)
    private String flagFileName;

    /** Korisnik je izabrao da reprezentacija nema zastavu, pa se ne prikazuje ni automatska. */
    @Column(nullable = false)
    private boolean flagRemoved;

    @Column(nullable = false)
    private Integer foundedYear;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Stadium> stadiums = new ArrayList<>();

    protected NationalTeam() {
    }

    public NationalTeam(User owner) {
        this.owner = owner;
    }

    public void updateDetails(String countryName, String teamName, Confederation confederation,
                              String coach, Integer foundedYear, String description) {
        this.countryName = countryName;
        this.teamName = teamName;
        this.confederation = confederation;
        this.coach = coach;
        this.foundedYear = foundedYear;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getTeamName() {
        return teamName;
    }

    public Confederation getConfederation() {
        return confederation;
    }

    public String getCoach() {
        return coach;
    }

    public String getCoachPhotoFileName() {
        return coachPhotoFileName;
    }

    public void setCoachPhotoFileName(String coachPhotoFileName) {
        this.coachPhotoFileName = coachPhotoFileName;
    }

    public String getFlagFileName() {
        return flagFileName;
    }

    public boolean isFlagRemoved() {
        return flagRemoved;
    }

    public void updateFlag(String flagFileName, boolean flagRemoved) {
        this.flagFileName = flagFileName;
        this.flagRemoved = flagRemoved;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public String getDescription() {
        return description;
    }

    public List<Stadium> getStadiums() {
        return stadiums;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
