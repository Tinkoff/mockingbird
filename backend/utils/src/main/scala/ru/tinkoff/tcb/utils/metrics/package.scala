package ru.tinkoff.tcb.utils

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

package object metrics {

  @inline def tag(key: String, value: String): Tag = new ImmutableTag(key, value)

  private[tcb] def makeRegistry(name: String): PrometheusMeterRegistry =
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT) {
      config().commonTags("application", name)
    }.tap { registry =>
      // Logback Metrics
      new LogbackMetrics().bindTo(registry)
      // JVM Metrics
      new ClassLoaderMetrics().bindTo(registry)
      new JvmMemoryMetrics().bindTo(registry)
      new JvmGcMetrics().bindTo(registry)
      new JvmThreadMetrics().bindTo(registry)
      // JVM Extra Metrics
      new ProcessMemoryMetrics().bindTo(registry);
      new ProcessThreadMetrics().bindTo(registry);
      // System metrics
      new ProcessorMetrics().bindTo(registry)
      new UptimeMetrics().bindTo(registry)
      new FileDescriptorMetrics().bindTo(registry)
    }
}
