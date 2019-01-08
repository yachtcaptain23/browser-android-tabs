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
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.chromium.chrome.R;
import org.chromium.base.ContextUtils;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeActivitySessionTracker;

public class BraveSetDefaultBrowserNotificationService extends BroadcastReceiver {
    public Context mContext;
    private Intent mIntent;
    
    public static final String HAS_ASKED_AT_FIFTEEN_MINUTES = "brave_set_default_browser_has_asked_at_fifteen_minutes";
    public static final String HAS_SET_ALARMS_FOR_DAYS_LATER = "brave_set_default_browser_has_set_alarm";

    public static final int FIVE_MINUTES = 300_000;
    public static final int FIFTEEN_MINUTES = 900_000;
    public static final int FIVE_DAYS = 216_000_000;
    public static final int TEN_DAYS = 432_000_000;
    public static final int FIFTEEN_DAYS = 648_000_000;
    public static final int NOTIFICATION_ID = 10;

    // Intent types
    public static final String INTENT_TYPE = "intent_type";
    public static final String FIFTEEN_MINUTES_LATER = "fifteen_minutes_later";
    public static final String FIVE_DAYS_LATER = "five_days_later";
    public static final String TEN_DAYS_LATER = "ten_days_later";
    public static final String FIFTEEN_DAYS_LATER = "fifteen_days_later";

    private boolean isBraveSetAsDefaultBrowser() {
        Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse("http://"));
        boolean supportsDefault = Build.VERSION.SDK_INT >= 24;
        ResolveInfo resolveInfo = mContext.getPackageManager().resolveActivity(browserIntent, supportsDefault ? PackageManager.MATCH_DEFAULT_ONLY : 0);
        return resolveInfo.activityInfo.packageName.equals("com.brave.browser_default");
    }

    private boolean shouldShowNotification() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        if (isBraveSetAsDefaultBrowser() || mIntent.getStringExtra(INTENT_TYPE) == null)
            return false;

        if (mIntent.getStringExtra(INTENT_TYPE).equals(FIFTEEN_MINUTES_LATER))
            return sharedPref.getBoolean(HAS_ASKED_AT_FIFTEEN_MINUTES, false) ? false : hasRanForPresetDuration();

        if (mIntent.getStringExtra(INTENT_TYPE).equals(FIVE_DAYS_LATER) ||
            mIntent.getStringExtra(INTENT_TYPE).equals(TEN_DAYS_LATER) ||
            mIntent.getStringExtra(INTENT_TYPE).equals(FIFTEEN_DAYS_LATER)
          )
            return true;

        // Shouldn't reach here. Just out of safety, don't annoy users
        return false;
    }

    private boolean hasRanForPresetDuration() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        return sharedPref.getLong(ChromeActivitySessionTracker.TOTAL_LIFETIME_USAGE, 0) + SystemClock.uptimeMillis() - sharedPref.getLong(ChromeActivitySessionTracker.SESSION_START_TIME, SystemClock.uptimeMillis()) >= FIFTEEN_MINUTES;
    }

    private void showNotification() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(HAS_ASKED_AT_FIFTEEN_MINUTES, true);
        editor.apply();
        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, ChromeActivity.CHANNEL_ID);

        b.setSmallIcon(R.drawable.brave_notification_white)
         .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.brave_notification_white))
         .setAutoCancel(false)
         .setContentTitle("Setting Brave Browser as Default Browser")
         .setContentText("Enjoying Brave Browser's speed? You may set Brave as the default browserby clicking on the triple dot settings.")      
         .setPriority(NotificationCompat.PRIORITY_DEFAULT)
         .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, b.build());
    }

    private boolean shouldNotifyLater() {
      // Don't notify if it's default
      return !(isBraveSetAsDefaultBrowser());
    }

    private void notifyLater() {
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, BraveSetDefaultBrowserNotificationService.class);
        intent.putExtra(INTENT_TYPE, FIFTEEN_MINUTES_LATER);
        intent.setAction(FIFTEEN_MINUTES_LATER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        am.set( AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + FIVE_MINUTES, pendingIntent );
    }

    private boolean hasSetAlarmsForDaysLater() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        // Check to see if we already set alarms for 5, 10, and 30 days later
        return !sharedPref.getBoolean(HAS_SET_ALARMS_FOR_DAYS_LATER, false);
    }

    private void setAlarmsForDaysLater() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(HAS_SET_ALARMS_FOR_DAYS_LATER, true);
        editor.apply();

        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mContext, BraveSetDefaultBrowserNotificationService.class);
        intent.putExtra(INTENT_TYPE, FIVE_DAYS_LATER);
        intent.setAction(FIVE_DAYS_LATER);
        am.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + FIVE_DAYS,
            PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        );

        intent.putExtra(INTENT_TYPE, TEN_DAYS_LATER);
        intent.setAction(TEN_DAYS_LATER);
        am.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + TEN_DAYS,
            PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        );

        intent.putExtra(INTENT_TYPE, FIFTEEN_DAYS_LATER);
        intent.setAction(FIFTEEN_DAYS_LATER);
        am.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + FIFTEEN_DAYS,
            PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        if (shouldShowNotification()) {
            showNotification();
        } else if (shouldNotifyLater()) {
            notifyLater();
            if (hasSetAlarmsForDaysLater()) {
                setAlarmsForDaysLater();
            }
        }
    }
}
