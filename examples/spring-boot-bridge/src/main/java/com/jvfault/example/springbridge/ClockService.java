package com.jvfault.example.springbridge;

import com.jvfault.spring.boot.starter.JvfaultComponent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Spring 侧服务：标注 {@link JvfaultComponent} 后被<b>反向注入</b> jvfault 容器，
 * 于是 jvfault 的 {@code @Component}（见 {@link Greeter}）能通过 {@code @Inject} 拿到它。
 *
 * <p>典型场景：这个 bean 依赖 Spring 生态的能力（事务、JPA、配置绑定……），
 * 而 jvfault 组件想直接复用它 —— 反向注入让两个容器各管各的，又能互相借力。
 *
 * @since v1.0.14 (2026)
 */
@Service
@JvfaultComponent
public class ClockService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public String now() {
        return LocalDateTime.now().format(FMT);
    }
}
