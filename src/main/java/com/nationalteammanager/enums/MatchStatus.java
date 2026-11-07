package com.nationalteammanager.enums;

public enum MatchStatus implements TranslatableEnum {
    SCHEDULED,
    IN_PROGRESS,
    FINISHED,
    CANCELLED;

    /** Rezultat i događaji postoje samo za utakmice koje su počele. */
    public boolean hasStarted() {
        return this == IN_PROGRESS || this == FINISHED;
    }
}
