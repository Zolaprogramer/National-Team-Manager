package com.nationalteammanager.entity;

import com.nationalteammanager.enums.LineupRole;
import com.nationalteammanager.enums.PlayerDuty;
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
@Table(name = "LineupPlayers", uniqueConstraints =
        @UniqueConstraint(name = "UQ_LineupPlayers_lineup_player", columnNames = {"lineupId", "playerId"}))
public class LineupPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lineupId", nullable = false, foreignKey = @ForeignKey(name = "FK_LineupPlayers_Lineups"))
    private Lineup lineup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playerId", nullable = false, foreignKey = @ForeignKey(name = "FK_LineupPlayers_Players"))
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineupRole role;

    /** Pozicija u formaciji; rezervni igrači je nemaju. */
    private Integer slotIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerDuty duty = PlayerDuty.BALANCED;

    protected LineupPlayer() {
    }

    LineupPlayer(Lineup lineup, Player player, LineupRole role, Integer slotIndex, PlayerDuty duty) {
        this.lineup = lineup;
        this.player = player;
        this.role = role;
        this.slotIndex = slotIndex;
        this.duty = duty;
    }

    public PlayerDuty getDuty() {
        return duty;
    }

    public Long getId() {
        return id;
    }

    public Lineup getLineup() {
        return lineup;
    }

    public Player getPlayer() {
        return player;
    }

    public LineupRole getRole() {
        return role;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }
}
