package com.example.authmanagement.dtos.validators.annotation;

import com.example.authmanagement.dtos.validators.RoleValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = RoleValidator.class)
public @interface ValidRole {
    String message() default "Role must be either ADMIN or CLIENT";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}