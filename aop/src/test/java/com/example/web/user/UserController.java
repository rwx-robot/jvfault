package com.example.web.user;

/**
 * Pointcut 匹配测试夹具（FQN: com.example.web.user.UserController）
 */
public class UserController {

    public String handle(String body) {
        return body;
    }

    public String ping() {
        return "pong";
    }
}
