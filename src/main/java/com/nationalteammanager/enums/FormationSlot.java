package com.nationalteammanager.enums;

/**
 * Jedna pozicija u formaciji. Koordinate x i y su procenti širine i visine terena.
 */
public record FormationSlot(int index, String label, double x, double y) {
}
