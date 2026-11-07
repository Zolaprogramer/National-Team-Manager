package com.nationalteammanager.validation;

import com.nationalteammanager.dto.CompetitionForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, CompetitionForm> {

    @Override
    public boolean isValid(CompetitionForm form, ConstraintValidatorContext context) {
        if (form.getStartDate() == null || form.getEndDate() == null
                || !form.getEndDate().isBefore(form.getStartDate())) {
            return true;
        }
        return ValidatorMessages.attachToField(context, "endDate");
    }
}
