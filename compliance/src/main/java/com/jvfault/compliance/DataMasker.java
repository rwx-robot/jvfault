package com.jvfault.compliance;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具 —— 手机号/邮箱/自定义模式掩码。
 * 对应 roadmap v1.0.0: 数据脱敏
 *
 * @since v1.0.0 (2026)
 * @author jvfault team
 */
public final class DataMasker {

    private static final Pattern PHONE = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern EMAIL = Pattern.compile("(^|)[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+)");

    private DataMasker() {
    }

    /** 手机号: 138****5678 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 邮箱: a***b@example.com（保留首字符与域名） */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.substring(0, Math.min(1, local.length()));
        return head + "***" + domain;
    }

    /** 通用掩码: 保留前 keep 个字符，其余以 * 替换 */
    public static String mask(String value, int keep) {
        if (value == null || value.length() <= keep) {
            return value;
        }
        return value.substring(0, keep) + "*".repeat(Math.max(0, value.length() - keep));
    }
}
