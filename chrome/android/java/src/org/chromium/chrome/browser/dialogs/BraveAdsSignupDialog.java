package org.chromium.chrome.browser.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;
import android.view.View;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveAdsNativeHelper;
import org.chromium.chrome.browser.BraveRewardsNativeWorker;
import org.chromium.chrome.browser.BraveRewardsPanelPopup;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.util.PackageUtils;

public class BraveAdsSignupDialog {

    private static String SHOULD_SHOW_DIALOG_COUNTER = "should_show_dialog_counter";

    public static boolean shouldShowNewUserDialog(Context context) {
        // TODO: Second condition which checks locality seems to have a different answer later
        boolean shouldShow =
          PackageUtils.isFirstInstall(context)
          && shouldViewCountDisplay()
          && !BraveAdsNativeHelper.nativeIsBraveAdsEnabled(Profile.getLastUsedProfile())
          && BraveAdsNativeHelper.nativeIsLocaleValid(Profile.getLastUsedProfile());
        return shouldShow;
    }

    public static boolean shouldShowExistingUserDialog(Context context) {
        // TODO: Second condition which checks locality seems to have a different answer later
        boolean shouldShow =
          !PackageUtils.isFirstInstall(context)
          && shouldViewCountDisplay()
          && (!BraveAdsNativeHelper.nativeIsBraveAdsEnabled(Profile.getLastUsedProfile()) || !wasBraveRewardsExplicitlyTurnedOn())
          && BraveAdsNativeHelper.nativeIsLocaleValid(Profile.getLastUsedProfile());
        return shouldShow;
    }

    public static void showNewUserDialog(Context context) {
        updateViewCount();
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.BraveDialogTheme)
        .setView(R.layout.brave_ads_new_user_dialog_layout)
        .setPositiveButton(R.string.brave_ads_new_user_offer_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Enable rewards
                // TODO: Make sure this is a synchronized process and doesn't create 2 wallets for the user
                BraveRewardsNativeWorker braveRewardsNativeWorker = BraveRewardsNativeWorker.getInstance();
                braveRewardsNativeWorker.CreateWallet();

                // Enable ads
                BraveAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
            }
        }).create();
        alertDialog.show();

        ImageView closeButton = alertDialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
    }

    public static void showExistingUserDialog(Context context) {
        updateViewCount();
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.BraveDialogTheme)
        .setView(R.layout.brave_ads_existing_user_dialog_layout)
        .setPositiveButton(R.string.brave_ads_existing_user_offer_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Enable ads
                BraveAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
            }
        }).create();
        alertDialog.show();

        ImageView closeButton = alertDialog.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
    }

    private static boolean shouldViewCountDisplay() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        int viewCount = sharedPref.getInt(SHOULD_SHOW_DIALOG_COUNTER, 0);
        return 0 == viewCount || 20 == viewCount || 40 == viewCount;
    }

    private static void updateViewCount() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SHOULD_SHOW_DIALOG_COUNTER, sharedPref.getInt(SHOULD_SHOW_DIALOG_COUNTER, 0) + 1);
        editor.apply();
    }

    private static boolean wasBraveRewardsExplicitlyTurnedOn() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        return sharedPref.contains(BraveRewardsPanelPopup.PREF_WAS_BRAVE_REWARDS_TURNED_ON);
    }
}
