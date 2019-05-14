package org.chromium.chrome.browser.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.chromium.chrome.R;
import org.chromium.base.annotations.CalledByNative;
import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeActivitySessionTracker;

@JNINamespace("brave_ads")
public class BraveAds {
    public Context mContext;

    private long mNativeBraveAds;

    private BraveAds() {
      mNativeBraveAds = 0;
    }

    private BraveAds(long staticBraveAds) {
        mNativeBraveAds = staticBraveAds;
    }

    @CalledByNative
    private static BraveAds create(long staticBraveAds) {
        return new BraveAds(staticBraveAds);
    }

    @CalledByNative
    public void showNotificationFromNative(String advertiser, String text, String url, String uuid) {
        if (mContext == null)
            mContext = ContextUtils.getApplicationContext();
        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, "com.brave.browser.ads");
        Context context = ContextUtils.getApplicationContext();
        Intent intent = new Intent(context, BraveAdsNotificationService.class);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_TITLE, advertiser);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_BODY, text);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_URL, url);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_NATIVE_UUID, uuid);
        context.sendBroadcast(intent);
    }
}
