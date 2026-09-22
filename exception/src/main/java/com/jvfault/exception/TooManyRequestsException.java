package com.jvfault.exception;

/** 429 Too Many Requests */
public class TooManyRequestsException extends HttpException {
    public TooManyRequestsException(String detail) {
        super(429, "Too Many Requests", detail);
    }
}
