package com.nationalteammanager.dto;

/** Par igrač-utakmica iz kojeg se računaju nastupi. */
public record PlayerMatchRef(Long playerId, Long matchId) {
}
