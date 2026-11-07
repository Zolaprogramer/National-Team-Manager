package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;

import java.util.List;

/** Podaci potrebni obrascu za kreiranje ili izmjenu sastava. */
public record LineupEditor(Match match, NationalTeam team, List<Player> squad, List<Player> goalkeepers,
                           List<Player> outfieldPlayers) {

    public boolean canFieldStartingEleven() {
        return !goalkeepers.isEmpty() && outfieldPlayers.size() >= LineupForm.STARTER_COUNT - 1;
    }
}
