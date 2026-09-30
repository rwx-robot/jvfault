package com.jvfault.spring.boot.starter.fixture;

import com.jvfault.spring.boot.starter.JvfaultComponent;
import org.springframework.stereotype.Service;

/**
 * Spring 侧 bean：标注 {@link JvfaultComponent} 后会被反向注入 jvfault 容器，
 * 从而能被 jvfault 的 {@code @Component} 通过 {@code @Inject} 直接拿到。
 */
@Service
@JvfaultComponent
public class ClockService {

    public String now() {
        return java.time.Instant.now().toString();
    }
}
