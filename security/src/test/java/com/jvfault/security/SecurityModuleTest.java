package com.jvfault.security;

import com.jvfault.security.crypto.PasswordHasher;
import com.jvfault.security.guard.JwtGuard;
import com.jvfault.security.jwt.JwtService;
import com.jvfault.web.http.HttpContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * jvfault-security 核心测试
 *
 * @since v1.0.0 (2026)
 */
@DisplayName("Security 模块测试")
class SecurityModuleTest {

    // ============ JWT ============

    @Test
    @DisplayName("JWT 签发与校验往返")
    void testJwtRoundtrip() {
        JwtService jwt = new JwtService("test-secret-key-123", 60);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "user-42");
        claims.put("role", "admin");

        String token = jwt.issue(claims);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT 应有三段");

        Map<String, Object> verified = jwt.verify(token);
        assertNotNull(verified);
        assertEquals("user-42", verified.get("sub"));
        assertEquals("admin", verified.get("role"));
        assertNotNull(verified.get("exp"));
    }

    @Test
    @DisplayName("JWT 过期返回 null")
    void testJwtExpiry() {
        JwtService jwt = new JwtService("test-secret-key-123", 1);
        String token = jwt.issue(new LinkedHashMap<>());
        // 回溯 exp：签发时 iat/exp 已定，等待 2 秒
        try {
            Thread.sleep(2100);
        } catch (InterruptedException ignored) {
        }
        assertNull(jwt.verify(token));
    }

    @Test
    @DisplayName("JWT 篡改与密钥不符均拒绝")
    void testJwtTamper() {
        JwtService jwt = new JwtService("test-secret-key-123", 60);
        String token = jwt.issue(new LinkedHashMap<String, Object>() {{ put("sub", "a"); }});

        // 篡改 payload
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + com.jvfault.security.TestSupport.b64url("{\"sub\":\"hacker\"}") + "." + parts[2];
        assertNull(jwt.verify(tampered), "篡改后应签名失败");

        // 换密钥
        JwtService other = new JwtService("another-secret-key", 60);
        assertNull(other.verify(token));
    }

    // ============ 口令哈希 ============

    @Test
    @DisplayName("PBKDF2 哈希与校验")
    void testPasswordHasher() {
        PasswordHasher hasher = new PasswordHasher();
        String stored = hasher.hash("S3cret!pass");
        assertTrue(stored.startsWith("pbkdf2$"), "格式: pbkdf2$iter$salt$hash");
        assertNotEquals("S3cret!pass", stored);

        assertTrue(hasher.verify("S3cret!pass", stored));
        assertFalse(hasher.verify("wrong", stored));
        assertFalse(hasher.verify(null, stored));

        // 相同口令两次哈希盐不同
        assertNotEquals(hasher.hash("S3cret!pass"), hasher.hash("S3cret!pass"));
    }

    // ============ JWT Guard ============

    @Test
    @DisplayName("JwtGuard 校验 Bearer 头与角色")
    void testJwtGuard() {
        JwtService jwt = new JwtService("test-secret-key-123", 60);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "user-42");
        claims.put("role", "admin");
        String token = jwt.issue(claims);

        JwtGuard guard = new JwtGuard(jwt, "admin");
        HttpContext context = com.jvfault.security.TestSupport.MockRequestHelperHelper.withBearer(token);

        assertTrue(guard.canActivate(context));
        Map<String, Object> injected = JwtGuard.claimsOf(context);
        assertEquals("user-42", injected.get("sub"));

        // 无头拒绝
        HttpContext anonymous = com.jvfault.security.TestSupport.MockRequestHelperHelper.empty();
        assertFalse(guard.canActivate(anonymous));

        // 角色不符拒绝
        JwtGuard userOnly = new JwtGuard(jwt, "user");
        Map<String, Object> userClaims = new LinkedHashMap<>();
        userClaims.put("role", "admin");
        HttpContext wrongRole = com.jvfault.security.TestSupport.MockRequestHelperHelper
                .withBearer(jwt.issue(userClaims));
        assertFalse(userOnly.canActivate(wrongRole));
    }
}
