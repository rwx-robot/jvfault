package com.jvfault.bootstrap;

/**
 * 测试用计算器接口（JDK 代理目标）
 */
public interface CalculatorInterface {
    int add(int a, int b);

    int mul(int a, int b);

    int div(int a, int b);

    String shout(String text);

    int abs(int value);
}
