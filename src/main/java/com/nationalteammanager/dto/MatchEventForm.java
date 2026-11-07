package com.nationalteammanager.dto;

import com.nationalteammanager.enums.EventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MatchEventForm {

    @NotNull(message = "{validation.event.player.required}")
    private Long playerId;

    @NotNull(message = "{validation.event.minute.required}")
    @Min(value = 1, message = "{validation.event.minute.range}")
    @Max(value = 130, message = "{validation.event.minute.range}")
    private Integer minute;

    @NotNull(message = "{validation.event.type.required}")
    private EventType type;

    @Size(max = 255, message = "{validation.event.description.size}")
    private String description;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
