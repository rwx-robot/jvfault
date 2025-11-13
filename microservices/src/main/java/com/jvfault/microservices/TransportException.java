package com.jvfault.microservices;

/**
 * 传输层异常。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class TransportException extends RuntimeException {

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
