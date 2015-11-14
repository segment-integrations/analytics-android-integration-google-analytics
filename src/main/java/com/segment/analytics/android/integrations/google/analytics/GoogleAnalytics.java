package com.segment.analytics.android.integrations.google.analytics;

import android.app.Activity;

interface GoogleAnalytics {
  Tracker newTracker(String trackingId);

  void reportActivityStop(Activity activity);

  void reportActivityStart(Activity activity);

  void dispatchLocalHits();
}
