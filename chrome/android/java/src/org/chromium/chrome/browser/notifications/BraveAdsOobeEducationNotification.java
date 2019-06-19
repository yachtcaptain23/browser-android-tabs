package org.chromium.chrome.browser.notifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.notifications.BraveAdsNotificationBuilder;
import org.chromium.chrome.browser.notifications.ChromeNotification;
import org.chromium.chrome.browser.notifications.NotificationBuilderBase;
import org.chromium.chrome.browser.notifications.NotificationManagerProxyImpl;
import org.chromium.chrome.browser.notifications.NotificationMetadata;
import org.chromium.chrome.browser.notifications.NotificationUmaTracker;

public class BraveAdsOobeEducationNotification extends BroadcastReceiver {
    public Context mContext;
    private Intent mIntent;

    private static final int BRAVE_ADS_OOBE_NOTIFICATION_ID = -2;
    private static String BRAVE_ADS_OOBE_NOTIFICATION_TAG = "brave_ads_oobe_notification_tag";

    private void showOobeNotification(Context context) {
        NotificationManagerProxyImpl notificationManager = new NotificationManagerProxyImpl(context);
        NotificationBuilderBase notificationBuilder =
          new BraveAdsNotificationBuilder(context)
              .setTitle(context.getString(R.string.brave_ads_oobe_education_notification_title))
              .setBody(context.getString(R.string.brave_ads_oobe_education_notification_body))
              .setSmallIconId(R.drawable.ic_chrome)
              .setPriority(Notification.PRIORITY_HIGH)
              .setOrigin("https://brave.com/brave-rewards");

        ChromeNotification notification = notificationBuilder.build(new NotificationMetadata(
                NotificationUmaTracker.SystemNotificationType.SITES,
                BRAVE_ADS_OOBE_NOTIFICATION_TAG /* notificationTag */, BRAVE_ADS_OOBE_NOTIFICATION_ID /* notificationId */));
        notificationManager.notify(notification);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        showOobeNotification(context);
    }
}
