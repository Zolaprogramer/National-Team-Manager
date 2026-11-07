package com.nationalteammanager.entity;

import com.nationalteammanager.enums.EventType;
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

@Entity
@Table(name = "MatchEvents")
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchId", nullable = false, foreignKey = @ForeignKey(name = "FK_MatchEvents_Matches"))
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playerId", nullable = false, foreignKey = @ForeignKey(name = "FK_MatchEvents_Players"))
    private Player player;

    @Column(name = "eventMinute", nullable = false)
    private Integer minute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(length = 255)
    private String description;

    protected MatchEvent() {
    }

    public MatchEvent(Match match, Player player, Integer minute, EventType type, String description) {
        this.match = match;
        this.player = player;
        this.minute = minute;
        this.type = type;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public Player getPlayer() {
        return player;
    }

    public Integer getMinute() {
        return minute;
    }

    public EventType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
