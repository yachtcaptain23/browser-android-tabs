package org.chromium.chrome.browser.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveAdsNativeHelper;
import org.chromium.chrome.browser.BraveRewardsNativeWorker;
import org.chromium.chrome.browser.BraveRewardsPanelPopup;
import org.chromium.chrome.browser.profiles.Profile;

public class BraveAdsSignupDialog {

    private static String SHOULD_SHOW_DIALOG_COUNTER = "should_show_dialog_counter";

    public static boolean shouldShowDialog() {
        boolean shouldShow = !BraveAdsNativeHelper.nativeIsBraveAdsEnabled(Profile.getLastUsedProfile())
          && BraveAdsNativeHelper.nativeIsLocaleValid(Profile.getLastUsedProfile())
          && shouldViewCountDisplay();
        updateViewCount();
        return shouldShow;
    }

    public static void showDialog(Context context) {
        new AlertDialog.Builder(context, R.style.BraveDialogTheme)
        .setView(R.layout.brave_ads_join_dialog_layout)
        .setPositiveButton(R.string.brave_ads_offer_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Enable rewards
                // TODO: Make sure this is a synchronized process and doesn't create 2 wallets for the user
                BraveRewardsNativeWorker braveRewardsNativeWorker = BraveRewardsNativeWorker.getInstance();
                braveRewardsNativeWorker.CreateWallet();

                // Enable ads
                BraveAdsNativeHelper.nativeSetAdsEnabled(Profile.getLastUsedProfile());
            }
        })
        .show();
    }

    private static boolean shouldViewCountDisplay() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        int viewCount = sharedPref.getInt(SHOULD_SHOW_DIALOG_COUNTER, 0);
        return 0 == viewCount || 1 == viewCount || 20 == viewCount || 40 == viewCount;
    }

    private static void updateViewCount() {
        SharedPreferences sharedPref = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SHOULD_SHOW_DIALOG_COUNTER, sharedPref.getInt(SHOULD_SHOW_DIALOG_COUNTER, 0) + 1);
        editor.apply();
    }
}
