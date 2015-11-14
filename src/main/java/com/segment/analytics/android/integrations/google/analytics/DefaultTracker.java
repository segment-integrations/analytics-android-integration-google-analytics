package com.segment.analytics.android.integrations.google.analytics;

import android.content.Context;
import com.google.android.gms.analytics.ExceptionReporter;
import java.util.Map;

class DefaultTracker implements Tracker {
  final com.google.android.gms.analytics.Tracker delegate;

  DefaultTracker(com.google.android.gms.analytics.Tracker delegate) {
    this.delegate = delegate;
  }

  @Override public void send(Map<String, String> params) {
    delegate.send(params);
  }

  @Override public void setScreenName(String name) {
    delegate.setScreenName(name);
  }

  @Override public void set(String key, String value) {
    delegate.set(key, value);
  }

  @Override public void setAnonymizeIp(boolean anonymizeIp) {
    delegate.setAnonymizeIp(anonymizeIp);
  }

  @Override public void setUncaughtExceptionReporter(Context context) {
    Thread.UncaughtExceptionHandler myHandler =
        new ExceptionReporter(delegate, Thread.getDefaultUncaughtExceptionHandler(), context);
    Thread.setDefaultUncaughtExceptionHandler(myHandler);
  }

  @Override public com.google.android.gms.analytics.Tracker delegate() {
    return delegate;
  }
}
