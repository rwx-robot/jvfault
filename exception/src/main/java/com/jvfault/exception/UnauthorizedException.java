package com.jvfault.exception;

/** 401 Unauthorized */
public class UnauthorizedException extends HttpException {
    public UnauthorizedException(String detail) {
        super(401, "Unauthorized", detail);
    }
}
