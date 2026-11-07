package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;

import java.util.List;

public record DashboardView(OverallStatistics statistics, List<Match> recentResults, List<Match> upcomingMatches,
                            List<NationalTeam> teams) {
}
