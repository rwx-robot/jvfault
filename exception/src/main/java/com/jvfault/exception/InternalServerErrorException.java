package com.jvfault.exception;

/** 500 Internal Server Error */
public class InternalServerErrorException extends HttpException {
    public InternalServerErrorException(String detail) {
        super(500, "Internal Server Error", detail);
    }
}
