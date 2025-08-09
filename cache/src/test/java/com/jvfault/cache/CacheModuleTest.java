package com.jvfault.cache;

import com.jvfault.cache.annotation.CacheEvict;
import com.jvfault.cache.annotation.CachePut;
import com.jvfault.cache.annotation.Cacheable;
import com.jvfault.cache.cache.Cache;
import com.jvfault.cache.cache.CaffeineCache;
import com.jvfault.cache.cache.ConcurrentMapCache;
import com.jvfault.cache.cache.DefaultCacheManager;
import com.jvfault.cache.cache.MultiLevelCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;

/**
 * jvfault-cache 核心测试
 *
 * @since v0.7.0 (2021)
 */
@DisplayName("Cache 模块测试")
class CacheModuleTest {

    interface UserService {
        String getUser(int id);

        String updateUser(int id, String name);

        void evictUser(int id);

        void clearAll();
    }

    static class UserServiceImpl implements UserService {
        final AtomicInteger getUserCalls = new AtomicInteger();

        @Override
        @Cacheable(cacheNames = "users", key = "#p0")
        public String getUser(int id) {
            return "user-" + id + "-" + getUserCalls.incrementAndGet();
        }

        @Override
        @CachePut(cacheNames = "users", key = "#p0")
        public String updateUser(int id, String name) {
            return name;
        }

        @Override
        @CacheEvict(cacheNames = "users", key = "#p0")
        public void evictUser(int id) {
        }

        @Override
        @CacheEvict(cacheNames = "users", allEntries = true)
        public void clearAll() {
        }
    }

    // ============ 基础缓存 ============

    @Test
    @DisplayName("ConcurrentMapCache 基本操作")
    void testConcurrentMapCache() {
        ConcurrentMapCache cache = new ConcurrentMapCache("basic");
        assertNull(cache.get("k", String.class));
        cache.put("k", "v");
        assertEquals("v", cache.get("k", String.class));
        cache.evict("k");
        assertNull(cache.get("k", String.class));
        cache.put("k1", 1);
        cache.put("k2", 2);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("TTL 惰性过期")
    void testTtlExpiry() throws Exception {
        ConcurrentMapCache cache = new ConcurrentMapCache("ttl", 150);
        cache.put("k", "v");
        assertEquals("v", cache.get("k", String.class));
        Thread.sleep(250);
        assertNull(cache.get("k", String.class), "TTL 后应过期");
    }

    @Test
    @DisplayName("CaffeineCache 读写")
    void testCaffeineCache() {
        CaffeineCache cache = new CaffeineCache("caffeine", 1000, 100);
        cache.put("k", "v");
        assertEquals("v", cache.get("k", String.class));
        cache.evict("k");
        assertNull(cache.get("k", String.class));
    }

    @Test
    @DisplayName("多级缓存：L2 回填 L1 与穿透")
    void testMultiLevel() {
        ConcurrentMapCache l1 = new ConcurrentMapCache("l1");
        ConcurrentMapCache l2 = new ConcurrentMapCache("l2");
        MultiLevelCache multi = new MultiLevelCache(l1, l2);

        multi.put("k", "v");
        assertEquals("v", l1.get("k", String.class));

        l1.evict("k"); // L1 失效
        assertEquals("v", multi.get("k", String.class), "应从 L2 回填");
        assertEquals("v", l1.get("k", String.class), "回填后 L1 应命中");

        multi.evict("k");
        assertNull(multi.get("k", String.class), "两级都应失效");
    }

    @Test
    @DisplayName("DefaultCacheManager 惰性创建与命名")
    void testCacheManager() {
        DefaultCacheManager manager = new DefaultCacheManager();
        Cache first = manager.getCache("lazy");
        assertSame(first, manager.getCache("lazy"), "同名缓存应复用");
        assertTrue(manager.getCacheNames().contains("lazy"));
    }

    // ============ 注解拦截 ============

    @Test
    @DisplayName("@Cacheable 命中不执行方法")
    void testCacheable() {
        DefaultCacheManager manager = new DefaultCacheManager();
        CacheableBeanPostProcessor bpp = new CacheableBeanPostProcessor(manager);

        UserService service = (UserService) bpp.postProcessAfterInitialization(new UserServiceImpl(), "userService");
        UserServiceImpl impl = new UserServiceImpl();
        UserService proxied = (UserService) bpp.postProcessAfterInitialization(impl, "impl");

        String first = proxied.getUser(1);
        String second = proxied.getUser(1);
        assertEquals(first, second, "第二次应命中缓存");
        assertEquals(1, impl.getUserCalls.get(), "方法只应执行一次");
    }

    @Test
    @DisplayName("@CachePut 总是执行并更新")
    void testCachePut() {
        DefaultCacheManager manager = new DefaultCacheManager();
        CacheableBeanPostProcessor bpp = new CacheableBeanPostProcessor(manager);
        UserServiceImpl impl = new UserServiceImpl();
        UserService proxied = (UserService) bpp.postProcessAfterInitialization(impl, "impl");

        // 填充缓存 key=7 -> user-7-1
        proxied.getUser(7);
        assertEquals(1, impl.getUserCalls.get());

        String updated = proxied.updateUser(7, "renamed");
        assertEquals("renamed", updated);

        // 命中被 @CachePut 更新的缓存，方法不再执行
        assertEquals("renamed", proxied.getUser(7));
        assertEquals(1, impl.getUserCalls.get(), "getUser 不应再次执行");
    }

    @Test
    @DisplayName("@CacheEvict 单键与 allEntries")
    void testCacheEvict() {
        DefaultCacheManager manager = new DefaultCacheManager();
        CacheableBeanPostProcessor bpp = new CacheableBeanPostProcessor(manager);
        UserServiceImpl impl = new UserServiceImpl();
        UserService proxied = (UserService) bpp.postProcessAfterInitialization(impl, "impl");

        proxied.getUser(1);
        proxied.getUser(2);
        Cache users = manager.getCache("users");
        assertEquals(2, users.size());

        proxied.clearAll();
        assertEquals(0, users.size(), "allEntries 应清空缓存");
    }

    @Test
    @DisplayName("无注解 Bean 原样返回")
    void testNoAnnotationPassthrough() {
        DefaultCacheManager manager = new DefaultCacheManager();
        CacheableBeanPostProcessor bpp = new CacheableBeanPostProcessor(manager);
        Object plain = new Object();
        assertSame(plain, bpp.postProcessAfterInitialization(plain, "plain"));
    }
}
