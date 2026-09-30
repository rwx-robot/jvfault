package com.jvfault.core.scopefixture;

import com.jvfault.core.annotation.Component;
import com.jvfault.core.annotation.Module;

@Module(providers = {RequestBean.class, ScopeSingletonBean.class})
public class ScopeModule {
}
