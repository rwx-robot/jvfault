package com.jvfault.microservices;

/**
 * 传输超时异常。
 *
 * @since v0.6.0 (2020)
 * @author jvfault team
 */
public class TransportTimeoutException extends TransportException {

    public TransportTimeoutException(String message) {
        super(message);
    }
}
