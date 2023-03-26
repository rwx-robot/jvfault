package com.jvfault.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 插件描述符（id/version/dependencies，来自 jar 内
 * META-INF/jvfault-plugin.properties）。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
public class PluginDescriptor {

    private final String id;
    private final String version;
    private final java.util.List<String> dependencies;
    private final File jarLocation;

    public PluginDescriptor(String id, String version, java.util.List<String> dependencies, File jarLocation) {
        this.id = id;
        this.version = version;
        this.dependencies = dependencies;
        this.jarLocation = jarLocation;
    }

    public static PluginDescriptor load(File jar) {
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jar)) {
            java.util.jar.JarEntry entry = jarFile.getJarEntry("META-INF/jvfault-plugin.properties");
            if (entry == null) {
                throw new PluginException("缺少 META-INF/jvfault-plugin.properties: " + jar.getName());
            }
            Properties props = new Properties();
            try (InputStream in = jarFile.getInputStream(entry)) {
                props.load(in);
            }
            String id = props.getProperty("id");
            if (id == null || id.trim().isEmpty()) {
                throw new PluginException("描述符缺少 id: " + jar.getName());
            }
            java.util.List<String> deps = new java.util.ArrayList<>();
            String raw = props.getProperty("dependencies", "");
            for (String d : raw.split(",")) {
                if (!d.trim().isEmpty()) {
                    deps.add(d.trim());
                }
            }
            return new PluginDescriptor(id.trim(), props.getProperty("version", "0.0.0"), deps, jar);
        } catch (IOException e) {
            throw new PluginException("无法读取插件 jar: " + jar, e);
        }
    }

    public String getId() { return id; }
    public String getVersion() { return version; }
    public java.util.List<String> getDependencies() { return dependencies; }
    public File getJarLocation() { return jarLocation; }
}
