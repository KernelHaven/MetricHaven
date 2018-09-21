package net.ssehub.kernel_haven.metric_haven.code_metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the constructor of an {@link AbstractFunctionMetric} to be used by the {@link MetricFactory}.
 * @author El-Sharkawy
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@interface PreferedConstructor {

}
