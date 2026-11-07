package com.nationalteammanager.dto;

import java.util.List;

/**
 * Pošto sve reprezentacije pripadaju jednom korisniku, svaka odlučena utakmica je istovremeno
 * pobjeda jedne i poraz druge njegove reprezentacije, pa su ukupne pobjede i porazi uvijek jednaki.
 */
public record OverallStatistics(long teamCount, long playerCount, long matchCount, long playedCount, int wins,
                                int losses, int draws, int goalsScored, List<PlayerStatistics> topScorers,
                                List<PlayerStatistics> mostAppearances, List<TeamStatistics> teams,
                                List<PlayerStatistics> players) {

    public PlayerStatistics bestScorer() {
        return topScorers.isEmpty() ? null : topScorers.get(0);
    }

    public PlayerStatistics mostCapped() {
        return mostAppearances.isEmpty() ? null : mostAppearances.get(0);
    }
}
