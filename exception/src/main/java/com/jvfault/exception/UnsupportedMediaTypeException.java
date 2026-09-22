package com.jvfault.exception;

/** 415 Unsupported Media Type */
public class UnsupportedMediaTypeException extends HttpException {
    public UnsupportedMediaTypeException(String detail) {
        super(415, "Unsupported Media Type", detail);
    }
}
