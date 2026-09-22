package com.jvfault.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器 - 发现、隔离加载、依赖排序与生命周期管理。
 *
 * <p>状态机：CREATED → INITIALIZED → STARTED → STOPPED（非法迁移抛异常）。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    public enum State { CREATED, INITIALIZED, STARTED, STOPPED, FAILED }

    /** 插件实例 + 隔离 classloader + 状态 */
    public static final class Loaded {
        public final Plugin plugin;
        public final PluginDescriptor descriptor;
        public final ClassLoader classLoader;
        public final PluginContext context;
        public volatile State state = State.CREATED;

        Loaded(Plugin plugin, PluginDescriptor descriptor, ClassLoader classLoader, PluginContext context) {
            this.plugin = plugin;
            this.descriptor = descriptor;
            this.classLoader = classLoader;
            this.context = context;
        }
    }

    private final ConcurrentHashMap<String, Loaded> plugins = new ConcurrentHashMap<>();
    private final Map<String, String> sharedConfig;
    private final boolean parentFirst;

    public PluginManager() {
        this(Collections.emptyMap(), false);
    }

    public PluginManager(Map<String, String> sharedConfig, boolean parentFirst) {
        this.sharedConfig = sharedConfig;
        this.parentFirst = parentFirst;
    }

    // ============ 发现与加载 ============

    /**
     * 从目录加载全部 *.jar 插件（URLClassLoader 隔离）。
     */
    public int loadFromDirectory(File directory) {
        File[] jars = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) {
            throw new PluginException("插件目录不可读: " + directory);
        }
        for (File jar : jars) {
            loadJar(jar);
        }
        return jars.length;
    }

    /**
     * 从单个 jar 加载。
     */
    public void loadJar(File jar) {
        PluginDescriptor descriptor = PluginDescriptor.load(jar);
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jar.toURI().toURL()},
                    parentFirst ? getClass().getClassLoader() : null);
            ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, classLoader);
            List<Plugin> found = new ArrayList<>();
            for (Plugin p : loader) {
                found.add(p);
            }
            if (found.isEmpty()) {
                throw new PluginException("jar 中未发现 Plugin 实现: " + jar.getName());
            }
            for (Plugin plugin : found) {
                PluginContext context = new PluginContext(
                        descriptor.getId(), classLoader, sharedConfig, registry());
                plugins.put(descriptor.getId(), new Loaded(plugin, descriptor, classLoader, context));
            }
            log.info("Loaded plugin: {} v{}", descriptor.getId(), descriptor.getVersion());
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException("插件加载失败: " + jar, e);
        }
    }

    /**
     * 从当前 classpath 发现插件（测试与内嵌场景）。
     */
    public int loadFromClasspath() {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, getClass().getClassLoader());
        int count = 0;
        for (Plugin plugin : loader) {
            PluginDescriptor descriptor = new PluginDescriptor(
                    plugin.getId(), plugin.getVersion(), plugin.dependencies(), null);
            PluginContext context = new PluginContext(
                    plugin.getId(), getClass().getClassLoader(), sharedConfig, registry());
            plugins.put(plugin.getId(), new Loaded(plugin, descriptor, getClass().getClassLoader(), context));
            count++;
        }
        return count;
    }

    // ============ 生命周期 ============

    /**
     * 按依赖拓扑顺序 init + start 全部插件。
     */
    public void startAll() {
        for (String id : topologicalOrder()) {
            start(id);
        }
    }

    public synchronized void start(String id) {
        Loaded loaded = require(id);
        if (loaded.state == State.INITIALIZED || loaded.state == State.CREATED) {
            try {
                if (loaded.state == State.CREATED) {
                    loaded.plugin.init(loaded.context);
                    loaded.state = State.INITIALIZED;
                }
                loaded.plugin.start();
            } catch (Exception e) {
                loaded.state = State.FAILED;
                throw new PluginException("插件启动失败: " + id, e);
            }
            loaded.state = State.STARTED;
            log.info("Started plugin: {}", id);
        } else {
            throw new PluginException("非法状态迁移: " + id + " " + loaded.state + " -> STARTED");
        }
    }

    public synchronized void stop(String id) {
        Loaded loaded = require(id);
        if (loaded.state != State.STARTED) {
            throw new PluginException("非法状态迁移: " + id + " " + loaded.state + " -> STOPPED");
        }
        try {
            loaded.plugin.stop();
            loaded.state = State.STOPPED;
            log.info("Stopped plugin: {}", id);
        } catch (Exception e) {
            loaded.state = State.FAILED;
            throw new PluginException("插件停止失败: " + id, e);
        }
    }

    public void stopAll() {
        // 反拓扑顺序停止
        List<String> order = topologicalOrder();
        Collections.reverse(order);
        for (String id : order) {
            Loaded loaded = plugins.get(id);
            if (loaded != null && loaded.state == State.STARTED) {
                stop(id);
            }
        }
    }

    // ============ 查询 ============

    public Loaded get(String id) {
        return plugins.get(id);
    }

    public Collection<Loaded> getAll() {
        return plugins.values();
    }

    /** 依赖拓扑排序（Kahn），缺依赖/循环依赖抛异常 */
    public List<String> topologicalOrder() {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> dependents = new LinkedHashMap<>();
        for (String id : plugins.keySet()) {
            inDegree.putIfAbsent(id, 0);
            for (String dep : plugins.get(id).descriptor.getDependencies()) {
                if (!plugins.containsKey(dep)) {
                    throw new PluginException("插件 " + id + " 依赖缺失: " + dep);
                }
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(id);
                inDegree.merge(id, 1, Integer::sum);
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.poll();
            order.add(id);
            for (String dependent : dependents.getOrDefault(id, Collections.emptyList())) {
                int left = inDegree.merge(dependent, -1, Integer::sum);
                if (left == 0) {
                    ready.add(dependent);
                }
            }
        }
        if (order.size() != plugins.size()) {
            List<String> cycle = new ArrayList<>(plugins.keySet());
            cycle.removeAll(order);
            throw new PluginException("插件循环依赖: " + cycle);
        }
        return order;
    }

    private PluginRegistry registry;

    private PluginRegistry registry() {
        if (registry == null) {
            registry = new PluginRegistry(this);
        }
        return registry;
    }

    private Loaded require(String id) {
        Loaded loaded = plugins.get(id);
        if (loaded == null) {
            throw new PluginException("插件未加载: " + id);
        }
        return loaded;
    }
}
