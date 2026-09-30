package com.jvfault.core.scopefixture;

import com.jvfault.core.annotation.Component;
import jakarta.annotation.PreDestroy;

@Component(scope = Component.Scope.REQUEST)
public class RequestBean {

    public int mark;
    public boolean destroyed;

    @PreDestroy
    public void onDestroy() {
        destroyed = true;
    }
}
