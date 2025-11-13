package com.jvfault.bootstrap;

/**
 * 测试用计算器实现（ByteBuddy 类代理目标）
 */
public class Calculator implements CalculatorInterface {

    public int add(int a, int b) {
        return a + b;
    }

    public int mul(int a, int b) {
        return a * b;
    }

    public int div(int a, int b) {
        return a / b;
    }

    public String shout(String text) {
        return text;
    }

    public int abs(int value) {
        return Math.abs(value);
    }
}
