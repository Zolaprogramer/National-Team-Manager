package com.nationalteammanager.validation;

import com.nationalteammanager.dto.MatchForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DifferentTeamsValidator implements ConstraintValidator<DifferentTeams, MatchForm> {

    @Override
    public boolean isValid(MatchForm form, ConstraintValidatorContext context) {
        if (form.getHomeTeamId() == null || !form.getHomeTeamId().equals(form.getAwayTeamId())) {
            return true;
        }
        return ValidatorMessages.attachToField(context, "awayTeamId");
    }
}
