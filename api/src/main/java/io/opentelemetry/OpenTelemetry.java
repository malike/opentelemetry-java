/*
 * Copyright 2019, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry;

import io.opentelemetry.metrics.DefaultMeter;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.metrics.spi.MeterProvider;
import io.opentelemetry.tags.DefaultTagger;
import io.opentelemetry.tags.Tagger;
import io.opentelemetry.tags.spi.TaggerProvider;
import io.opentelemetry.trace.DefaultTracer;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.spi.TracerProvider;
import java.util.ServiceLoader;
import javax.annotation.Nullable;

/**
 * This class provides a static global accessor for telemetry objects {@link Tracer}, {@link Meter}
 * and {@link Tagger}.
 *
 * <p>The telemetry objects are lazy-loaded singletons resolved via {@link ServiceLoader} mechanism.
 *
 * @see TracerProvider
 * @see MeterProvider
 * @see TaggerProvider
 */
public final class OpenTelemetry {

  @Nullable private static volatile OpenTelemetry instance;

  private final Tracer tracer;
  private final Meter meter;
  private final Tagger tagger;

  /**
   * Returns a singleton {@link Tracer}.
   *
   * @return registered tracer or default via {@link DefaultTracer#getInstance()}.
   * @throws IllegalStateException if a specified tracer (via system properties) could not be found.
   * @since 0.1.0
   */
  public static Tracer getTracer() {
    return getInstance().tracer;
  }

  /**
   * Returns a singleton {@link Meter}.
   *
   * @return registered meter or default via {@link DefaultMeter#getInstance()}.
   * @throws IllegalStateException if a specified meter (via system properties) could not be found.
   * @since 0.1.0
   */
  public static Meter getMeter() {
    return getInstance().meter;
  }

  /**
   * Returns a singleton {@link Tagger}.
   *
   * @return registered meter or default via {@link DefaultTagger#getInstance()}.
   * @throws IllegalStateException if a specified meter (via system properties) could not be found.
   * @since 0.1.0
   */
  public static Tagger getTagger() {
    return getInstance().tagger;
  }

  /**
   * Load provider class via {@link ServiceLoader}. A specific provider class can be requested via
   * setting a system property with FQCN.
   *
   * @param providerClass a provider class
   * @param <T> provider type
   * @return a provider or null if not found
   * @throws IllegalStateException if a specified provider is not found
   */
  @Nullable
  private static <T> T loadSpi(Class<T> providerClass) {
    String specifiedProvider = System.getProperty(providerClass.getName());
    ServiceLoader<T> providers = ServiceLoader.load(providerClass);
    for (T provider : providers) {
      if (specifiedProvider == null || specifiedProvider.equals(provider.getClass().getName())) {
        return provider;
      }
    }
    if (specifiedProvider != null) {
      throw new IllegalStateException(String.format("Tracer %s not found", specifiedProvider));
    }
    return null;
  }

  /** Lazy loads an instance. */
  private static OpenTelemetry getInstance() {
    if (instance == null) {
      synchronized (OpenTelemetry.class) {
        if (instance == null) {
          instance = new OpenTelemetry();
        }
      }
    }
    return instance;
  }

  private OpenTelemetry() {
    TracerProvider tracerProvider = loadSpi(TracerProvider.class);
    tracer = tracerProvider != null ? tracerProvider.create() : DefaultTracer.getInstance();
    MeterProvider meterProvider = loadSpi(MeterProvider.class);
    meter = meterProvider != null ? meterProvider.create() : DefaultMeter.getInstance();
    TaggerProvider taggerProvider = loadSpi(TaggerProvider.class);
    tagger = taggerProvider != null ? taggerProvider.create() : DefaultTagger.getInstance();
  }

  // for testing
  static void reset() {
    instance = null;
  }
}
