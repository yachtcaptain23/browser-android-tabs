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

public class BraveAdsNotificationService extends BroadcastReceiver {
    public Context mContext;
    private Intent mIntent;

    // (Albert Wang): One higher than BraveSetDefaultBrowserNotificationService.
    // I didn't check to see if this was used by another service
    public static final int NOTIFICATION_ID = 11;

    // Deep links
    public static final String DEEP_LINK = "deep_link";
    public static final String DEEP_LINK_TYPE_PAGE = "deep_link_type_page"; // For opening up pages in Brave or redirecting to Google Play

    // Intent types
    public static final String INTENT_TYPE = "intent_type";
    public static final String DISMISS_INTENT = "dismiss_intent";

    // Payload keys for displaying the information
    public static final String NOTIFICATION_TITLE = "notification_title";
    public static final String NOTIFICATION_BODY = "notification_body";

    // E.g. values: 
    // ["https://play.google.com/store/apps/details?id=com.brave.browser",
    //  "market://details?id=com.brave.browser"]
    public static final String NOTIFICATION_URL = "notification_url";
    public static final String NOTIFICATION_NATIVE_UUID = "notification_native_uuid";

    private void showNotification(Intent intent) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, "com.brave.browser.ads");
        int smallIconId =  (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ? R.drawable.brave_logo_19 : R.drawable.ic_chrome;
        b.setDefaults(Notification.DEFAULT_ALL)
         .setSmallIcon(R.drawable.ic_chrome)
         .setContentTitle((String) intent.getStringExtra(NOTIFICATION_TITLE))
         .setContentText((String) intent.getStringExtra(NOTIFICATION_BODY))
         .setCategory(Notification.CATEGORY_MESSAGE)
         .setPriority(Notification.PRIORITY_MAX);

        b.setContentIntent(getAdsDeepLinkIntent(mContext, intent));
        b.setDeleteIntent(getAdsDismissIntent(mContext, intent));

        if (Build.VERSION.SDK_INT >= 21) b.setVibrate(new long[0]);

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, b.build());

        // TODO: (Albert Wang): Make sure there's permissions to show notification
        BraveAds.nativeOnShowHelper(Profile.getLastUsedProfile(), intent.getStringExtra(NOTIFICATION_NATIVE_UUID));
    }

    private PendingIntent getAdsDeepLinkIntent(Context context, Intent intent) {
        Intent deepLinkIntent = new Intent(context, BraveAdsNotificationService.class);
        deepLinkIntent.putExtra(INTENT_TYPE, DEEP_LINK);
        deepLinkIntent.putExtra(DEEP_LINK, DEEP_LINK_TYPE_PAGE);
        deepLinkIntent.putExtra(NOTIFICATION_URL, intent.getStringExtra(NOTIFICATION_URL));
        deepLinkIntent.putExtra(NOTIFICATION_NATIVE_UUID, intent.getStringExtra(NOTIFICATION_NATIVE_UUID));
        return PendingIntent.getBroadcast(context, 0, deepLinkIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void handleAdsDeepLink(Intent intent) {
        if (intent.getStringExtra(BraveAdsNotificationService.DEEP_LINK).equals(BraveAdsNotificationService.DEEP_LINK_TYPE_PAGE)) {
            String url = intent.getStringExtra(NOTIFICATION_URL);
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
            Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            deepLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            BraveAds.nativeOnClickHelper(Profile.getLastUsedProfile(), url, true);
            mContext.startActivity(deepLinkIntent);
        }
     }

    private PendingIntent getAdsDismissIntent(Context context, Intent intent) {
        Intent deepLinkIntent = new Intent(context, BraveAdsNotificationService.class);
        deepLinkIntent.putExtra(INTENT_TYPE, DISMISS_INTENT);
        deepLinkIntent.putExtra(NOTIFICATION_URL, intent.getStringExtra(NOTIFICATION_URL));
        deepLinkIntent.putExtra(NOTIFICATION_NATIVE_UUID, intent.getStringExtra(NOTIFICATION_NATIVE_UUID));
        return PendingIntent.getBroadcast(context, 0, deepLinkIntent, 0);
    }
    
    // (Albert Wang): Need to figure out if there's a way to distinguish dismissed by user vs system.
    private void handleDismiss(Intent intent) {
        BraveAds.nativeOnDismissHelper(
            Profile.getLastUsedProfile(), 
            intent.getStringExtra(NOTIFICATION_URL),
            intent.getStringExtra(NOTIFICATION_NATIVE_UUID),
            true
        );
     }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("albert", "got an intent" + intent.toString());
        mContext = context;
        mIntent = intent;
        if (intent != null && intent.hasExtra(INTENT_TYPE)) {
            if (intent.getStringExtra(INTENT_TYPE).equals(DEEP_LINK))
                handleAdsDeepLink(intent);
            else if (intent.getStringExtra(INTENT_TYPE).equals(DISMISS_INTENT))
                handleDismiss(intent);
        } else {
            showNotification(intent);
        }
    }
}
