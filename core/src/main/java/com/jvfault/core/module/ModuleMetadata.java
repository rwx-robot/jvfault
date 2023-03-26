package com.jvfault.core.module;

import com.jvfault.core.annotation.Module;
import com.jvfault.core.container.BeanDefinition;
import java.util.*;

/**
 * 模块元数据 - 描述模块的结构信息
 * 
 * <p>由注解处理器 (APT) 在编译时生成，运行时直接读取，避免反射开销。
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public class ModuleMetadata {

    private final Class<?> moduleClass;
    private final Module moduleAnnotation;
    private final List<Class<?>> providers = new ArrayList<>();
    private final List<Class<?>> controllers = new ArrayList<>();
    private final List<Class<?>> imports = new ArrayList<>();
    private final List<Class<?>> exports = new ArrayList<>();
    private final boolean global;

    // 运行时解析后的 BeanDefinitions
    private final Map<String, BeanDefinition> providerDefinitions = new LinkedHashMap<>();
    private final Map<String, BeanDefinition> controllerDefinitions = new LinkedHashMap<>();

    // 模块依赖图
    private final Set<ModuleMetadata> importedModules = new LinkedHashSet<>();
    private final Set<ModuleMetadata> importingModules = new LinkedHashSet<>();
    private final Set<ModuleMetadata> exportedToModules = new LinkedHashSet<>();

    // 拓扑排序状态
    private int topologicalOrder = -1;
    private boolean visited = false;
    private boolean visiting = false;

    public ModuleMetadata(Class<?> moduleClass) {
        this.moduleClass = moduleClass;
        this.moduleAnnotation = moduleClass.getAnnotation(Module.class);
        if (this.moduleAnnotation != null) {
            this.providers.addAll(Arrays.asList(moduleAnnotation.providers()));
            this.controllers.addAll(Arrays.asList(moduleAnnotation.controllers()));
            this.imports.addAll(Arrays.asList(moduleAnnotation.imports()));
            this.exports.addAll(Arrays.asList(moduleAnnotation.exports()));
            this.global = moduleAnnotation.global();
        } else {
            this.global = false;
        }
    }

    // ============ Getters ============

    public Class<?> getModuleClass() { return moduleClass; }
    public Module getModuleAnnotation() { return moduleAnnotation; }
    public List<Class<?>> getProviders() { return providers; }
    public List<Class<?>> getControllers() { return controllers; }
    public List<Class<?>> getImports() { return imports; }
    public List<Class<?>> getExports() { return exports; }
    public boolean isGlobal() { return global; }

    public Map<String, BeanDefinition> getProviderDefinitions() { return providerDefinitions; }
    public Map<String, BeanDefinition> getControllerDefinitions() { return controllerDefinitions; }

    public Set<ModuleMetadata> getImportedModules() { return importedModules; }
    public Set<ModuleMetadata> getImportingModules() { return importingModules; }
    public Set<ModuleMetadata> getExportedToModules() { return exportedToModules; }

    // ============ 图操作 ============

    public void addImportedModule(ModuleMetadata imported) {
        importedModules.add(imported);
        imported.importingModules.add(this);
    }

    public void addExportedToModule(ModuleMetadata importer) {
        exportedToModules.add(importer);
    }

    // ============ 拓扑排序支持 ============

    public int getTopologicalOrder() { return topologicalOrder; }
    public void setTopologicalOrder(int order) { this.topologicalOrder = order; }

    public boolean isVisited() { return visited; }
    public void setVisited(boolean visited) { this.visited = visited; }

    public boolean isVisiting() { return visiting; }
    public void setVisiting(boolean visiting) { this.visiting = visiting; }

    public void resetTopologicalState() {
        this.topologicalOrder = -1;
        this.visited = false;
        this.visiting = false;
    }

    // ============ 实用方法 ============

    public String getModuleName() {
        return moduleClass.getSimpleName();
    }

    public String getModuleKey() {
        return moduleClass.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleMetadata)) return false;
        ModuleMetadata that = (ModuleMetadata) o;
        return moduleClass.equals(that.moduleClass);
    }

    @Override
    public int hashCode() {
        return moduleClass.hashCode();
    }

    @Override
    public String toString() {
        return "ModuleMetadata{" +
                "name='" + getModuleName() + '\'' +
                ", providers=" + providers.size() +
                ", controllers=" + controllers.size() +
                ", imports=" + imports.size() +
                ", exports=" + exports.size() +
                ", global=" + global +
                '}';
    }
}