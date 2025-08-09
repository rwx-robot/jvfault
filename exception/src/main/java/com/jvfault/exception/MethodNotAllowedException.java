package com.jvfault.exception;

/** 405 Method Not Allowed */
public class MethodNotAllowedException extends HttpException {
    public MethodNotAllowedException(String detail) {
        super(405, "Method Not Allowed", detail);
    }
}
