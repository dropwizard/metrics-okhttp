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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RecordingEventListener;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class InstrumentedOkHttpClientTest {
  private MetricRegistry registry;
  private OkHttpClient rawClient;

  @Before
  public void setUp() throws Exception {
    registry = new MetricRegistry();
    rawClient = new OkHttpClient();
  }

  @Rule public MockWebServer server = new MockWebServer();
  @Rule public TemporaryFolder cacheRule = new TemporaryFolder();

  @Test
  public void syncNetworkRequestsAreInstrumented() throws IOException {
    MockResponse mockResponse =
        new MockResponse()
            .addHeader("Cache-Control:public, max-age=31536000")
            .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
            .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
            .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-submitted"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getCounters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-running"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-completed"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getTimers()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-duration"))
                .getCount())
        .isEqualTo(0);

    Request request = new Request.Builder().url(baseUrl).build();

    try (Response response = client.newCall(request).execute()) {
      assertThat(
              registry
                  .getMeters()
                  .get(InstrumentedOkHttpClient.metricId(null, "network-requests-submitted"))
                  .getCount())
          .isEqualTo(1);
      assertThat(
              registry
                  .getCounters()
                  .get(InstrumentedOkHttpClient.metricId(null, "network-requests-running"))
                  .getCount())
          .isEqualTo(0);
      assertThat(
              registry
                  .getMeters()
                  .get(InstrumentedOkHttpClient.metricId(null, "network-requests-completed"))
                  .getCount())
          .isEqualTo(1);
      assertThat(
              registry
                  .getTimers()
                  .get(InstrumentedOkHttpClient.metricId(null, "network-requests-duration"))
                  .getCount())
          .isEqualTo(1);
    }
  }

  @Test
  public void aSyncNetworkRequestsAreInstrumented() {
    MockResponse mockResponse =
        new MockResponse()
            .addHeader("Cache-Control:public, max-age=31536000")
            .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
            .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
            .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    final OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-submitted"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getCounters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-running"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-completed"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getTimers()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-duration"))
                .getCount())
        .isEqualTo(0);

    final Request request = new Request.Builder().url(baseUrl).build();

    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                fail();
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                assertThat(
                        registry
                            .getMeters()
                            .get(
                                InstrumentedOkHttpClient.metricId(
                                    null, "network-requests-submitted"))
                            .getCount())
                    .isEqualTo(1);
                assertThat(
                        registry
                            .getCounters()
                            .get(
                                InstrumentedOkHttpClient.metricId(null, "network-requests-running"))
                            .getCount())
                    .isEqualTo(0);
                assertThat(
                        registry
                            .getMeters()
                            .get(
                                InstrumentedOkHttpClient.metricId(
                                    null, "network-requests-completed"))
                            .getCount())
                    .isEqualTo(1);
                assertThat(
                        registry
                            .getTimers()
                            .get(
                                InstrumentedOkHttpClient.metricId(
                                    null, "network-requests-duration"))
                            .getCount())
                    .isEqualTo(1);
                response.body().close();
              }
            });
  }

  @Test
  public void httpCacheIsInstrumented() throws Exception {
    MockResponse mockResponse =
        new MockResponse()
            .addHeader("Cache-Control:public, max-age=31536000")
            .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
            .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
            .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    Cache cache = new Cache(cacheRule.getRoot(), Long.MAX_VALUE);
    rawClient = rawClient.newBuilder().cache(cache).build();
    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "cache-max-size"))
                .getValue())
        .isEqualTo(Long.MAX_VALUE);
    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "cache-current-size"))
                .getValue())
        .isEqualTo(0L);

    Request request = new Request.Builder().url(baseUrl).build();
    Response response = client.newCall(request).execute();

    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "cache-current-size"))
                .getValue())
        .isEqualTo(rawClient.cache().size());

    response.body().close();
  }

  @Test
  public void connectionPoolIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "connection-pool-total-count"))
                .getValue())
        .isEqualTo(0);
    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "connection-pool-idle-count"))
                .getValue())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "connection-pool-total-count"))
                .getValue())
        .isEqualTo(2);
    assertThat(
            registry
                .getGauges()
                .get(InstrumentedOkHttpClient.metricId(null, "connection-pool-idle-count"))
                .getValue())
        .isEqualTo(0);

    resp1.body().close();
    resp2.body().close();
  }

  @Test
  public void eventListenerIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "calls-start"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "calls-end"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "calls-failed"));
    assertThat(registry.getTimers().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "calls-duration"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "dns-start"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "dns-end"));
    assertThat(registry.getTimers().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "dns-duration"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-start"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-end"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-failed"));
    assertThat(registry.getTimers().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-duration"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-acquired"));
    assertThat(registry.getMeters().keySet())
        .doesNotContain(MetricRegistry.name(EventListener.class, "connections-released"));

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-start"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-end"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-failed"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getTimers()
                .get(MetricRegistry.name(EventListener.class, "calls-duration"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "dns-start"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "dns-end"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getTimers()
                .get(MetricRegistry.name(EventListener.class, "dns-duration"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-start"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-end"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-failed"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getTimers()
                .get(MetricRegistry.name(EventListener.class, "connections-duration"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-acquired"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-released"))
                .getCount())
        .isEqualTo(0);

    // Some end/release events don't fire until after the response bodies are closed/consumed.
    resp1.close();
    resp2.close();

    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-start"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-end"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "calls-failed"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getTimers()
                .get(MetricRegistry.name(EventListener.class, "calls-duration"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(MetricRegistry.name(EventListener.class, "connections-released"))
                .getCount())
        .isEqualTo(2);
  }

  @Test
  public void eventListenerDelegatesSuccessfully() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    RecordingEventListener delegate = new RecordingEventListener();
    OkHttpClient configureClient = rawClient.newBuilder().eventListener(delegate).build();

    OkHttpClient client = InstrumentedOkHttpClient.build(registry, configureClient, null);
    Request request = new Request.Builder().url(baseUrl).build();
    Response response = client.newCall(request).execute();
    response.close();

    List<String> expectedEvents =
        Arrays.asList(
            "CallStart",
            "DnsStart",
            "DnsEnd",
            "ConnectStart",
            "ConnectEnd",
            "ConnectionAcquired",
            "RequestHeadersStart",
            "RequestHeadersEnd",
            "ResponseHeadersStart",
            "ResponseHeadersEnd",
            "ResponseBodyStart",
            "ResponseBodyEnd",
            "ConnectionReleased",
            "CallEnd");
    Assertions.assertThat(delegate.recordedEventTypes()).isEqualTo(expectedEvents);
  }

  @Test
  public void executorServiceIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    // Force the requests to execute on this unit tests thread.
    rawClient =
        rawClient
            .newBuilder()
            .dispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()))
            .build();
    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, null);

    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-submitted"))
                .getCount())
        .isEqualTo(0);
    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-completed"))
                .getCount())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    client.newCall(req1).enqueue(new TestCallback());
    client.newCall(req2).enqueue(new TestCallback());

    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-submitted"))
                .getCount())
        .isEqualTo(2);
    assertThat(
            registry
                .getMeters()
                .get(InstrumentedOkHttpClient.metricId(null, "network-requests-completed"))
                .getCount())
        .isEqualTo(2);
  }

  @Test
  public void providedNameUsedInMetricId() {
    String prefix = "custom";
    String baseId = "network-requests-submitted";

    assertThat(registry.getMeters()).isEmpty();

    OkHttpClient client = InstrumentedOkHttpClient.build(registry, rawClient, prefix);
    String generatedId = InstrumentedOkHttpClient.metricId(prefix, baseId);

    assertThat(registry.getMeters().get(generatedId)).isNotNull();
  }

  /**
   * @param delta the offset from the current date to use. Negative values yield dates in the past;
   *     positive values yield dates in the future.
   */
  private String formatDate(long delta, TimeUnit timeUnit) {
    return formatDate(new Date(System.currentTimeMillis() + timeUnit.toMillis(delta)));
  }

  private String formatDate(Date date) {
    DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    rfc1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    return rfc1123.format(date);
  }

  private static final class TestCallback implements Callback {
    @Override
    public void onFailure(Call call, IOException e) {}

    @Override
    public void onResponse(Call call, Response response) throws IOException {
      response.body().close();
    }
  }
}
