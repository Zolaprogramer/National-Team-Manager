package com.nationalteammanager.dto;

import com.nationalteammanager.entity.Match;
import com.nationalteammanager.entity.NationalTeam;
import com.nationalteammanager.entity.Player;
import com.nationalteammanager.entity.Stadium;

import java.util.List;

public record TeamDetails(NationalTeam team, List<Player> players, List<Match> matches, TeamStatistics statistics,
                          List<Stadium> stadiums) {
}
