package com.nationalteammanager.dto;

import com.nationalteammanager.enums.MatchStatus;

public class MatchFilterForm {

    public static final String VIEW_UPCOMING = "upcoming";
    public static final String VIEW_FINISHED = "finished";

    private Long teamId;
    private MatchStatus status;
    private String view;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
