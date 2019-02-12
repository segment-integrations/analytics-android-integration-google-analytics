package com.segment.analytics.android.integrations.google.analytics;

import android.app.Activity;
import android.app.Application;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.ecommerce.ProductAction;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.AnalyticsContext.Campaign;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class GoogleAnalyticsTest {

  GoogleAnalyticsIntegration integration;
  @Mock GoogleAnalytics googleAnalytics;
  @Mock Tracker tracker;
  @Mock Analytics analytics;
  @Mock Application application;

  static final String TRACKING_ID = "foo";

  @Before
  public void setUp() {
    initMocks(this);

    when(analytics.getApplication()).thenReturn(application);
    when(googleAnalytics.newTracker(TRACKING_ID)).thenReturn(tracker);

    integration = new GoogleAnalyticsIntegration(application, googleAnalytics,
        new ValueMap().putValue("mobileTrackingId", TRACKING_ID), Logger.with(VERBOSE));
  }

  private static AnalyticsContext contextWithCampaign(Campaign campaign) throws Exception {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("traits", new Traits());
    Constructor<AnalyticsContext> constructor =
        AnalyticsContext.class.getDeclaredConstructor(Map.class);
    constructor.setAccessible(true);
    AnalyticsContext analyticsContext = constructor.newInstance(map);
    return analyticsContext.putCampaign(campaign);
  }

  @Test public void initialize() throws IllegalStateException {
    ValueMap customDimensions = new ValueMap().putValue("tag", "dimension0");
    ValueMap customMetrics = new ValueMap().putValue("lag", "metric0");

    integration = new GoogleAnalyticsIntegration(application, googleAnalytics,
            new ValueMap().putValue("mobileTrackingId", TRACKING_ID)
                    .putValue("anonymizeIp", true)
                    .putValue("reportUncaughtExceptions", true)
                    .putValue("sendUserId", true)
                    .putValue("dimensions", customDimensions)
                    .putValue("metrics", customMetrics), Logger.with(VERBOSE));

    verify(googleAnalytics, atLeastOnce()).newTracker(TRACKING_ID);
    assertEquals(tracker, integration.tracker);

    verify(tracker).setAnonymizeIp(true);
    verify(tracker).setUncaughtExceptionReporter(application);

    assertTrue(integration.sendUserId);
    assertEquals(customDimensions, integration.customDimensions);
    assertEquals(customMetrics, integration.customMetrics);

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
    IdentifyPayload payload = (new IdentifyPayload.Builder()).userId("foo").traits(traits).build();
    integration.identify(payload);

    // If there are no custom dimensions/metrics and `sendUserId` is false,
    // nothing should happen.
    verify(tracker, never()).set(anyString(), anyString());
  }

  @Test public void identifyWithUserIdAndWithoutCustomDimensionsAndMetrics() {
    integration.sendUserId = true;

    Traits traits = createTraits("foo").putAge(20);
    integration.identify((new IdentifyPayload.Builder()).userId("foo").traits(traits).build());

    // If there are no custom dimensions/metrics and `sendUserId` is true,
    // only the userId should be set.
    verify(tracker).set("&uid", "foo");
  }

  @Test public void identifyWithUserIdAndCustomDimensionsAndMetrics() {
    integration.sendUserId = true;
    integration.customDimensions = new ValueMap().putValue("name", "dimension10");
    integration.customMetrics = new ValueMap().putValue("level", "metric12");

    Traits traits = createTraits("foo").putAge(20).putName("Chris").putValue("level", 13);
    integration.identify((new IdentifyPayload.Builder()).userId("foo").traits(traits).build());

    // Verify user id is set.
    verify(tracker).set("&uid", "foo");

    // Verify dimensions and metrics are set.
    verify(tracker).set("&cd10", "Chris");
    verify(tracker).set("&cm12", "13");
  }

  @Test public void track() {
    integration.track((new TrackPayload.Builder()).anonymousId("1234").event("foo").build());
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .build());
  }

  @Test public void trackWithProperties() {
    Properties properties =
        new Properties().putValue(51).putValue("label", "bar").putCategory("baz");

    integration.track((new TrackPayload.Builder()).anonymousId("1234").properties(properties).event("foo").build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("baz")
        .setAction("foo")
        .setLabel("bar")
        .setValue(51)
        .build());
  }

  @Test public void trackWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension3");

    integration.track((new TrackPayload.Builder()).anonymousId("1234").event("foo")
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

    integration.track((new TrackPayload.Builder()).anonymousId("1234").event("foo")
        .properties(new Properties().putValue("score", 50))
        .build());

    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("foo")
        .setLabel(null)
        .setValue(0)
        .setCustomMetric(5, 50)
        .build());
  }

  @Test public void trackWithAllCampaignData() throws Exception{

    Campaign campaign = new Campaign() //
        .putContent("newsletter") //
        .putMedium("online")
        .putName("coupons")
        .putSource("email");

    TrackPayload payload = (new TrackPayload.Builder()).anonymousId("1234")  //
        .event("bar") //
        .context(contextWithCampaign(campaign)) //
        .build();

    integration.track(payload);
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("bar")
        .setLabel(null)
        .setValue(0)
        .setCampaignParamsFromUrl(
            "utm_content=newsletter&utm_source=email&utm_medium=online&utm_campaign=coupons")
        .build());
  }

  @Test public void trackWithNullCampaignData() throws Exception{

    TrackPayload payload = (new TrackPayload.Builder()).anonymousId("1234")  //
        .event("bar") //
        .build();

    integration.track(payload);
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("bar")
        .setLabel(null)
        .setValue(0)
        .build());
  }

  @Test public void trackWithSomeCampaignData() throws Exception{

    Campaign campaign = new Campaign() //
        .putContent("email") //
        .putMedium("online");

    TrackPayload payload = (new TrackPayload.Builder()).anonymousId("1234")  //
        .event("bar") //
        .context(contextWithCampaign(campaign)) //
        .build();

    integration.track(payload);
    verify(tracker).send(new HitBuilders.EventBuilder().setCategory("All")
        .setAction("bar")
        .setLabel(null)
        .setValue(0)
        .setCampaignParamsFromUrl(
            "utm_content=email&utm_source=null&utm_medium=online&utm_campaign=null")
        .build());
  }

  @Test public void screen() {
    integration.screen((new ScreenPayload.Builder()).anonymousId("1234").name("foo").build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(anyMapOf(String.class, String.class));
  }

  @Test public void screenWithCustomDimensions() {
    integration.customDimensions = new ValueMap().putValue("custom", "dimension10");

    integration.screen((new ScreenPayload.Builder()).anonymousId("1234").name("foo")
        .properties(new Properties().putValue("custom", "value"))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder() //
        .setCustomDimension(10, "value").build());
  }

  @Test public void screenWithCustomMetrics() {
    integration.customMetrics = new ValueMap().putValue("count", "metric14");

    integration.screen((new ScreenPayload.Builder()).anonymousId("1234").name("foo")
        .properties(new Properties().putValue("count", 100))
        .build());

    InOrder inOrder = inOrder(tracker);
    inOrder.verify(tracker).setScreenName("foo");
    inOrder.verify(tracker).send(new HitBuilders.AppViewBuilder().setCustomMetric(14, 100).build());
  }

  @Test public void screenWithAllCampaignData() throws Exception {
    Campaign campaign = new Campaign() //
        .putContent("textlink") //
        .putSource("google") //
        .putMedium("cpc")
        .putName("spring_sale");
    ScreenPayload payload = (new ScreenPayload.Builder()).anonymousId("1234")  //
        .name("foo") //
        .context(contextWithCampaign(campaign)) //
        .build();

    integration.screen(payload);

    verify(tracker).setScreenName("foo");
    verify(tracker).send(new HitBuilders.ScreenViewBuilder() //
        .setCampaignParamsFromUrl(
            "utm_content=textlink&utm_source=google&utm_medium=cpc&utm_campaign=spring_sale") //
        .build());
  }

  @Test public void screenWithSomeCampaignData() throws Exception {
    Campaign campaign = new Campaign() //
        .putContent("textlink") //
        .putMedium("cpc");
    ScreenPayload payload = (new ScreenPayload.Builder()).anonymousId("1234")  //
        .name("hey") //
        .context(contextWithCampaign(campaign)) //
        .build();

    integration.screen(payload);

    verify(tracker).setScreenName("hey");
    verify(tracker).send(new HitBuilders.ScreenViewBuilder() //
        .setCampaignParamsFromUrl(
            "utm_content=textlink&utm_source=null&utm_medium=cpc&utm_campaign=null") //
        .build());
  }

  @Test public void screenWithNullCampaignData() throws Exception {
    ScreenPayload payload = (new ScreenPayload.Builder()).anonymousId("1234")  //
        .name("hey") //
        .build();

    integration.screen(payload);

    verify(tracker).setScreenName("hey");
    verify(tracker).send(new HitBuilders.ScreenViewBuilder().build());
  }

  @Test public void flush() {
    integration.flush();

    verify(googleAnalytics).dispatchLocalHits();
  }

  @Test public void sendProductEvent() {
    Properties properties = new Properties().putOrderId("foo")
        .putProductId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putCategory("purchase")
        .putValue("quantity", 10);

    integration.sendProductEvent("Viewed Product", "sports", properties);

    com.google.android.gms.analytics.ecommerce.Product product =
            new com.google.android.gms.analytics.ecommerce.Product()
                .setId("foo")
                .setName("baz")
                .setCategory("sports")
                .setPrice(20)
                .setQuantity(10);

    ProductAction productAction = new ProductAction(ProductAction.ACTION_DETAIL);

    verify(tracker).send(new HitBuilders.EventBuilder()
                .addProduct(product)
                .setProductAction(productAction)
                .setCategory("purchase")
                .setAction("Product Viewed")
                .build());
  }

  @Test public void sendProductEventWithUpdatedFormat() {
    Properties properties = new Properties().putOrderId("foo")
            .putProductId("foo")
            .putCurrency("bar")
            .putName("baz")
            .putSku("qaz")
            .putPrice(20)
            .putValue("quantity", 10);

    integration.sendProductEvent("Product Viewed", "sports", properties);

    com.google.android.gms.analytics.ecommerce.Product product =
            new com.google.android.gms.analytics.ecommerce.Product()
                    .setId("foo")
                    .setName("baz")
                    .setCategory("sports")
                    .setPrice(20)
                    .setQuantity(10);

    ProductAction productAction = new ProductAction(ProductAction.ACTION_DETAIL);

    verify(tracker).send(new HitBuilders.EventBuilder()
            .addProduct(product)
            .setProductAction(productAction)
            .setCategory("EnhancedEcommerce")
            .setAction("Product Viewed")
            .build());
  }

  @Test public void sendProductEventWithCustomDimensionsAndMetrics() {
    integration.customDimensions = new ValueMap().putValue("customDimension", "dimension2");
    integration.customMetrics = new ValueMap().putValue("customMetric", "metric3");

    Properties properties = new Properties().putOrderId("foo")
        .putProductId("foo")
        .putCurrency("bar")
        .putName("baz")
        .putSku("qaz")
        .putPrice(20)
        .putValue("quantity", 10)
        .putValue("customMetric", 32)
        .putValue("customDimension", "barbaz");
    integration.sendProductEvent("Removed Product", "sports", properties);

    com.google.android.gms.analytics.ecommerce.Product product =
            new com.google.android.gms.analytics.ecommerce.Product()
                .setId("foo")
                .setName("baz")
                .setCategory("sports")
                .setPrice(20)
                .setQuantity(10);

    ProductAction productAction = new ProductAction(ProductAction.ACTION_REMOVE);

    verify(tracker).send(new HitBuilders.EventBuilder()
            .addProduct(product)
            .setProductAction(productAction)
            .setAction("Product Removed")
            .setCategory("EnhancedEcommerce")
            .setCustomMetric(3, 32)
            .setCustomDimension(2, "barbaz")
            .build());
  }

  @Test public void completedOrderEventsAreDetectedCorrectly() {
    Pattern pattern = GoogleAnalyticsIntegration.COMPLETED_ORDER_PATTERN;

    String[] shouldMatch = new String[]{
            "Completed Order",
            "completed Order",
            "Completed order",
            "completed order",
            "completed           order",
            "Order Completed",
            "order Completed",
            "Order completed",
            "order completed",
            "order           completed"
    };

    String [] shouldNotMatch = new String[] {
            "completed",
            "order",
            "completed orde",
            "",
            "ompleted order"
    };

    assertPatternCases(pattern, shouldMatch, shouldNotMatch);
  }

  @Test public void productEventsAreAreDetectedCorrectly() {

    Pattern pattern = GoogleAnalyticsIntegration.PRODUCT_EVENT_NAME_PATTERN;

    String[] shouldMatch = new String[]{
            "Viewed Product Category",
            "VIEweD prODUct",
            "adDed Product",
            "Removed Product",
            "Viewed      Product"
    };

    String [] shouldNotMatch = new String[] {
            "removed",
            "Viewed",
            "adDed"
    };

    assertPatternCases(pattern, shouldMatch, shouldNotMatch);

  }

  private static void assertPatternCases(Pattern pattern, String[] shouldMatch, String[] shouldNotMatch) {
    for (String text : shouldMatch) {
      String msg = String.format("Expected <%s> to match pattern <%s> but did not.", text, pattern.pattern());
      assertTrue(msg, pattern.matcher(text).matches());
    }

    for (String text : shouldNotMatch) {
      String msg = String.format("Expected <%s> to not match patter <%s> but did.", text, pattern.pattern());
      assertFalse(msg, pattern.matcher(text).matches());
    }
  }

}