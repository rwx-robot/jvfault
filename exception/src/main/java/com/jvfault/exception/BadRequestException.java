package com.jvfault.exception;

/** 400 Bad Request */
public class BadRequestException extends HttpException {
    public BadRequestException(String detail) {
        super(400, "Bad Request", detail);
    }
}
