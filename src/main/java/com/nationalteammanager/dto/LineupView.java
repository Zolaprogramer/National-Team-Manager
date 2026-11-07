package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Lineup;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.FormationSlot;

import java.util.List;

public record LineupView(Lineup lineup, List<PitchPlayer> starters, List<Player> substitutes) {

    /** Igrač postavljen na određenu poziciju na terenu. */
    public record PitchPlayer(Player player, FormationSlot slot, boolean captain) {
    }
}
