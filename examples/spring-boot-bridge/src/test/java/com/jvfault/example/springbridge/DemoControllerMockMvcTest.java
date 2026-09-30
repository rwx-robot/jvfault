package com.jvfault.example.springbridge;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实 HTTP 请求链路回归（MockMvc，不占端口、CI 友好）。
 *
 * <p><b>为什么必须有它</b>：{@link SpringBridgeExampleTest} 以 {@code web=NONE} 启动，
 * 只验证了「bean 能取到」，<b>抓不到请求链路上的问题</b> —— 实测中就踩过一次：
 * 示例模块不继承根构建的 {@code -parameters}，{@code @RequestParam} 参数名拿不到，
 * 上下文一切正常但真实请求直接 500。只有真发一次请求才会暴露。
 * 这条用例就是为这类「启动绿、请求红」的缺陷兜底。
 *
 * @since v1.0.14 (2026)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Spring Boot ↔ jvfault：真实 HTTP 请求链路")
class DemoControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /greet?name=ny -> 200，响应含 jvfault 组件与 Spring ClockService 的输出")
    void greetEndpointReturnsBridgedGreeting() throws Exception {
        mockMvc.perform(get("/greet").param("name", "ny"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("hi ny")))
                .andExpect(content().string(Matchers.containsString("bridged at")));
    }

    @Test
    @DisplayName("GET /greet 不带参数 -> 200，走默认值 world")
    void greetEndpointUsesDefaultName() throws Exception {
        mockMvc.perform(get("/greet"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.startsWith("hi world")));
    }
}
