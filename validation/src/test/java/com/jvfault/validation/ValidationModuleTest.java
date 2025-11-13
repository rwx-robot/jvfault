package com.jvfault.validation;

import com.jvfault.validation.constraint.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-validation 核心测试
 *
 * @since v0.2.0 (2016)
 */
@DisplayName("Validation 模块测试")
class ValidationModuleTest {

    // ============ 测试模型 ============

    static class Address {
        @NotNull(message = "{field} is required")
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
        private final List<Item> itemObjects = new ArrayList<>();

        Order(Item... items) {
            this.itemObjects.addAll(Arrays.asList(items));
        }
    }

    static class CustomAnnotated {
        @EvenNumber
        private final int value;

        CustomAnnotated(int value) {
            this.value = value;
        }
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @interface EvenNumber {
        String message() default "{field} must be even";
    }

    private final DefaultValidationEngine engine = new DefaultValidationEngine();

    // ============ 基础约束 ============

    @Test
    @DisplayName("合法对象通过校验")
    void testValidObject() {
        User user = new User("Alice", "alice@example.com", 25, "13800138000",
                "alice", 100, Instant.now().minusSeconds(20 * 365 * 24 * 3600L),
                new Address("Beijing", "100000"));
        assertTrue(engine.validate(user).isEmpty());
    }

    @Test
    @DisplayName("@NotNull / @NotBlank / @Size 违反")
    void testBasicViolations() {
        User user = new User("A", null, 25, "13800138000", "", 100,
                Instant.now().minusSeconds(20 * 365 * 24 * 3600L), null);
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
                Instant.now().minusSeconds(20 * 365 * 24 * 3600L), null);
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
                Instant.now().minusSeconds(20 * 365 * 24 * 3600L), null);
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
                Instant.now().minusSeconds(20 * 365 * 24 * 3600L), null);
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
                Instant.now().minusSeconds(20 * 365 * 24 * 3600L), badAddress);

        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<User> v : engine.validate(user)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("address.city"), "级联路径 address.city: " + paths);
        assertTrue(paths.contains("address.postcode"), "级联路径 address.postcode");
    }

    @Test
    @DisplayName("集合元素级联，路径带下标")
    void testCollectionCascade() {
        Order order = new Order(new Item("SKU-1"), new Item(null));
        Set<String> paths = new HashSet<>();
        for (ConstraintViolation<Order> v : engine.validate(order)) {
            paths.add(v.getPropertyPath());
        }
        assertTrue(paths.contains("itemObjects[1].sku"), "带下标的级联路径: " + paths);
    }

    // ============ 消息插值 / validateProperty ============

    @Test
    @DisplayName("消息模板插值 {field} 与注解属性")
    void testMessageInterpolation() {
        Address address = new Address(null, "123");
        Set<ConstraintViolation<Address>> violations = engine.validate(address);

        boolean found = false;
        for (ConstraintViolation<Address> v : violations) {
            if (v.getPropertyPath().equals("city")) {
                assertEquals("city is required", v.getMessage());
                found = true;
            }
            if (v.getPropertyPath().equals("postcode")) {
                assertEquals("postcode size must be between 6 and 6", v.getMessage());
            }
        }
        assertTrue(found);
    }

    @Test
    @DisplayName("validateProperty 只校验指定属性")
    void testValidateProperty() {
        Address address = new Address(null, "123");
        Set<ConstraintViolation<Address>> violations = engine.validateProperty(address, "city");
        assertEquals(1, violations.size());
        assertEquals("city", violations.iterator().next().getPropertyPath());
    }

    // ============ 自定义校验器 ============

    @Test
    @DisplayName("自定义 ConstraintValidator 注册与触发")
    void testCustomValidator() {
        engine.registerValidator(EvenNumber.class,
                (value, ctx) -> value == null || ((Number) value).intValue() % 2 == 0);

        assertTrue(engine.validate(new CustomAnnotated(4)).isEmpty());
        Set<ConstraintViolation<CustomAnnotated>> violations = engine.validate(new CustomAnnotated(5));
        assertEquals(1, violations.size());
        assertEquals("value must be even", violations.iterator().next().getMessage());
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
