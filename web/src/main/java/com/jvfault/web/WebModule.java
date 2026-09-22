package com.jvfault.web;

import com.jvfault.core.annotation.Module;
import com.jvfault.web.exception.GlobalExceptionResolver;

/**
 * Web 模块 - 装配全局异常解析器等 Web 基础设施。
 * 控制器由应用模块 providers 声明（@Controller + @Component）。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
@Module(providers = {GlobalExceptionResolver.class})
public class WebModule {
}
