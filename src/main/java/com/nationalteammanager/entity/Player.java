package com.nationalteammanager.entity;

import com.nationalteammanager.enums.PlayerPosition;
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

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "Players", uniqueConstraints =
        @UniqueConstraint(name = "UQ_Players_team_jersey", columnNames = {"teamId", "jerseyNumber"}))
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teamId", nullable = false, foreignKey = @ForeignKey(name = "FK_Players_NationalTeams"))
    private NationalTeam team;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 100)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "playerPosition", nullable = false, length = 20)
    private PlayerPosition position;

    @Column(nullable = false)
    private Integer jerseyNumber;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(length = 100)
    private String club;

    @Column(length = 100)
    private String photoFileName;

    protected Player() {
    }

    public Player(NationalTeam team) {
        this.team = team;
    }

    public void updateDetails(String firstName, String lastName, LocalDate birthDate, String nationality,
                              PlayerPosition position, Integer jerseyNumber, Integer heightCm,
                              String club) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.position = position;
        this.jerseyNumber = jerseyNumber;
        this.heightCm = heightCm;
        this.club = club;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getInitials() {
        return firstName.substring(0, 1) + lastName.substring(0, 1);
    }

    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public boolean isGoalkeeper() {
        return position == PlayerPosition.GOALKEEPER;
    }

    public Long getId() {
        return id;
    }

    public NationalTeam getTeam() {
        return team;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public Integer getJerseyNumber() {
        return jerseyNumber;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public String getClub() {
        return club;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }
}
