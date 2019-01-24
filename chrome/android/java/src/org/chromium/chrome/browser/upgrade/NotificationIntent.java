/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.upgrade;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.chrome.browser.notifications.BraveSetDefaultBrowserNotificationService;
import org.chromium.chrome.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Fires notification about new features.
 */
public class NotificationIntent {
    private static final String TAG = "NotificationIntent";
    private static final String NOTIFICATION_TAG = "8f162e76-052d-449b-b3e8-2da3c891b7c2";
    private static final String NOTIFICATION_CHANNEL_ID = "7299a8d0-0f7a-4c29-9369-c22425d35341";
    private static final String UPDATE_EXTRA_PARAM = "org.chromium.chrome.browser.upgrade.UPDATE_NOTIFICATION";
    private static final String PREF_NAME = "org.chromium.chrome.browser.upgrade.NotificationUpdateTimeStampPreferences";
    private static final String MILLISECONDS_NAME = "org.chromium.chrome.browser.upgrade.Milliseconds";
    private static final String URL = "https://brave.com/new-brave-22-percent-faster/";
    //private static final List<String> mWhitelistedRegionalLocales = Arrays.asList("en", "ru", "uk", "be", "pt", "fr", "es");
    private static final int NOTIFICATION_ID = 632;
    private static final String NOTIFICATION_DISMISS_EXTRA = "org.chromium.chrome.browser.upgrade.NotificationUpgrade";

    public static void fireNotificationIfNecessary(Context context) {
        String notification_text = context.getString(R.string.update_notification_text);
        if (!ShouldNotify(context)) {
            return;
        }
        NotificationManager mNotificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Channel for notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);

        // Create the intent that’ll fire when the user taps the notification
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
        intent.putExtra(UPDATE_EXTRA_PARAM, true);
        intent.setPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);

        Intent dismissIntent = new Intent(context, NotificationUpgradeDismissedReceiver.class);
        dismissIntent.putExtra(NOTIFICATION_DISMISS_EXTRA, NOTIFICATION_ID);

        PendingIntent pendingDismissIntent =
               PendingIntent.getBroadcast(context.getApplicationContext(), 
                                          NOTIFICATION_ID, dismissIntent, 0);
        mBuilder.setDeleteIntent(pendingDismissIntent);

        mBuilder.setSmallIcon(R.drawable.ic_chrome);
        mBuilder.setContentTitle(context.getString(R.string.update_notification_title));
        mBuilder.setContentText(notification_text);

        mNotificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, mBuilder.build());
        fireBraveSetDefaultBrowserNotification(context);
    }

    private static void fireBraveSetDefaultBrowserNotification(Context context) {
        Intent intent = new Intent(context, BraveSetDefaultBrowserNotificationService.class);
        intent.putExtra(BraveSetDefaultBrowserNotificationService.INTENT_TYPE, BraveSetDefaultBrowserNotificationService.BROWSER_UPDATED);
        intent.setAction(BraveSetDefaultBrowserNotificationService.BROWSER_UPDATED);
        context.sendBroadcast(intent);
    }

    public static boolean ShouldNotify(Context context) {
        //String deviceLanguage = Locale.getDefault().getLanguage();
        if (GetPreferences(context) != 0/*
              || !mWhitelistedRegionalLocales.contains(new Locale(deviceLanguage).getLanguage())*/) {
            return false;
        }

        return true;
    }

    public static long GetPreferences(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, 0);

        return sharedPref.getLong(MILLISECONDS_NAME, 0);
    }
}
