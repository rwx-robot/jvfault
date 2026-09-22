package com.jvfault.exception;

/** 403 Forbidden */
public class ForbiddenException extends HttpException {
    public ForbiddenException(String detail) {
        super(403, "Forbidden", detail);
    }
}
