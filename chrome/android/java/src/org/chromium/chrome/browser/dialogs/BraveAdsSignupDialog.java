package org.chromium.chrome.browser.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveAdsNativeHelper;
import org.chromium.chrome.browser.BraveRewardsPanelPopup;
import org.chromium.chrome.browser.profiles.Profile;

public class BraveAdsSignupDialog {

  public static boolean shouldShowDialog() {
      return BraveRewardsPanelPopup.isBraveRewardsEnabled()
        && !BraveAdsNativeHelper.nativeIsBraveAdsEnabled(Profile.getLastUsedProfile())
        && BraveAdsNativeHelper.nativeIsLocaleValid(Profile.getLastUsedProfile());
  }

  public static void showDialog(Context context) {
      new AlertDialog.Builder(context, R.style.BraveDialogTheme)
      .setView(R.layout.brave_ads_join_dialog_layout)
      .setPositiveButton(R.string.brave_ads_offer_positive, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
          }
      })
      .show();
  }
}
