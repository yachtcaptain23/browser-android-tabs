package org.chromium.chrome.browser.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import org.chromium.chrome.R;

public class BraveAdsSignupDialog {

  public static boolean shouldShowDialog() {
    return true;
  }

  public static void showDialog(Context context) {
//      ContextUtils.getAppSharedPreferences().edit().putBoolean(TemplateUrlService.PREF_DDG_OFFER_SHOWN, true).apply();
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
