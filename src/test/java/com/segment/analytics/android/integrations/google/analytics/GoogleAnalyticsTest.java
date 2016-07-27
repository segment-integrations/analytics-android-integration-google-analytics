package com.segment.analytics.android.integrations.google.analytics;

import android.app.Activity;
import android.app.Application;
import com.google.android.gms.analytics.HitBuilders;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
// todo: These tests do not run in the IDE http://pastebin.com/YZZTcZa8
public class GoogleAnalyticsTest {
  GoogleAnalyticsIntegration integration;
  GoogleAnalytics googleAnalytics;
  Tracker tracker;
  @Mock Analytics analytics;
  @Mock Application application;

  @Before public void setUp() {
    initMocks(this);
    googleAnalytics = mock(GoogleAnalytics.class);
    tracker = mock(Tracker.class);

    when(analytics.getApplication()).thenReturn(application);
    when(googleAnalytics.newTracker("foo")).thenReturn(tracker);

    integration = new GoogleAnalyticsIntegration(application, googleAnalytics,
        new ValueMap().putValue("mobileTrackingId", "foo"), Logger.with(VERBOSE));
  }

  @Test public void initialize() throws IllegalStateException {
    // TODO
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verify(googleAnalytics).reportActivityStart(activity);
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verify(googleAnalytics).reportActivityStop(activity);
  }

  @Test public void identify() {
    Traits traits = createTraits("foo").putAge(20);
    IdentifyPayload payload = new IdentifyPayloadBuilder().traits(traits).build();
    integration.identify(payload);

    // If there are no custom dimensions/metrics and `sendUserId` is false,
    // nothing should happen.
    verify(tracker, never()).set(anyString(), anyString());
  }

  @Test public void identifyWithUserIdAndWithoutCustomDimensionsAndMetrics() {
    integration.sendUserId = true;

    Traits traits = createTraits("foo").putAge(20);
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    // If there are no custom dimensions/metrics and `sendUserId` is true,
    // only the userId should be set.
    verify(tracker).set("&uid", "foo");
  }

  @Test public void identifyWithUserIdAndCustomDimensionsAndMetrics() {
    integration.sendUserId = true;
    integration.customDimensions = new ValueMap().putValue("name", "dimension10");
    integration.customMetrics = new ValueMap().putValue("level", "metric12");

    Traits traits = createTraits("foo").putAge(20).putName("Chris").putValue("level", 13);
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    // Verify user id is set.
    verify(tracker).set("&uid", "foo");

    // Verify dimensions and metrics are set.
    verify(tracker).set("&cd10", "Chris");
    verify(tracker).set("&cm12", "13");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .build());
  }

  @Test public void trackWithProperties() {
    Properties properties =
        new Properties().putValue(51).putValue("label", "bar").putCategory("baz");

    integration.track(new TrackPayloadBuilder().properties(properties).event("foo").build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("baz")
        .setAction("foo")
        .setLabel("bar")
        .setValue(51)
        .build());
  }

  @Test public void trackWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension3");

    integration.track(new TrackPayloadBuilder().event("foo")
        .properties(new Properties().putValue("custom", "test"))
        .build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .setCustomDimension(3, "test")
        .build());
  }

  @Test public void trackWithCustomMetrics() {
    integration.customMetrics = new ValueMap().putValue("score", "metric5");

    integration.track(new TrackPayloadBuilder().event("foo")
        .properties(new Properties().putValue("score", 50))
        .build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .setCustomMetric(5, 50)
        .build());
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(anyMapOf(String.class, String.class));
  }

  @Test public void screenWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension10");

    integration.screen(new ScreenPayloadBuilder().name("foo")
        .properties(new Properties().putValue("custom", "value"))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder() //
        .setCustomDimension(10, "value").build());
  }

  @Test public void screenWithCustomMetrics() {
    integration.customMetrics = new ValueMap().putValue("count", "metric14");

    integration.screen(new ScreenPayloadBuilder().name("foo")
        .properties(new Properties().putValue("count", 100))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder().setCustomMetric(14, 100).build());
  }

  @Test public void flush() {
    integration.flush();

    verify(googleAnalytics).dispatchLocalHits();
  }

  @Test public void sendProductEvent() {
    Properties properties = new Properties().putOrderId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putValue("quantity", 10);

    integration.sendProductEvent("Viewed Product", "sports", properties);

    verify(tracker).send(new HitBuilders.ItemBuilder() //
        .setTransactionId("foo")
        .setCurrencyCode("bar")
        .setName("baz")
        .setSku("qaz")
        .setCategory("sports")
        .setPrice(20)
        .setQuantity(10)
        .build());
  }

  @Test public void sendProductEventWithCustomDimensionsAndMetrics() {
    integration.customDimensions = new ValueMap().putValue("customDimension", "dimension2");
    integration.customMetrics = new ValueMap().putValue("customMetric", "metric3");

    Properties properties = new Properties().putOrderId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putValue("quantity", 10)
        .putValue("customMetric", "32.22")
        .putValue("customDimension", "barbaz");
    integration.sendProductEvent("Removed Product", "sports", properties);

    verify(tracker).send(new HitBuilders.ItemBuilder() //
        .setTransactionId("foo")
        .setCurrencyCode("bar")
        .setName("baz")
        .setSku("qaz")
        .setCategory("sports")
        .setPrice(20)
        .setQuantity(10)
        .setCustomDimension(2, "barbaz")
        .setCustomMetric(3, 32.22f)
        .build());
  }

  @Test public void completedOrderEventsAreDetectedCorrectly() {
    assertThat(GoogleAnalyticsIntegration.COMPLETED_ORDER_PATTERN) //
        .matches("Completed Order")
        .matches("completed Order")
        .matches("Completed order")
        .matches("completed order")
        .matches("completed           order")
        .doesNotMatch("completed")
        .doesNotMatch("order")
        .doesNotMatch("completed orde")
        .doesNotMatch("")
        .doesNotMatch("ompleted order");
  }

  @Test public void productEventsAreAreDetectedCorrectly() {
    assertThat(GoogleAnalyticsIntegration.PRODUCT_EVENT_NAME_PATTERN) //
        .matches("Viewed Product Category")
        .matches("VIEweD prODUct")
        .matches("adDed Product")
        .matches("Removed Product")
        .matches("Viewed      Product")
        .doesNotMatch("removed")
        .doesNotMatch("Viewed")
        .doesNotMatch("Viewed")
        .doesNotMatch("adDed");
  }

  PatternAssert assertThat(Pattern pattern) {
    return new PatternAssert(pattern);
  }

  static class PatternAssert extends AbstractAssert<PatternAssert, Pattern> {
    public PatternAssert(Pattern actual) {
      super(actual, PatternAssert.class);
    }

    public PatternAssert matches(String text) {
      isNotNull();
      Assertions.assertThat(actual.matcher(text).matches())
          .overridingErrorMessage("Expected <%s> to match pattern <%s> but did not.", text,
              actual.pattern())
          .isTrue();
      return this;
    }

    public PatternAssert doesNotMatch(String text) {
      isNotNull();
      Assertions.assertThat(actual.matcher(text).matches())
          .overridingErrorMessage("Expected <%s> to not match patter <%s> but did.", text,
              actual.pattern())
          .isFalse();
      return this;
    }
  }
}