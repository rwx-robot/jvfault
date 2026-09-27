package com.jvfault.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-validation 核心测试（基于 Jakarta Validation + Hibernate Validator）。
 *
 * @since v1.1.0
 */
@DisplayName("Validation 模块测试")
class ValidationModuleTest {

    /** 固定为英文 locale，避免默认消息因 JVM 区域被本地化（如中文「不能为null」）。 */
    @BeforeAll
    static void useEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }


    // ============ 测试模型 ============

    static class Address {
        @NotNull
        private final String city;

        @Size(min = 6, max = 6)
        private final String postcode;

        Address(String city, String postcode) {
            this.city = city;
            this.postcode = postcode;
        }
    }

    static class User {
        @NotNull
        @Size(min = 2, max = 20)
        private final String name;

        @Email
        private final String email;

        @Min(18)
        @Max(120)
        private final int age;

        @Pattern(regexp = "^1\\d{10}$")
        private final String phone;

        @NotBlank
        private final String nickname;

        @PositiveOrZero
        private final int balance;

        @Past
        private final Instant birthday;

        @Valid
        private final Address address;

        User(String name, String email, int age, String phone, String nickname,
             int balance, Instant birthday, Address address) {
            this.name = name;
            this.email = email;
            this.age = age;
            this.phone = phone;
            this.nickname = nickname;
            this.balance = balance;
            this.birthday = birthday;
            this.address = address;
        }

        @AssertTrue
        public boolean isActive() {
            return true;
        }
    }

    static class Item {
        @NotNull
        private final String sku;

        Item(String sku) {
            this.sku = sku;
        }
    }

    static class Order {
        @Valid
        private final List<Item> items = new ArrayList<>();

        Order(Item... items) {
            this.items.addAll(Arrays.asList(items));
        }
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = EvenNumberValidator.class)
    public @interface EvenNumber {
        String message() default "{0} must be even";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class EvenNumberValidator implements ConstraintValidator<EvenNumber, Number> {
        @Override
        public boolean isValid(Number value, ConstraintValidatorContext ctx) {
            return value == null || value.intValue() % 2 == 0;
        }
    }

    static class CustomAnnotated {
        @EvenNumber
        private final Integer value;

        CustomAnnotated(Integer value) {
            this.value = value;
        }
    }

    private final DefaultValidationEngine engine = new DefaultValidationEngine();

    // ============ 基础约束 ============

    @Test
    @DisplayName("合法对象通过校验")
    void testValidObject() {
        User user = new User("Alice", "alice@example.com", 25, "13800138000",
                "alice", 100, Instant.now().minusSeconds(20L * 365 * 24 * 3600),
                new Address("Beijing", "100000"));
        assertTrue(engine.validate(user).isEmpty());
    }

    @Test
    @DisplayName("@NotNull / @NotBlank / @Size 违反")
    void testBasicViolations() {
        User user = new User("A", null, 25, "13800138000", "", 100,
                Instant.now().minusSeconds(20L * 365 * 24 * 3600), null);
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("name"), "应包含 name (Size min=2): " + paths);
        assertTrue(paths.contains("nickname"), "应包含 nickname (NotBlank)");
        assertFalse(paths.contains("balance"), "balance 满足 PositiveOrZero");
    }

    @Test
    @DisplayName("@Min / @Max / @PositiveOrZero")
    void testNumericConstraints() {
        User user = new User("Alice", "a@b.co", 15, "13800138000", "a", -1,
                Instant.now().minusSeconds(20L * 365 * 24 * 3600), null);
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("age"), "age 违反 @Min(18): " + paths);
        assertTrue(paths.contains("balance"), "balance 违反 @PositiveOrZero");
    }

    @Test
    @DisplayName("@Pattern / @Email 格式校验")
    void testFormatConstraints() {
        User user = new User("Alice", "not-an-email", 25, "123", "a", 0,
                Instant.now().minusSeconds(20L * 365 * 24 * 3600), null);
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("email"), "email 格式非法: " + paths);
        assertTrue(paths.contains("phone"), "phone 格式非法");
    }

    @Test
    @DisplayName("@Past 时间校验")
    void testPastConstraint() {
        User user = new User("Alice", "a@b.co", 25, "13800138000", "a", 0,
                Instant.now().plus(1, ChronoUnit.DAYS), null);
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("birthday"), "birthday 违反 @Past: " + paths);
    }

    @Test
    @DisplayName("getter 校验 (@AssertTrue on isActive)")
    void testGetterValidation() {
        User user = new User("Alice", "a@b.co", 25, "13800138000", "a", 0,
                Instant.now().minusSeconds(20L * 365 * 24 * 3600), null);
        for (ConstraintViolation<User> v : engine.validate(user)) {
            assertNotEquals("active", v.getPropertyPath());
        }
    }

    // ============ 级联 ============

    @Test
    @DisplayName("@Valid 级联嵌套对象，路径含父级")
    void testCascade() {
        Address badAddress = new Address(null, "123");
        User user = new User("Alice", "a@b.co", 25, "13800138000", "a", 0,
                Instant.now().minusSeconds(20L * 365 * 24 * 3600), badAddress);

        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("address.city"), "级联路径 address.city: " + paths);
        assertTrue(paths.contains("address.postcode"), "级联路径 address.postcode");
    }

    @Test
    @DisplayName("集合元素级联，路径带下标（Hibernate 格式 items.sku[1]）")
    void testCollectionCascade() {
        Order order = new Order(new Item("SKU-1"), new Item(null));
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<Order> v : engine.validate(order)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("items.sku[1]"), "带下标的级联路径: " + paths);
    }

    // ============ 消息插值 / validateProperty ============

    @Test
    @DisplayName("默认消息 + 属性路径 + 命名属性插值")
    void testMessageInterpolation() {
        Address address = new Address(null, "123");
        Set<ConstraintViolation<Address>> violations = engine.validate(address);

        boolean foundCity = false;
        for (ConstraintViolation<Address> v : violations) {
            if (v.getPropertyPath().equals("city")) {
                // 默认 @NotNull 消息含 "must not be null"（命名属性 {min}/{max} 会被插值，
                // 位置占位符 {0} 是否插值取决于校验 Provider 配置，这里只断言稳定子串）
                assertTrue(v.getMessage().contains("must not be null"),
                        "city 默认消息: " + v.getMessage());
                foundCity = true;
            }
            if (v.getPropertyPath().equals("postcode")) {
                assertTrue(v.getMessage().contains("size must be between")
                                && v.getMessage().contains("6"),
                        "postcode 默认 Size 消息: " + v.getMessage());
            }
        }
        assertTrue(foundCity);
    }

    @Test
    @DisplayName("validateProperty 只校验指定属性")
    void testValidateProperty() {
        Address address = new Address(null, "123");
        Set<ConstraintViolation<Address>> violations = engine.validateProperty(address, "city");
        assertEquals(1, violations.size());
        assertEquals("city", violations.iterator().next().getPropertyPath());
    }

    // ============ 自定义校验器（标准 Jakarta ConstraintValidator） ============

    @Test
    @DisplayName("自定义 ConstraintValidator 注册与触发")
    void testCustomValidator() {
        assertTrue(engine.validate(new CustomAnnotated(4)).isEmpty());
        Set<ConstraintViolation<CustomAnnotated>> violations =
                engine.validate(new CustomAnnotated(5));
        assertEquals(1, violations.size());
        ConstraintViolation<CustomAnnotated> v = violations.iterator().next();
        assertEquals("value", v.getPropertyPath());
        assertTrue(v.getMessage().contains("must be even"), v.getMessage());
    }

    // ============ Validator 门面 ============

    @Test
    @DisplayName("Validator.validateOrThrow 抛出 ValidationException")
    void testValidatorFacade() {
        assertDoesNotThrow(() -> Validator.validateOrThrow(new Address("Beijing", "100000")));
        ValidationException ex = assertThrows(ValidationException.class,
                () -> Validator.validateOrThrow(new Address(null, "1")));
        assertEquals(2, ex.getViolations().size());
    }
}
