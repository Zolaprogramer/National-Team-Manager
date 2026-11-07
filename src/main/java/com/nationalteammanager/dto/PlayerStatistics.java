package com.nationalteammanager.dto;

import com.nationalteammanager.enums.PlayerPosition;

public record PlayerStatistics(Long playerId, String playerName, String teamName, PlayerPosition position,
                               int appearances, long goals, long assists, long yellowCards, long redCards) {
}
