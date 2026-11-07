package com.nationalteammanager.dto;

import com.nationalteammanager.enums.EventType;

public record PlayerEventCount(Long playerId, EventType type, long total) {
}
