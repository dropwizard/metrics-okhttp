/*
 * Copyright 2015 Ras Kasa Williams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raskasa.metrics.okhttp;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import java.io.IOException;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wraps an {@link OkHttpClient} in order to provide data about its internals. */
final class InstrumentedOkHttpClient extends OkHttpClient {
  private static final Logger LOG = LoggerFactory.getLogger(InstrumentedOkHttpClient.class);

  public static OkHttpClient build(MetricRegistry registry, OkHttpClient rawClient, String name) {
    Builder builder = rawClient.newBuilder();
    EventListener.Factory eventListenerFactory = rawClient.eventListenerFactory();
    instrumentNetworkRequests(builder, registry, name);
    instrumentEventListener(builder, eventListenerFactory, registry, name);

    OkHttpClient client = builder.build();
    if (rawClient.cache() != null) {
      instrumentHttpCache(client, registry, name);
    }
    instrumentConnectionPool(client, registry, name);

    return client;
  }

  /**
   * Generates an identifier, with a common prefix, in order to uniquely identify the {@code metric}
   * in the registry.
   *
   * <p>The generated identifier includes:
   *
   * <ul>
   *   <li>the fully qualified name of the {@link OkHttpClient} class
   *   <li>the name of the instrumented client, if provided
   *   <li>the given {@code metric}
   * </ul>
   */
  static String metricId(String name, String metric) {
    return name(OkHttpClient.class, name, metric);
  }

  private static void instrumentHttpCache(
      OkHttpClient client, MetricRegistry registry, String name) {
    registry.register(
        metricId(name, "cache-request-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // The number of HTTP requests issued since this cache was created.
            return client.cache().requestCount();
          }
        });
    registry.register(
        metricId(name, "cache-hit-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // ... the number of those requests that required network use.
            return client.cache().hitCount();
          }
        });
    registry.register(
        metricId(name, "cache-network-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // ... the number of those requests whose responses were served by the cache.
            return client.cache().networkCount();
          }
        });
    registry.register(
        metricId(name, "cache-write-success-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return client.cache().writeSuccessCount();
          }
        });
    registry.register(
        metricId(name, "cache-write-abort-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return client.cache().writeAbortCount();
          }
        });
    final Gauge<Long> currentCacheSize =
        new Gauge<Long>() {
          @Override
          public Long getValue() {
            try {
              return client.cache().size();
            } catch (IOException ex) {
              LOG.error(ex.getMessage(), ex);
              return -1L;
            }
          }
        };
    final Gauge<Long> maxCacheSize =
        new Gauge<Long>() {
          @Override
          public Long getValue() {
            return client.cache().maxSize();
          }
        };
    registry.register(metricId(name, "cache-current-size"), currentCacheSize);
    registry.register(metricId(name, "cache-max-size"), maxCacheSize);
    registry.register(
        metricId(name, "cache-size"),
        new RatioGauge() {
          @Override
          protected Ratio getRatio() {
            return Ratio.of(currentCacheSize.getValue(), maxCacheSize.getValue());
          }
        });
  }

  private static void instrumentConnectionPool(
      OkHttpClient client, MetricRegistry registry, String name) {
    registry.register(
        metricId(name, "connection-pool-total-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return client.connectionPool().connectionCount();
          }
        });
    registry.register(
        metricId(name, "connection-pool-idle-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return client.connectionPool().idleConnectionCount();
          }
        });
  }

  private static void instrumentNetworkRequests(
      Builder builder, MetricRegistry registry, String name) {
    builder.addNetworkInterceptor(
        new InstrumentedInterceptor(registry, name(OkHttpClient.class, name)));
  }

  private static void instrumentEventListener(
      Builder builder,
      EventListener.Factory eventListenerFactory,
      MetricRegistry registry,
      String name) {
    builder.eventListenerFactory(
        new InstrumentedEventListener.Factory(
            registry, eventListenerFactory, name(EventListener.class, name)));
  }
}
