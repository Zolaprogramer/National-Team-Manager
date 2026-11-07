package com.nationalteammanager.entity;

import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.LineupRole;
import com.nationalteammanager.enums.PlayerDuty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "Lineups", uniqueConstraints =
        @UniqueConstraint(name = "UQ_Lineups_match_team", columnNames = {"matchId", "teamId"}))
public class Lineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchId", nullable = false, foreignKey = @ForeignKey(name = "FK_Lineups_Matches"))
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teamId", nullable = false, foreignKey = @ForeignKey(name = "FK_Lineups_NationalTeams"))
    private NationalTeam team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Formation formation;

    @Column(length = 500)
    private String tactic;

    /** Jedan strani ključ na kapitena garantuje da sastav ne može imati dva kapitena. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "captainId", nullable = false, foreignKey = @ForeignKey(name = "FK_Lineups_Captain"))
    private Player captain;

    @Embedded
    private TacticalSettings settings = TacticalSettings.defaults();

    // Svaka uloga je jedan strani ključ, pa je može imati samo jedan igrač
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penaltyTakerId", foreignKey = @ForeignKey(name = "FK_Lineups_PenaltyTaker"))
    private Player penaltyTaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freeKickTakerId", foreignKey = @ForeignKey(name = "FK_Lineups_FreeKickTaker"))
    private Player freeKickTaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cornerTakerId", foreignKey = @ForeignKey(name = "FK_Lineups_CornerTaker"))
    private Player cornerTaker;

    /** Stanje prije posljednjeg čuvanja (JSON), koje omogućava vraćanje na prethodni sastav. */
    @Column(length = 4000)
    private String previousState;

    @OneToMany(mappedBy = "lineup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("role ASC, slotIndex ASC, id ASC")
    private List<LineupPlayer> lineupPlayers = new ArrayList<>();

    protected Lineup() {
    }

    public Lineup(Match match, NationalTeam team) {
        this.match = match;
        this.team = team;
    }

    public void updateDetails(Formation formation, String tactic, Player captain) {
        this.formation = formation;
        this.tactic = tactic;
        this.captain = captain;
    }

    public void updateTactics(TacticalSettings settings, Player penaltyTaker, Player freeKickTaker, Player cornerTaker) {
        this.settings = settings;
        this.penaltyTaker = penaltyTaker;
        this.freeKickTaker = freeKickTaker;
        this.cornerTaker = cornerTaker;
    }

    /** Izvođač prekida mora biti na terenu, pa uloga nestaje kada igrač izađe iz početnih 11. */
    public void removeTakersOutside(Collection<Long> starterIds) {
        if (penaltyTaker != null && !starterIds.contains(penaltyTaker.getId())) {
            penaltyTaker = null;
        }
        if (freeKickTaker != null && !starterIds.contains(freeKickTaker.getId())) {
            freeKickTaker = null;
        }
        if (cornerTaker != null && !starterIds.contains(cornerTaker.getId())) {
            cornerTaker = null;
        }
    }

    public void addStarter(Player player, int slotIndex, PlayerDuty duty) {
        lineupPlayers.add(new LineupPlayer(this, player, LineupRole.STARTER, slotIndex, duty));
    }

    public void addSubstitute(Player player, PlayerDuty duty) {
        lineupPlayers.add(new LineupPlayer(this, player, LineupRole.SUBSTITUTE, null, duty));
    }

    public Map<Long, PlayerDuty> getDutiesByPlayer() {
        Map<Long, PlayerDuty> duties = new HashMap<>();
        lineupPlayers.forEach(lineupPlayer -> duties.put(lineupPlayer.getPlayer().getId(), lineupPlayer.getDuty()));
        return duties;
    }

    public void rememberPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public TacticalSettings getSettings() {
        return settings;
    }

    public Player getPenaltyTaker() {
        return penaltyTaker;
    }

    public Player getFreeKickTaker() {
        return freeKickTaker;
    }

    public Player getCornerTaker() {
        return cornerTaker;
    }

    public String getPreviousState() {
        return previousState;
    }

    public List<LineupPlayer> getPlayersWithRole(LineupRole role) {
        return lineupPlayers.stream().filter(lineupPlayer -> lineupPlayer.getRole() == role).toList();
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public NationalTeam getTeam() {
        return team;
    }

    public Formation getFormation() {
        return formation;
    }

    public String getTactic() {
        return tactic;
    }

    public Player getCaptain() {
        return captain;
    }

    public List<LineupPlayer> getLineupPlayers() {
        return lineupPlayers;
    }
}
