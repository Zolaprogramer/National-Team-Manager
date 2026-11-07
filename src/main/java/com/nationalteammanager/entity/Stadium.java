package com.nationalteammanager.entity;

import com.nationalteammanager.enums.StadiumSurface;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Stadiums", uniqueConstraints =
        @UniqueConstraint(name = "UQ_Stadiums_team_name", columnNames = {"teamId", "name"}))
public class Stadium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teamId", nullable = false, foreignKey = @ForeignKey(name = "FK_Stadiums_NationalTeams"))
    private NationalTeam team;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer builtYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StadiumSurface surface;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String photoFileName;

    @Column(nullable = false)
    private boolean mainStadium;

    protected Stadium() {
    }

    public Stadium(NationalTeam team) {
        this.team = team;
    }

    public void updateDetails(NationalTeam team, String name, String city, String country, Integer capacity,
                              Integer builtYear, StadiumSurface surface, String address, String description) {
        this.team = team;
        this.name = name;
        this.city = city;
        this.country = country;
        this.capacity = capacity;
        this.builtYear = builtYear;
        this.surface = surface;
        this.address = address;
        this.description = description;
    }

    public boolean belongsTo(NationalTeam otherTeam) {
        return team.getId().equals(otherTeam.getId());
    }

    public Long getId() {
        return id;
    }

    public NationalTeam getTeam() {
        return team;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getBuiltYear() {
        return builtYear;
    }

    public StadiumSurface getSurface() {
        return surface;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    public boolean isMainStadium() {
        return mainStadium;
    }

    public void setMainStadium(boolean mainStadium) {
        this.mainStadium = mainStadium;
    }
}
