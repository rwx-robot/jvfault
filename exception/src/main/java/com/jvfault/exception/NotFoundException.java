package com.jvfault.exception;

/** 404 Not Found */
public class NotFoundException extends HttpException {
    public NotFoundException(String detail) {
        super(404, "Not Found", detail);
    }
}
