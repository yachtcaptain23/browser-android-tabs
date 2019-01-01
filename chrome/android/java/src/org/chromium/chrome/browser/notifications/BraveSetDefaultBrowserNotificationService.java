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
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.chromium.chrome.R;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeActivitySessionTracker;

public class BraveSetDefaultBrowserNotificationService extends BroadcastReceiver {
    public Context mContext;

//    public int FIFTEEN_MINUTES = 900_000;
    public int FIFTEEN_MINUTES = 300_000;

    private boolean isBraveSetAsDefaultBrowser() {
        Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
        boolean supportsDefault = Build.VERSION.SDK_INT >= 24;
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(browserIntent, supportsDefault ? PackageManager.MATCH_DEFAULT_ONLY : 0);
        return resolveInfo.activityInfo.packageName.equals("com.brave.browser_default");
    }

    private boolean shouldShowNotification() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        if(isBraveSetAsDefaultBrowser() || sharedPref.getBoolean(ChromeActivity.BRAVE_SET_DEFAULT_BROWSER_HAS_ASKED_KEY, false)) {
            // Don't bother showing since we've asked already
            return false;
        }
        return hasRanForPresetDuration();
    }

    private boolean hasRanForPresetDuration() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        return sharedPref.getLong(ChromeActivitySessionTracker.TOTAL_LIFETIME_USAGE, 0) + SystemClock.uptimeMillis() - sharedPref.getLong(ChromeActivitySessionTracker.SESSION_START_TIME, SystemClock.uptimeMillis()) >= FIFTEEN_MINUTES;
    }

    private void showNotification() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ChromeActivity.BRAVE_SET_DEFAULT_BROWSER_HAS_ASKED_KEY, true);
        editor.apply();
        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, ChromeActivity.CHANNEL_ID);
        // TODO: Change this to brave asset icon
        b.setSmallIcon(R.drawable.ic_videocam_white_24dp)
         .setAutoCancel(false)
         .setContentTitle("Setting Brave Browser as Default Browser")
         .setContentText("Enjoying Brave Browser's speed? You may set Brave as the default browserby clicking on the triple dot settings.")      
         .setPriority(NotificationCompat.PRIORITY_DEFAULT)
         .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, b.build());
    }

    private boolean shouldNotifyLater() {
      return !(isBraveSetAsDefaultBrowser());
    }

    private void notifyLater() {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, BraveSetDefaultBrowserNotificationService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        am.set( AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + FIFTEEN_MINUTES, pendingIntent );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (shouldShowNotification()) {
            showNotification();
        } else if (shouldNotifyLater()) {
            notifyLater();
        }
    }
}
