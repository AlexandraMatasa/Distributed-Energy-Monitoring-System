package com.example.usermanagement.dtos.validators;

import com.example.usermanagement.dtos.validators.annotation.ValidRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {

    @Override
    public void initialize(ValidRole constraintAnnotation) {
    }

    @Override
    public boolean isValid(String role, ConstraintValidatorContext context) {
        if (role == null) {
            return false;
        }
        return role.equals("ADMIN") || role.equals("CLIENT");
    }
}
