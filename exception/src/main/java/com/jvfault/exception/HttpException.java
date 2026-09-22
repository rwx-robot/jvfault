package com.jvfault.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 异常基类 - 携带 RFC 7807 ProblemDetail。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class HttpException extends RuntimeException {

    private final ProblemDetail problem;
    private final Map<String, String> headers = new LinkedHashMap<>();

    public HttpException(int status, String title, String detail) {
        super(detail);
        this.problem = ProblemDetail.forStatusAndDetail(status, detail);
        this.problem.setTitle(title);
    }

    public HttpException(ProblemDetail problem) {
        super(problem.getDetail());
        this.problem = problem;
    }

    public HttpException(int status, String title, String detail, Throwable cause) {
        super(detail, cause);
        this.problem = ProblemDetail.forStatusAndDetail(status, detail);
        this.problem.setTitle(title);
    }

    public int getStatus() {
        return problem.getStatus();
    }

    public ProblemDetail getProblem() {
        return problem;
    }

    /**
     * 附加响应头（如 WWW-Authenticate、Retry-After）。
     */
    public HttpException header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    // ============ 常用异常工厂 ============

    public static BadRequestException badRequest(String detail) {
        return new BadRequestException(detail);
    }

    public static UnauthorizedException unauthorized(String detail) {
        return new UnauthorizedException(detail);
    }

    public static ForbiddenException forbidden(String detail) {
        return new ForbiddenException(detail);
    }

    public static NotFoundException notFound(String detail) {
        return new NotFoundException(detail);
    }

    public static ConflictException conflict(String detail) {
        return new ConflictException(detail);
    }

    public static InternalServerErrorException internalServerError(String detail) {
        return new InternalServerErrorException(detail);
    }

    public static ServiceUnavailableException serviceUnavailable(String detail) {
        return new ServiceUnavailableException(detail);
    }
}
