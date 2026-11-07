package com.nationalteammanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nationalteammanager.dto.TacticsForm;
import com.nationalteammanager.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/** Pretvara stanje sastava u JSON koji se čuva u koloni Lineups.previousState i obrnuto. */
@Component
public class LineupSnapshotMapper {

    private final ObjectMapper objectMapper;

    public LineupSnapshotMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(TacticsForm form) {
        try {
            return objectMapper.writeValueAsString(form);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stanje sastava nije moguće sačuvati.", exception);
        }
    }

    public TacticsForm fromJson(String json) {
        try {
            return objectMapper.readValue(json, TacticsForm.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessRuleException("error.tactics.invalidPrevious");
        }
    }
}
