analytics-android-integration-google-analytics
==============================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/google-analytics/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/google-analytics)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/google-analytics.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/google-analytics)

**WARNING**: This SDK has been deprecated. [Google is sunsetting their Google Analytics mobile SDKs on October 31st.](https://support.google.com/firebase/answer/9167112?hl=en). Please [see our migration tutorial](https://segment.com/docs/destinations/google-analytics/#migrating-deprecated-google-analytics-mobile-sdks-to-firebase) to learn more about migrating to our Firebase SDKs for Android. 

Google Analytics integration for [analytics-android](https://github.com/segmentio/analytics-android).

## Installation

To install the Segment-Google Analytics integration, simply add this line to your gradle file:

```
compile 'com.segment.analytics.android.integrations:google-analytics:+'
```

## Usage

After adding the dependency, you must register the integration with our SDK.  To do this, import the Google Analytics integration:


```
import com.segment.analytics.android.integrations.google.analytics.GoogleAnalyticsIntegration;

```

And add the following line:

```
analytics = new Analytics.Builder(this, "write_key")
                .use(GoogleAnalyticsIntegration.FACTORY)
                .build();
```

Please see [our documentation](https://segment.com/docs/integrations/google-analytics/#mobile-apps) for more information.

## License

```
WWWWWW||WWWWWW
 W W W||W W W
      ||
    ( OO )__________
     /  |           \
    /o o|    MIT     \
    \___/||_||__||_|| *
         || ||  || ||
        _||_|| _||_||
       (__|__|(__|__|

The MIT License (MIT)

Copyright (c) 2014 Segment, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
