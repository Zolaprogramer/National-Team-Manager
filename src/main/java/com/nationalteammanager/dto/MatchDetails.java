package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.MatchEvent;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.enums.MatchStatus;

import java.time.LocalDateTime;
import java.util.List;

public record MatchDetails(Match match, List<MatchEvent> events, List<Player> homePlayers, List<Player> awayPlayers,
                           boolean homeLineupExists, boolean awayLineupExists) {

    public boolean canRecordResult() {
        return match.getStatus() != MatchStatus.CANCELLED && !match.getKickoff().isAfter(LocalDateTime.now());
    }

    public boolean canRecordEvents() {
        return match.getStatus().hasStarted();
    }

    public boolean canEditLineups() {
        return match.getStatus() != MatchStatus.CANCELLED;
    }
}
