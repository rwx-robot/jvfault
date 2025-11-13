package com.jvfault.core.spi;

import com.jvfault.core.module.ModuleMetadata;

import java.util.Map;

/**
 * 模块扫描器 SPI 接口
 * 允许第三方扩展扫描策略 (如: 基于注解处理器生成的索引扫描)
 * 
 * @since v0.1.0 (2015)
 * @author jvfault team
 */
public interface ModuleScanner {

    /**
     * 扫描模块
     * 
     * @param basePackages 基础包路径
     * @return 模块类名 -> ModuleMetadata 映射
     */
    Map<String, ModuleMetadata> scan(String[] basePackages);

    /**
     * 获取扫描器优先级 (数值越大优先级越高)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 获取扫描器名称
     */
    String getName();
}