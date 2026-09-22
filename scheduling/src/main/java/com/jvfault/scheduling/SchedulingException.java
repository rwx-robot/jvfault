package com.jvfault.scheduling;

/**
 * 调度异常。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class SchedulingException extends RuntimeException {

    public SchedulingException(String message) {
        super(message);
    }

    public SchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}
