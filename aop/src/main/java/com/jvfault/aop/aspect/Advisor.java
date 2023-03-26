package com.jvfault.aop.aspect;

import com.jvfault.aop.advice.AdviceMethod;
import com.jvfault.aop.advice.AdviceScanner;
import com.jvfault.aop.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 切面 Advisor - 一个 @Aspect Bean 及其通知列表。
 *
 * @since v0.2.0 (2016)
 * @author jvfault team
 */
public class Advisor {

    private final Object aspectInstance;
    private final int order;
    private final List<AdviceMethod> advices;

    public Advisor(Object aspectInstance) {
        this.aspectInstance = aspectInstance;
        Order orderAnn = aspectInstance.getClass().getAnnotation(Order.class);
        this.order = orderAnn != null ? orderAnn.value() : Integer.MAX_VALUE;
        this.advices = Collections.unmodifiableList(AdviceScanner.scan(aspectInstance));
    }

    public Object getAspectInstance() {
        return aspectInstance;
    }

    public int getOrder() {
        return order;
    }

    public List<AdviceMethod> getAdvices() {
        return advices;
    }

    public List<AdviceMethod> getAdvicesOfKind(AdviceMethod.AdviceKind kind) {
        List<AdviceMethod> result = new ArrayList<>();
        for (AdviceMethod advice : advices) {
            if (advice.getKind() == kind) {
                result.add(advice);
            }
        }
        return result;
    }
}
