package com.nationalteammanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MatchResultForm {

    @NotNull(message = "{validation.result.home.required}")
    @Min(value = 0, message = "{validation.match.goals.negative}")
    private Integer homeGoals;

    @NotNull(message = "{validation.result.away.required}")
    @Min(value = 0, message = "{validation.match.goals.negative}")
    private Integer awayGoals;

    private boolean finished;

    public Integer getHomeGoals() {
        return homeGoals;
    }

    public void setHomeGoals(Integer homeGoals) {
        this.homeGoals = homeGoals;
    }

    public Integer getAwayGoals() {
        return awayGoals;
    }

    public void setAwayGoals(Integer awayGoals) {
        this.awayGoals = awayGoals;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
