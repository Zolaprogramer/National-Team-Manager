package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;

import java.util.Collection;

public record TeamStatistics(Long teamId, String teamName, long playerCount, int played, int wins, int draws,
                             int losses, int goalsFor, int goalsAgainst) {

    private static final int POINTS_FOR_WIN = 3;

    /** Računa statistiku iz završenih utakmica u kojima je reprezentacija učestvovala. */
    public static TeamStatistics of(NationalTeam team, long playerCount, Collection<Match> matches) {
        int played = 0;
        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        for (Match match : matches) {
            if (!match.isFinished() || !match.involves(team)) {
                continue;
            }
            boolean home = match.isHomeTeam(team);
            int scored = home ? match.getHomeGoals() : match.getAwayGoals();
            int conceded = home ? match.getAwayGoals() : match.getHomeGoals();

            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                wins++;
            } else if (scored < conceded) {
                losses++;
            } else {
                draws++;
            }
        }
        return new TeamStatistics(team.getId(), team.getTeamName(), playerCount, played, wins, draws, losses,
                goalsFor, goalsAgainst);
    }

    public int points() {
        return wins * POINTS_FOR_WIN + draws;
    }

    public int goalDifference() {
        return goalsFor - goalsAgainst;
    }
}
