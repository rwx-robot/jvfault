package com.jvfault.exception;

/** 409 Conflict */
public class ConflictException extends HttpException {
    public ConflictException(String detail) {
        super(409, "Conflict", detail);
    }
}
