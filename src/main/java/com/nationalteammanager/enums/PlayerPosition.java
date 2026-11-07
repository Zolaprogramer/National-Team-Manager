package com.nationalteammanager.enums;

public enum PlayerPosition implements TranslatableEnum {
    GOALKEEPER("GK"),
    DEFENDER("DF"),
    MIDFIELDER("MF"),
    FORWARD("FW");

    private final String shortLabel;

    PlayerPosition(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}
