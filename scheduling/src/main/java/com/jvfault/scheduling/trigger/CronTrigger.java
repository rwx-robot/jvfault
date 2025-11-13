package com.jvfault.scheduling.trigger;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import com.jvfault.scheduling.SchedulingException;
import java.time.temporal.TemporalAdjusters;
import java.util.BitSet;

/**
 * Cron 表达式解析与触发（5/6 字段）。
 *
 * <p>格式：<code>秒 分 时 日 月 周</code>（5 字段时秒固定为 0）。
 * 支持：* 、逗号列表、范围 a-b、步进 a/b、月份与星期英文缩写
 * （JAN..DEC、SUN..SAT，星期 0/7 均为周日）。不支持 L/W/#。
 *
 * @since v0.7.0 (2021)
 * @author jvfault team
 */
public class CronTrigger implements Trigger {

    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] DAYS = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private final BitSet seconds = new BitSet(60);
    private final BitSet minutes = new BitSet(60);
    private final BitSet hours = new BitSet(24);
    private final BitSet daysOfMonth = new BitSet(31);
    private final BitSet months = new BitSet(12);
    private final BitSet daysOfWeek = new BitSet(7);
    private final boolean dayOfMonthWildcard;
    private final boolean dayOfWeekWildcard;
    private final String expression;

    public CronTrigger(String expression) {
        this.expression = expression;
        String[] fields = expression.trim().split("\\s+");
        if (fields.length == 5) {
            parseField(fields[0], minutes, 0, 59, "分钟", expression, 0);
            parseField(fields[1], hours, 0, 23, "小时", expression, 0);
            parseField(fields[2], daysOfMonth, 1, 31, "日", expression, -1);
            parseField(fields[3], months, 1, 12, "月", expression, -1);
            parseField(fields[4], daysOfWeek, 0, 6, "周", expression, 0);
            dayOfMonthWildcard = fields[2].trim().equals("*");
            dayOfWeekWildcard = fields[4].trim().equals("*");
            seconds.set(0);
        } else if (fields.length == 6) {
            parseField(fields[0], seconds, 0, 59, "秒", expression, 0);
            parseField(fields[1], minutes, 0, 59, "分钟", expression, 0);
            parseField(fields[2], hours, 0, 23, "小时", expression, 0);
            parseField(fields[3], daysOfMonth, 1, 31, "日", expression, -1);
            parseField(fields[4], months, 1, 12, "月", expression, -1);
            parseField(fields[5], daysOfWeek, 0, 6, "周", expression, 0);
            dayOfMonthWildcard = fields[3].trim().equals("*");
            dayOfWeekWildcard = fields[5].trim().equals("*");
        } else {
            throw new SchedulingException("Cron 需要 5 或 6 个字段: " + expression);
        }
    }

    public String getExpression() {
        return expression;
    }

    private void parseField(String field, BitSet target, int min, int max, String name, String expr, int bitOffset) {
        String upper = field.toUpperCase(java.util.Locale.ROOT);
        for (String part : upper.split(",")) {
            int step = 1;
            String rangePart = part;
            int slash = part.indexOf('/');
            if (slash >= 0) {
                rangePart = part.substring(0, slash);
                step = Integer.parseInt(part.substring(slash + 1));
                if (step <= 0) {
                    throw new SchedulingException("步进必须为正: " + field + " in " + expr);
                }
            }
            int start;
            int end;
            if (rangePart.equals("*")) {
                start = min;
                end = max;
            } else if (rangePart.contains("-")) {
                int dash = rangePart.indexOf('-');
                start = parseValue(rangePart.substring(0, dash), name, min, max, expr);
                end = parseValue(rangePart.substring(dash + 1), name, min, max, expr);
            } else {
                start = parseValue(rangePart, name, min, max, expr);
                // "5/10" 形式：从 5 开始步进到 max
                end = slash >= 0 ? max : start;
            }
            for (int v = start; v <= end; v += step) {
                target.set(normalize(v, name, min, max, expr) + bitOffset);
            }
        }
    }

    private int parseValue(String token, String name, int min, int max, String expr) {
        try {
            int value = Integer.parseInt(token);
            return normalize(value, name, min, max, expr);
        } catch (NumberFormatException e) {
            int mapped = nameMapping(token, name);
            if (mapped >= 0) {
                return mapped;
            }
            throw new SchedulingException("无法解析 " + name + " 值: " + token + " in " + expr);
        }
    }

    /** 英文缩写映射 */
    private int nameMapping(String token, String name) {
        if ("月".equals(name)) {
            for (int i = 0; i < MONTHS.length; i++) {
                if (MONTHS[i].equals(token)) {
                    return i + 1;
                }
            }
        }
        if ("周".equals(name)) {
            for (int i = 0; i < DAYS.length; i++) {
                if (DAYS[i].equals(token)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int normalize(int value, String name, int min, int max, String expr) {
        // 星期 7 视为 0（周日）
        if ("周".equals(name) && value == 7) {
            return 0;
        }
        if (value < min || value > max) {
            throw new SchedulingException(name + " 超出范围 [" + min + "," + max + "]: " + value + " in " + expr);
        }
        return value;
    }

    @Override
    public ZonedDateTime nextExecution(ZonedDateTime prev, ZonedDateTime now) {
        return next(now);
    }

    /**
     * 精确匹配某时刻（含秒位）。
     */
    public boolean matches(ZonedDateTime time) {
        if (!seconds.get(time.getSecond())
                || !minutes.get(time.getMinute())
                || !hours.get(time.getHour())
                || !months.get(time.getMonthValue() - 1)) {
            return false;
        }
        boolean domMatch = daysOfMonth.get(time.getDayOfMonth() - 1);
        boolean dowMatch = daysOfWeek.get(time.getDayOfWeek().getValue() % 7);
        // 标准 cron 语义：一方为 * 时按另一方判定；双方受限时按 OR
        if (dayOfMonthWildcard && dayOfWeekWildcard) {
            return true;
        }
        if (dayOfMonthWildcard) {
            return dowMatch;
        }
        if (dayOfWeekWildcard) {
            return domMatch;
        }
        return domMatch || dowMatch;
    }

    /**
     * 从给定时间起查找下一次匹配（仅秒位 0 时按分钟跳跃优化）。
     */
    public ZonedDateTime next(ZonedDateTime now) {
        boolean secondRestricted = seconds.cardinality() == 1 && seconds.get(0);
        ZonedDateTime candidate = now.withNano(0).plusSeconds(1);
        ZonedDateTime limit = now.plusYears(4);
        while (!candidate.isAfter(limit)) {
            if (matches(candidate)) {
                return candidate;
            }
            // 常见形态 "0 * * * *"：秒位固定 0，按分钟跳跃
            candidate = secondRestricted
                    ? candidate.plusMinutes(1).withSecond(0)
                    : candidate.plusSeconds(1);
        }
        throw new SchedulingException("Cron 在 4 年内无匹配时间: " + expression);
    }
}
