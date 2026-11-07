package com.nationalteammanager.dto;

import com.nationalteammanager.entity.NationalTeam;

public record TeamSummary(NationalTeam team, TeamStatistics statistics) {
}
