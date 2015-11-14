package com.segment.analytics.android.integrations.google.analytics;

import android.content.Context;
import java.util.Map;

interface Tracker {
  void send(Map<String, String> params);

  void setScreenName(String name);

  void set(String key, String value);

  void setAnonymizeIp(boolean anonymizeIp);

  void setUncaughtExceptionReporter(Context context);

  com.google.android.gms.analytics.Tracker delegate();
}
