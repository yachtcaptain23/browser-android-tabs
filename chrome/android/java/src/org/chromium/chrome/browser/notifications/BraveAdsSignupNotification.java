package org.chromium.chrome.browser.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeTabbedActivity;
import org.chromium.chrome.browser.dialogs.BraveAdsSignupDialog;
import org.chromium.chrome.R;

public class BraveAdsSignupNotification extends BroadcastReceiver {

    public static final int NOTIFICATION_ID = 11;
    public static void showNotification() {
        Context context = ContextUtils.getApplicationContext();
        NotificationCompat.Builder b = new NotificationCompat.Builder(context, ChromeActivity.CHANNEL_ID);

        Intent intent = new Intent(context, ChromeTabbedActivity.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(R.drawable.bat_icon)
         .setContentTitle(context.getString(R.string.brave_ads_offer_title))
         .setContentText(context.getString(R.string.brave_ads_offer_text))
         .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.brave_ads_offer_text)))
         .setPriority(NotificationCompat.PRIORITY_DEFAULT)
         .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, b.build());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent activityIntent = new Intent();
        activityIntent.setPackage(context.getPackageName());
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);
        BraveAdsSignupDialog.showDialog(context);
    }
}
