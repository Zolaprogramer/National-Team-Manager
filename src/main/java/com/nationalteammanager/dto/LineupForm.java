package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.enums.Formation;
import com.nationalteammanager.enums.LineupRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LineupForm {

    public static final int STARTER_COUNT = 11;

    @NotNull(message = "{validation.lineup.formation.required}")
    private Formation formation = Formation.F_4_3_3;

    @Size(max = 500, message = "{validation.lineup.tactic.size}")
    private String tactic;

    @NotNull(message = "{validation.lineup.captain.required}")
    private Long captainId;

    /** Indeks u listi je pozicija u formaciji, a indeks 0 je golman. */
    private List<Long> starterIds = new ArrayList<>(Arrays.asList(new Long[STARTER_COUNT]));

    private List<Long> substituteIds = new ArrayList<>();

    public static LineupForm from(Lineup lineup) {
        LineupForm form = new LineupForm();
        form.fillFrom(lineup);
        return form;
    }

    protected void fillFrom(Lineup lineup) {
        formation = lineup.getFormation();
        tactic = lineup.getTactic();
        captainId = lineup.getCaptain().getId();
        lineup.getPlayersWithRole(LineupRole.STARTER).forEach(
                starter -> starterIds.set(starter.getSlotIndex(), starter.getPlayer().getId()));
        lineup.getPlayersWithRole(LineupRole.SUBSTITUTE).forEach(
                substitute -> substituteIds.add(substitute.getPlayer().getId()));
    }

    public Formation getFormation() {
        return formation;
    }

    public void setFormation(Formation formation) {
        this.formation = formation;
    }

    public String getTactic() {
        return tactic;
    }

    public void setTactic(String tactic) {
        this.tactic = tactic;
    }

    public Long getCaptainId() {
        return captainId;
    }

    public void setCaptainId(Long captainId) {
        this.captainId = captainId;
    }

    public List<Long> getStarterIds() {
        return starterIds;
    }

    public void setStarterIds(List<Long> starterIds) {
        this.starterIds = starterIds;
    }

    public List<Long> getSubstituteIds() {
        return substituteIds;
    }

    public void setSubstituteIds(List<Long> substituteIds) {
        this.substituteIds = substituteIds;
    }
}
