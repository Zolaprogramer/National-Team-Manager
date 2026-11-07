package com.nationalteammanager.validation;

import jakarta.validation.ConstraintValidatorContext;

final class ValidatorMessages {

    private ValidatorMessages() {
    }

    /** Prikazuje poruku validatora klase pored konkretnog polja obrasca. Uvijek vraća false. */
    static boolean attachToField(ConstraintValidatorContext context, String field) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
