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
import org.chromium.chrome.browser.profiles.Profile;

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
/**
    @CalledByNative
    public void showNotificationFromNative(String advertiser, String text, String url, String uuid) {
        Log.d("albert", "Got a notification from: " + advertiser);
        if (mContext == null)
            mContext = ContextUtils.getApplicationContext();
        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, "com.brave.browser.ads");
        Intent intent = new Intent(mContext, BraveAdsNotificationService.class);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_TITLE, advertiser);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_BODY, text);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_URL, url);
        intent.putExtra(BraveAdsNotificationService.NOTIFICATION_NATIVE_UUID, uuid);
        mContext.sendBroadcast(intent);
    }
*/

    @CalledByNative
    public void openPageFromNative(String url) {
        Log.d("chromium", "Opening page albert to url: " + url);
        if (mContext == null) mContext = ContextUtils.getApplicationContext();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /*
    public static native void nativeOnShowHelper(Profile profile, String uuid);
    public static native void nativeOnClickHelper(Profile profile, String url, boolean should_close);
    public static native void nativeOnDismissHelper(Profile profile, String url, String uuid, boolean dismissed_by_user);
    */
}
