package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.entity.TacticalSettings;
import com.nationalteammanager.enums.AttackDirection;
import com.nationalteammanager.enums.DefensiveLine;
import com.nationalteammanager.enums.Mentality;
import com.nationalteammanager.enums.PassingStyle;
import com.nationalteammanager.enums.PlayStyle;
import com.nationalteammanager.enums.PlayTempo;
import com.nationalteammanager.enums.PlayWidth;
import com.nationalteammanager.enums.PlayerDuty;
import com.nationalteammanager.enums.PressingIntensity;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/** Obrazac taktičke table: sve iz običnog sastava i dodatno postavke i individualne uloge. */
public class TacticsForm extends LineupForm {

    @NotNull(message = "{validation.tactics.required}")
    private PlayStyle playStyle;

    @NotNull(message = "{validation.tactics.required}")
    private Mentality mentality;

    @NotNull(message = "{validation.tactics.required}")
    private PlayWidth playWidth;

    @NotNull(message = "{validation.tactics.required}")
    private PlayTempo playTempo;

    @NotNull(message = "{validation.tactics.required}")
    private PressingIntensity pressing;

    @NotNull(message = "{validation.tactics.required}")
    private DefensiveLine defensiveLine;

    @NotNull(message = "{validation.tactics.required}")
    private PassingStyle passingStyle;

    @NotNull(message = "{validation.tactics.required}")
    private AttackDirection attackDirection;

    private Long penaltyTakerId;

    private Long freeKickTakerId;

    private Long cornerTakerId;

    private Map<Long, PlayerDuty> duties = new HashMap<>();

    public static TacticsForm from(Lineup lineup) {
        TacticsForm form = new TacticsForm();
        form.fillFrom(lineup);
        TacticalSettings settings = lineup.getSettings();
        form.playStyle = settings.getPlayStyle();
        form.mentality = settings.getMentality();
        form.playWidth = settings.getPlayWidth();
        form.playTempo = settings.getPlayTempo();
        form.pressing = settings.getPressing();
        form.defensiveLine = settings.getDefensiveLine();
        form.passingStyle = settings.getPassingStyle();
        form.attackDirection = settings.getAttackDirection();
        form.penaltyTakerId = idOf(lineup.getPenaltyTaker());
        form.freeKickTakerId = idOf(lineup.getFreeKickTaker());
        form.cornerTakerId = idOf(lineup.getCornerTaker());
        form.duties = lineup.getDutiesByPlayer();
        return form;
    }

    public TacticalSettings toSettings() {
        return new TacticalSettings(playStyle, mentality, playWidth, playTempo, pressing, defensiveLine,
                passingStyle, attackDirection);
    }

    private static Long idOf(Player player) {
        return player == null ? null : player.getId();
    }

    public PlayStyle getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(PlayStyle playStyle) {
        this.playStyle = playStyle;
    }

    public Mentality getMentality() {
        return mentality;
    }

    public void setMentality(Mentality mentality) {
        this.mentality = mentality;
    }

    public PlayWidth getPlayWidth() {
        return playWidth;
    }

    public void setPlayWidth(PlayWidth playWidth) {
        this.playWidth = playWidth;
    }

    public PlayTempo getPlayTempo() {
        return playTempo;
    }

    public void setPlayTempo(PlayTempo playTempo) {
        this.playTempo = playTempo;
    }

    public PressingIntensity getPressing() {
        return pressing;
    }

    public void setPressing(PressingIntensity pressing) {
        this.pressing = pressing;
    }

    public DefensiveLine getDefensiveLine() {
        return defensiveLine;
    }

    public void setDefensiveLine(DefensiveLine defensiveLine) {
        this.defensiveLine = defensiveLine;
    }

    public PassingStyle getPassingStyle() {
        return passingStyle;
    }

    public void setPassingStyle(PassingStyle passingStyle) {
        this.passingStyle = passingStyle;
    }

    public AttackDirection getAttackDirection() {
        return attackDirection;
    }

    public void setAttackDirection(AttackDirection attackDirection) {
        this.attackDirection = attackDirection;
    }

    public Long getPenaltyTakerId() {
        return penaltyTakerId;
    }

    public void setPenaltyTakerId(Long penaltyTakerId) {
        this.penaltyTakerId = penaltyTakerId;
    }

    public Long getFreeKickTakerId() {
        return freeKickTakerId;
    }

    public void setFreeKickTakerId(Long freeKickTakerId) {
        this.freeKickTakerId = freeKickTakerId;
    }

    public Long getCornerTakerId() {
        return cornerTakerId;
    }

    public void setCornerTakerId(Long cornerTakerId) {
        this.cornerTakerId = cornerTakerId;
    }

    public Map<Long, PlayerDuty> getDuties() {
        return duties;
    }

    public void setDuties(Map<Long, PlayerDuty> duties) {
        this.duties = duties;
    }
}
