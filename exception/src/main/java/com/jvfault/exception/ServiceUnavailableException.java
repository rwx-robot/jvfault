package com.jvfault.exception;

/** 503 Service Unavailable */
public class ServiceUnavailableException extends HttpException {
    public ServiceUnavailableException(String detail) {
        super(503, "Service Unavailable", detail);
    }
}
