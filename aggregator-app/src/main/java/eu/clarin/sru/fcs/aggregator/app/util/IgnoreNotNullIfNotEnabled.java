package eu.clarin.sru.fcs.aggregator.app.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = IgnoreNonNullIfNotEnabledValidator.class)
public @interface IgnoreNotNullIfNotEnabled {

    // https://github.com/jakartaee/validation/blob/main/src/main/java/jakarta/validation/constraints/NotNull.java

    String enabledFieldName() default "enabled";

    String message() default "{jakarta.validation.constraints.NotNull.message}"; // reuse same message as @NotNull

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // ----------------------------------------------------------------------

    @Target({ ElementType.FIELD, ElementType.METHOD }) // ElementType.ANNOTATION_TYPE ?
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface IgnorableNotNull {
    }

}
