package com.segment.analytics.android.integrations.google.analytics;

import android.app.Activity;

class DefaultGoogleAnalytics implements GoogleAnalytics {
  final com.google.android.gms.analytics.GoogleAnalytics delegate;

  public DefaultGoogleAnalytics(com.google.android.gms.analytics.GoogleAnalytics delegate) {
    this.delegate = delegate;
  }

  @Override public Tracker newTracker(String trackingId) {
    com.google.android.gms.analytics.Tracker delegateTracker = delegate.newTracker(trackingId);
    return new DefaultTracker(delegateTracker);
  }

  @Override public void reportActivityStop(Activity activity) {
    delegate.reportActivityStop(activity);
  }

  @Override public void reportActivityStart(Activity activity) {
    delegate.reportActivityStart(activity);
  }

  @Override public void dispatchLocalHits() {
    delegate.dispatchLocalHits();
  }
}
