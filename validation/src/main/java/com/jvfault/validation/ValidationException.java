package com.jvfault.validation;

import java.util.Set;

/**
 * 校验失败异常 - 携带全部违反项。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class ValidationException extends RuntimeException {

    private final Set<ConstraintViolation<?>> violations;

    public ValidationException(Set<ConstraintViolation<?>> violations) {
        super("Validation failed: " + violations.size() + " violation(s) - " + violations);
        this.violations = violations;
    }

    public Set<ConstraintViolation<?>> getViolations() {
        return violations;
    }
}
