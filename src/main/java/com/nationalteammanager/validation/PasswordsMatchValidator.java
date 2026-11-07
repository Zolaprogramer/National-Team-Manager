package com.nationalteammanager.validation;

import com.nationalteammanager.dto.RegistrationForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegistrationForm> {

    @Override
    public boolean isValid(RegistrationForm form, ConstraintValidatorContext context) {
        if (Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            return true;
        }
        return ValidatorMessages.attachToField(context, "confirmPassword");
    }
}
