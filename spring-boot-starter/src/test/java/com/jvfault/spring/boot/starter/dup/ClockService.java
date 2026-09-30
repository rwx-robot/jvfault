package com.jvfault.spring.boot.starter.dup;

import com.jvfault.spring.boot.starter.JvfaultComponent;
import org.springframework.stereotype.Service;

/**
 * 与 {@code fixture.ClockService} <b>同简单名、不同包</b>的 Spring bean —— 用来复现
 * 「两个 Spring bean 的 jvfault bean 名都叫 clockService」的撞名场景。
 *
 * <p>放独立包（与 fixture 平级）是刻意的：jvfault 的 {@code @Module} 推断会<b>递归</b>
 * 扫描 base-packages，放进 fixture 子包会被顺带扫到。
 */
@Service
@JvfaultComponent
public class ClockService {

    public String now() {
        return "dup";
    }
}
