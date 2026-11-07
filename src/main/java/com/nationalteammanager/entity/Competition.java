package com.nationalteammanager.entity;

import com.nationalteammanager.enums.CompetitionType;
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

import java.time.LocalDate;

@Entity
@Table(name = "Competitions")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Takmičenje bez vlasnika je zajedničko (zvanično): vide ga svi korisnici, a mijenja ga administrator. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", foreignKey = @ForeignKey(name = "FK_Competitions_Users"))
    private User owner;

    /** Relativna putanja logotipa u folderu uploads, npr. competitions/abc.png. */
    @Column(length = 255)
    private String logoPath;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompetitionType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    protected Competition() {
    }

    public Competition(User owner) {
        this.owner = owner;
    }

    public void updateDetails(String name, String season, CompetitionType type,
                              LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.season = season;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isShared() {
        return owner == null;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public boolean includes(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getSeason() {
        return season;
    }

    public CompetitionType getType() {
        return type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
