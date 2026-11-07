package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;

import java.util.List;

/** Podaci potrebni stranici taktičke table. */
public record TacticsBoard(Match match, NationalTeam team, List<BoardPlayer> players, boolean previousStateExists) {

    /** Igrač u obliku koji JavaScript koristi za crtanje terena; photoUrl je null ako igrač nema fotografiju. */
    public record BoardPlayer(Long id, int number, String fullName, String lastName, String position,
                              boolean goalkeeper, String photoUrl) {

        public static BoardPlayer of(Player player) {
            String photoUrl = player.getPhotoFileName() == null ? null : "/players/" + player.getId() + "/photo";
            return new BoardPlayer(player.getId(), player.getJerseyNumber(), player.getFullName(), player.getLastName(),
                    player.getPosition().getShortLabel(), player.isGoalkeeper(), photoUrl);
        }
    }
}
