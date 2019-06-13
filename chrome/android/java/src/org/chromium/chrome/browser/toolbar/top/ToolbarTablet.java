// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar.top;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.base.ApiCompatibilityUtils;
import org.chromium.base.ApplicationStatus;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.metrics.RecordUserAction;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveRewardsNativeWorker;
import org.chromium.chrome.browser.BraveRewardsObserver;
import org.chromium.chrome.browser.BraveRewardsPanelPopup;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.ChromeTabbedActivity;
import org.chromium.chrome.browser.NavigationPopup;
import org.chromium.chrome.browser.dialogs.BraveAdsSignupDialog;
import org.chromium.chrome.browser.download.DownloadUtils;
import org.chromium.chrome.browser.ntp.NewTabPage;
import org.chromium.chrome.browser.omnibox.LocationBar;
import org.chromium.chrome.browser.omnibox.LocationBarTablet;
import org.chromium.chrome.browser.partnercustomizations.HomepageManager;
import org.chromium.chrome.browser.preferences.ChromePreferenceManager;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.RestartWorker;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.toolbar.HomeButton;
import org.chromium.chrome.browser.toolbar.KeyboardNavigationListener;
import org.chromium.chrome.browser.toolbar.TabCountProvider;
import org.chromium.chrome.browser.toolbar.TabCountProvider.TabCountObserver;
import org.chromium.chrome.browser.util.AccessibilityUtil;
import org.chromium.chrome.browser.util.ColorUtils;
import org.chromium.chrome.browser.util.FeatureUtilities;
import org.chromium.ui.UiUtils;
import org.chromium.ui.base.DeviceFormFactor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Toolbar object for Tablet screens.
 */
@SuppressLint("Instantiatable")
public class ToolbarTablet extends ToolbarLayout
        implements OnClickListener, View.OnLongClickListener, TabCountObserver,
        BraveRewardsObserver {
    // The number of toolbar buttons that can be hidden at small widths (reload, back, forward).
    public static final int HIDEABLE_BUTTON_COUNT = 3;

    private static final String PREF_HIDE_BRAVE_ICON = "hide_brave_rewards_icon";

    private HomeButton mHomeButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mReloadButton;
    private ImageButton mBookmarkButton;
    private ImageButton mSaveOfflineButton;
    private ToggleTabStackButton mAccessibilitySwitcherButton;
    private ImageView mBraveShieldsButton;
    private ImageView mBraveRewardsPanelButton;

    private OnClickListener mBookmarkListener;
    private OnClickListener mBraveShieldsListener;

    private boolean mIsInTabSwitcherMode;

    private boolean mShowTabStack;
    private boolean mToolbarButtonsVisible;
    private ImageButton[] mToolbarButtons;

    private NavigationPopup mNavigationPopup;

    private Boolean mIsIncognito;
    private LocationBarTablet mLocationBar;

    private final int mStartPaddingWithButtons;
    private final int mStartPaddingWithoutButtons;
    private boolean mShouldAnimateButtonVisibilityChange;
    private AnimatorSet mButtonVisibilityAnimators;

    private NewTabPage mVisibleNtp;

    private TextView mBraveRewardsNotificationsCount;
    private BraveRewardsNativeWorker mBraveRewardsNativeWorker;
    private BraveRewardsPanelPopup mRewardsPopup;

    private FrameLayout mShieldsLayout;
    private boolean mShieldsLayoutIsColorBackground;
    private FrameLayout mRewardsLayout;

    /**
     * Constructs a ToolbarTablet object.
     * @param context The Context in which this View object is created.
     * @param attrs The AttributeSet that was specified with this View.
     */
    public ToolbarTablet(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStartPaddingWithButtons =
                getResources().getDimensionPixelOffset(R.dimen.tablet_toolbar_start_padding);
        mStartPaddingWithoutButtons =
                getResources().getDimensionPixelOffset(R.dimen.toolbar_edge_padding);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mLocationBar = (LocationBarTablet) findViewById(R.id.location_bar);

        mHomeButton = findViewById(R.id.home_button);
        mBackButton = findViewById(R.id.back_button);
        mForwardButton = findViewById(R.id.forward_button);
        mReloadButton = findViewById(R.id.refresh_button);
        // ImageView tinting doesn't work with LevelListDrawable, use Drawable tinting instead.
        // See https://crbug.com/891593 for details.
        Drawable reloadIcon = UiUtils.getTintedDrawable(
                getContext(), R.drawable.btn_reload_stop, R.color.standard_mode_tint);
        mReloadButton.setImageDrawable(reloadIcon);
        mShowTabStack = AccessibilityUtil.isAccessibilityEnabled()
                && isAccessibilityTabSwitcherPreferenceEnabled();

        mAccessibilitySwitcherButton = findViewById(R.id.tab_switcher_button);
        updateSwitcherButtonVisibility(mShowTabStack);

        mBookmarkButton = findViewById(R.id.bookmark_button);

        final View menuButtonWrapper = getMenuButtonWrapper();
        menuButtonWrapper.setVisibility(View.VISIBLE);

        if (mAccessibilitySwitcherButton.getVisibility() == View.GONE
                && menuButtonWrapper.getVisibility() == View.GONE) {
            ViewCompat.setPaddingRelative((View) menuButtonWrapper.getParent(), 0, 0,
                    getResources().getDimensionPixelSize(R.dimen.tablet_toolbar_end_padding), 0);
        }

        mSaveOfflineButton = findViewById(R.id.save_offline_button);

        // Initialize values needed for showing/hiding toolbar buttons when the activity size
        // changes.
        mShouldAnimateButtonVisibilityChange = false;
        mToolbarButtonsVisible = true;
        mBraveShieldsButton = (ImageView) findViewById(R.id.brave_shields_button);
        if (mBraveShieldsButton != null) {
            mBraveShieldsButton.setClickable(true);
        }
        mShieldsLayout = (FrameLayout) findViewById(R.id.brave_shields_button_layout);
        mBraveRewardsPanelButton = (ImageView) findViewById(R.id.brave_rewards_button);
        if (mBraveRewardsPanelButton != null) {
            mBraveRewardsPanelButton.setClickable(true);
        }
        mRewardsLayout = (FrameLayout) findViewById(R.id.brave_rewards_button_layout);
        mBraveRewardsNotificationsCount = (TextView) findViewById(R.id.br_notifications_count);
        mToolbarButtons = new ImageButton[] {mBackButton, mForwardButton, mReloadButton};
    }

    @Override
    void destroy() {
        super.destroy();
        if (mHomeButton != null) mHomeButton.destroy();
        if (mBraveRewardsNativeWorker != null) mBraveRewardsNativeWorker.RemoveObserver(this);
    }

    /**
     * Sets up key listeners after native initialization is complete, so that we can invoke
     * native functions.
     */
    @Override
    public void onNativeLibraryReady() {
        SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
        if (ChromeFeatureList.isEnabled(ChromeFeatureList.BRAVE_REWARDS) &&
            !PrefServiceBridge.getInstance().isSafetynetCheckFailed() &&
                !sharedPreferences.getBoolean(PREF_HIDE_BRAVE_ICON, false)) {
            if (mRewardsLayout != null && mShieldsLayout != null) {
                mShieldsLayout.setBackgroundColor(ColorUtils.getDefaultThemeColor(getResources(), isIncognito()));
                mShieldsLayoutIsColorBackground = true;
                mRewardsLayout.setVisibility(View.VISIBLE);
            }
        }

        if (mShieldsLayout != null) {
            mShieldsLayout.setVisibility(View.VISIBLE);
        }
        super.onNativeLibraryReady();
        mLocationBar.onNativeLibraryReady();
        mHomeButton.setOnClickListener(this);
        mHomeButton.setOnLongClickListener(this);
        mHomeButton.setOnKeyListener(new KeyboardNavigationListener() {
            @Override
            public View getNextFocusForward() {
                if (mBackButton.isFocusable()) {
                    return findViewById(R.id.back_button);
                } else if (mForwardButton.isFocusable()) {
                    return findViewById(R.id.forward_button);
                } else {
                    return findViewById(R.id.refresh_button);
                }
            }

            @Override
            public View getNextFocusBackward() {
                return findViewById(R.id.menu_button);
            }
        });

        mBackButton.setOnClickListener(this);
        mBackButton.setLongClickable(true);
        mBackButton.setOnKeyListener(new KeyboardNavigationListener() {
            @Override
            public View getNextFocusForward() {
                if (mForwardButton.isFocusable()) {
                    return findViewById(R.id.forward_button);
                } else {
                    return findViewById(R.id.refresh_button);
                }
            }

            @Override
            public View getNextFocusBackward() {
                if (mHomeButton.getVisibility() == VISIBLE) {
                    return findViewById(R.id.home_button);
                } else {
                    return findViewById(R.id.menu_button);
                }
            }
        });

        mForwardButton.setOnClickListener(this);
        mForwardButton.setLongClickable(true);
        mForwardButton.setOnKeyListener(new KeyboardNavigationListener() {
            @Override
            public View getNextFocusForward() {
                return findViewById(R.id.refresh_button);
            }

            @Override
            public View getNextFocusBackward() {
                if (mBackButton.isFocusable()) {
                    return mBackButton;
                } else if (mHomeButton.getVisibility() == VISIBLE) {
                    return findViewById(R.id.home_button);
                } else {
                    return findViewById(R.id.menu_button);
                }
            }
        });

        mReloadButton.setOnClickListener(this);
        mReloadButton.setOnLongClickListener(this);
        mReloadButton.setOnKeyListener(new KeyboardNavigationListener() {
            @Override
            public View getNextFocusForward() {
                return findViewById(R.id.url_bar);
            }

            @Override
            public View getNextFocusBackward() {
                if (mForwardButton.isFocusable()) {
                    return mForwardButton;
                } else if (mBackButton.isFocusable()) {
                    return mBackButton;
                } else if (mHomeButton.getVisibility() == VISIBLE) {
                    return findViewById(R.id.home_button);
                } else {
                    return findViewById(R.id.menu_button);
                }
            }
        });

        mBookmarkButton.setOnClickListener(this);
        mBookmarkButton.setOnLongClickListener(this);

        getMenuButton().setOnKeyListener(new KeyboardNavigationListener() {
            @Override
            public View getNextFocusForward() {
                return getCurrentTabView();
            }

            @Override
            public View getNextFocusBackward() {
                return findViewById(R.id.url_bar);
            }

            @Override
            protected boolean handleEnterKeyPress() {
                return getMenuButtonHelper().onEnterKeyPress(getMenuButton());
            }
        });
        if (HomepageManager.isHomepageEnabled()) {
            mHomeButton.setVisibility(VISIBLE);
        }

        mSaveOfflineButton.setOnClickListener(this);
        mSaveOfflineButton.setOnLongClickListener(this);
        mBraveShieldsButton.setOnClickListener(this);
        mBraveShieldsButton.setOnLongClickListener(this);
        mBraveRewardsPanelButton.setOnClickListener(this);
        mBraveRewardsPanelButton.setOnLongClickListener(this);
        mBraveRewardsNativeWorker = BraveRewardsNativeWorker.getInstance();
        if (mBraveRewardsNativeWorker != null) {
            mBraveRewardsNativeWorker.AddObserver(this);
            mBraveRewardsNativeWorker.GetAllNotifications();
        }
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        if (mBackButton == originalView) {
            // Display backwards navigation popup.
            displayNavigationPopup(false, mBackButton);
            return true;
        } else if (mForwardButton == originalView) {
            // Display forwards navigation popup.
            displayNavigationPopup(true, mForwardButton);
            return true;
        }
        return super.showContextMenuForChild(originalView);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // Ensure the the popup is not shown after resuming activity from background.
        if (hasWindowFocus && mNavigationPopup != null) {
            mNavigationPopup.dismiss();
            mNavigationPopup = null;
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    private void displayNavigationPopup(boolean isForward, View anchorView) {
        Tab tab = getToolbarDataProvider().getTab();
        if (tab == null || tab.getWebContents() == null) return;
        mNavigationPopup = new NavigationPopup(tab.getProfile(), getContext(),
                tab.getWebContents().getNavigationController(),
                isForward ? NavigationPopup.Type.TABLET_FORWARD : NavigationPopup.Type.TABLET_BACK);
        mNavigationPopup.show(anchorView);
    }

    @Override
    public void onClick(View v) {
        if (mHomeButton == v) {
            openHomepage();
        } else if (mBackButton == v) {
            if (!back()) return;
            RecordUserAction.record("MobileToolbarBack");
        } else if (mForwardButton == v) {
            forward();
            RecordUserAction.record("MobileToolbarForward");
        } else if (mReloadButton == v) {
            stopOrReloadCurrentTab();
        } else if (mBookmarkButton == v) {
            if (mBookmarkListener != null) {
                mBookmarkListener.onClick(mBookmarkButton);
                RecordUserAction.record("MobileToolbarToggleBookmark");
            }
        } else if (mSaveOfflineButton == v) {
            DownloadUtils.downloadOfflinePage(getContext(), getToolbarDataProvider().getTab());
            RecordUserAction.record("MobileToolbarDownloadPage");
        } else if (mBraveShieldsButton == v) {
            if (null != mBraveShieldsButton) {
                mBraveShieldsListener.onClick(mBraveShieldsButton);
                RecordUserAction.record("MobileToolbarShowBraveShields");
            }
        } else if (mBraveRewardsPanelButton == v) {
            if (null == mRewardsPopup) {
                mRewardsPopup = new BraveRewardsPanelPopup(v);
                mRewardsPopup.showLikePopDownMenu();
            }
        }
    }

    @Override
    public void onRewardsPanelDismiss() {
        mRewardsPopup = null;
    }

    @Override
    public void dismissRewardsPanel() {
        if ( null != mRewardsPopup ) {
            mRewardsPopup.dismiss();
            mRewardsPopup = null;
        }
    }

    private boolean mayShowBraveAdsOobeDialog() {
        if (BraveAdsSignupDialog.shouldShowNewUserDialog(getContext())) {
            BraveAdsSignupDialog.showNewUserDialog(getContext());
            return true;
        } else if (BraveAdsSignupDialog.shouldShowExistingUserDialog(getContext())) {
            BraveAdsSignupDialog.showExistingUserDialog(getContext());
            return true;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        String description = "";
        Context context = getContext();
        Resources resources = context.getResources();

        if (v == mReloadButton) {
            description = (mReloadButton.getDrawable().getLevel()
                                  == resources.getInteger(R.integer.reload_button_level_reload))
                    ? resources.getString(R.string.menu_refresh)
                    : resources.getString(R.string.menu_stop_refresh);
        } else if (v == mBookmarkButton) {
            description = resources.getString(R.string.menu_bookmark);
        } else if (v == mSaveOfflineButton) {
            description = resources.getString(R.string.menu_download);
        } else if (v == mHomeButton) {
            description = resources.getString(R.string.accessibility_toolbar_btn_home);
        } else if (v == mBraveShieldsButton) {
            description = description = resources.getString(R.string.accessibility_toolbar_btn_brave_shields);
        } else if (v == mBraveRewardsPanelButton) {
            description = resources.getString(R.string.accessibility_toolbar_btn_brave_rewards);
        }

        return AccessibilityUtil.showAccessibilityToast(context, v, description);
    }

    private void updateSwitcherButtonVisibility(boolean enabled) {
        mAccessibilitySwitcherButton.setVisibility(
                mShowTabStack || enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    boolean isReadyForTextureCapture() {
        return !urlHasFocus();
    }

    @Override
    void onTabOrModelChanged() {
        super.onTabOrModelChanged();
        final boolean incognito = isIncognito();
        if (mIsIncognito == null || mIsIncognito != incognito) {
            // TODO (amaralp): Have progress bar observe theme color and incognito changes directly.
            getProgressBar().setThemeColor(
                    ColorUtils.getDefaultThemeColor(getResources(), incognito), isIncognito());

            mIsIncognito = incognito;
        }

        updateNtp();
    }

    @Override
    public void onTintChanged(ColorStateList tint, boolean useLight) {
        ApiCompatibilityUtils.setImageTintList(mHomeButton, tint);
        ApiCompatibilityUtils.setImageTintList(mBackButton, tint);
        ApiCompatibilityUtils.setImageTintList(mForwardButton, tint);
        ApiCompatibilityUtils.setImageTintList(mSaveOfflineButton, tint);
        ApiCompatibilityUtils.setImageTintList(mReloadButton, tint);
        mAccessibilitySwitcherButton.setUseLightDrawables(useLight);
    }

    @Override
    public void onThemeColorChanged(int color, boolean shouldAnimate) {
        setBackgroundColor(color);
        final int textBoxColor = ColorUtils.getTextBoxColorForToolbarBackground(
                getResources(), false, color, isIncognito());
        mLocationBar.getBackground().setColorFilter(textBoxColor, PorterDuff.Mode.SRC_IN);
        mShieldsLayout.getBackground().setColorFilter(textBoxColor, PorterDuff.Mode.SRC_IN);
        if (mShieldsLayoutIsColorBackground) {
            mShieldsLayout.setBackgroundColor(ColorUtils.getDefaultThemeColor(getResources(), isIncognito()));
        }
        mRewardsLayout.getBackground().setColorFilter(textBoxColor, PorterDuff.Mode.SRC_IN);

        mLocationBar.updateVisualsForState();
    }

    @Override
    public void OnWalletInitialized(int error_code) {
        if (error_code == 20) { // ledger::Result::SAFETYNET_ATTESTATION_FAILED
            PrefServiceBridge.getInstance().setSafetynetCheckFailed(true);
            if (mRewardsLayout != null && mShieldsLayout != null) {
                mRewardsLayout.setVisibility(View.GONE);
                mShieldsLayout.setBackgroundDrawable(
                    ApiCompatibilityUtils.getDrawable(getContext().getResources(), R.drawable.modern_toolbar_background_grey_end_segment));
                mShieldsLayoutIsColorBackground = false;
            }
            // Show message
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.Theme_Chromium_AlertDialog);
            AlertDialog alertDialog = alert.setMessage(getResources().getString(R.string.brave_rewards_not_available))
                                           .setPositiveButton(R.string.ok, null)
                                           .create();
            alertDialog.getDelegate().setHandleNativeActionModesEnabled(false);
            alertDialog.show();
            // If current tab is rewards tab we close it, as it is not valid anymore
            Tab currentTab = getToolbarDataProvider().getTab();
            if (currentTab != null && getToolbarDataProvider().getCurrentUrl().equals(ChromeTabbedActivity.REWARDS_SETTINGS_URL)) {
                if (getContext() instanceof ChromeActivity) {
                    ChromeActivity activity = (ChromeActivity)getContext();
                    activity.getCurrentTabModel().closeTab(currentTab);
                }
            }
        } else if (error_code == 12) {
            // Check and set flag to show Brave Rewards icon if enabled
            SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean(BraveRewardsPanelPopup.PREF_WAS_BRAVE_REWARDS_TURNED_ON, true);
            if (sharedPreferences.getBoolean(PREF_HIDE_BRAVE_ICON, false)) {
                sharedPreferencesEditor.putBoolean(PREF_HIDE_BRAVE_ICON, false);
                sharedPreferencesEditor.apply();
                RestartWorker.AskForRelaunch((ChromeActivity)getContext());
            }
            sharedPreferencesEditor.apply();
        }
    }

    @Override
    public void OnWalletProperties(int error_code) {}

    @Override
    public void OnPublisherInfo(int tabId) {}

    @Override
    public void OnGetCurrentBalanceReport(String[] report) {}

    @Override
    public void OnNotificationAdded(String id, int type, long timestamp,
            String[] args) {
        if (mBraveRewardsNativeWorker != null) {
            if (type == BraveRewardsNativeWorker.REWARDS_NOTIFICATION_BACKUP_WALLET) {
                mBraveRewardsNativeWorker.DeleteNotification(id);
            } else if (type == BraveRewardsNativeWorker.REWARDS_NOTIFICATION_GRANT) {
                // Set flag
                SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putBoolean(BraveRewardsPanelPopup.PREF_GRANTS_NOTIFICATION_RECEIVED, true);
                sharedPreferencesEditor.apply();
            }
            mBraveRewardsNativeWorker.GetAllNotifications();
        }
    }

    @Override
    public void OnNotificationsCount(int count) {
        SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
        boolean rewardsEnabled = sharedPreferences.getBoolean(
            BraveRewardsPanelPopup.PREF_WAS_BRAVE_REWARDS_ENABLED, true);
        boolean shouldUpdateRewardsCountForAdsOobe =
            BraveAdsSignupDialog.shouldShowNewUserDialog(getContext()) ||
            BraveAdsSignupDialog.shouldShowExistingUserDialog(getContext());
        if (mBraveRewardsNotificationsCount != null &&
            (rewardsEnabled || shouldUpdateRewardsCountForAdsOobe)) {
            if (shouldUpdateRewardsCountForAdsOobe) count = count + 1;
            if (count != 0) {
                String value = Integer.toString(count);
                if (count > 99) {
                    mBraveRewardsNotificationsCount.setBackground(
                        getResources().getDrawable(R.drawable.brave_rewards_rectangle));
                    value = "99+";
                } else {
                    mBraveRewardsNotificationsCount.setBackground(
                        getResources().getDrawable(R.drawable.brave_rewards_circle));
                }
                mBraveRewardsNotificationsCount.setText(value);
                mBraveRewardsNotificationsCount.setVisibility(View.VISIBLE);
            } else {
                mBraveRewardsNotificationsCount.setText("");
                mBraveRewardsNotificationsCount.setVisibility(View.GONE);
            }
        }

        mayShowBraveAdsOobeDialog();
    }

    @Override
    public void OnGetLatestNotification(String id, int type, long timestamp,
            String[] args) {}

    @Override
    public void OnNotificationDeleted(String id) {}

    @Override
    public void OnIsWalletCreated(boolean created) {}

    @Override
    public void OnGetPendingContributionsTotal(double amount) {}

    @Override
    public void OnGetRewardsMainEnabled(boolean enabled) {}

    @Override
    public void OnGetAutoContributeProps() {}

    @Override
    public void OnGetReconcileStamp(long timestamp) {}

    @Override
    public void OnRecurringDonationUpdated() {}

    @Override
    public void OnResetTheWholeState(boolean success) {}

    @Override
    public void OnRewardsMainEnabled(boolean enabled) {
        SharedPreferences sharedPreferences = ContextUtils.getAppSharedPreferences();
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(BraveRewardsPanelPopup.PREF_WAS_BRAVE_REWARDS_ENABLED, enabled);
        sharedPreferencesEditor.apply();
        if (mBraveRewardsNotificationsCount != null) {
            String count = mBraveRewardsNotificationsCount.getText().toString();
            if (!count.isEmpty()) {
                mBraveRewardsNotificationsCount.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Called when the currently visible New Tab Page changes.
     */
    private void updateNtp() {
        NewTabPage newVisibleNtp = getToolbarDataProvider().getNewTabPageForCurrentTab();
        if (mVisibleNtp == newVisibleNtp) return;

        if (mVisibleNtp != null) {
            mVisibleNtp.setSearchBoxScrollListener(null);
        }
        mVisibleNtp = newVisibleNtp;
        if (mVisibleNtp != null) {
            mVisibleNtp.setSearchBoxScrollListener(new NewTabPage.OnSearchBoxScrollListener() {
                @Override
                public void onNtpScrollChanged(float scrollPercentage) {
                    // Fade the search box out in the first 40% of the scrolling transition.
                    float alpha = Math.max(1f - scrollPercentage * 2.5f, 0f);
                    mVisibleNtp.setSearchBoxAlpha(alpha);
                    mVisibleNtp.setSearchProviderLogoAlpha(alpha);
                }
            });
        }
    }

    @Override
    void onTabContentViewChanged() {
        super.onTabContentViewChanged();
        updateNtp();
    }

    @Override
    void updateButtonVisibility() {
        if (mBraveRewardsPanelButton != null && mBraveRewardsPanelButton.getDrawable() != null) {
            if (isIncognito()) {
                mBraveRewardsPanelButton.setEnabled(false);
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                mBraveRewardsPanelButton.getDrawable().setColorFilter(filter);
            } else {
                mBraveRewardsPanelButton.setEnabled(true);
                mBraveRewardsPanelButton.getDrawable().clearColorFilter();
            }
        }

        if (FeatureUtilities.isNewTabPageButtonEnabled()) {
            mHomeButton.setVisibility(isIncognito() ? GONE : VISIBLE);
        }
        mLocationBar.updateButtonVisibility();
    }

    @Override
    void updateBackButtonVisibility(boolean canGoBack) {
        boolean enableButton = canGoBack && !mIsInTabSwitcherMode;
        mBackButton.setEnabled(enableButton);
        mBackButton.setFocusable(enableButton);
    }

    @Override
    void updateForwardButtonVisibility(boolean canGoForward) {
        boolean enableButton = canGoForward && !mIsInTabSwitcherMode;
        mForwardButton.setEnabled(enableButton);
        mForwardButton.setFocusable(enableButton);
    }

    @Override
    void updateReloadButtonVisibility(boolean isReloading) {
        if (isReloading) {
            mReloadButton.getDrawable().setLevel(
                    getResources().getInteger(R.integer.reload_button_level_stop));
            mReloadButton.setContentDescription(
                    getContext().getString(R.string.accessibility_btn_stop_loading));
        } else {
            mReloadButton.getDrawable().setLevel(
                    getResources().getInteger(R.integer.reload_button_level_reload));
            mReloadButton.setContentDescription(
                    getContext().getString(R.string.accessibility_btn_refresh));
        }
        mReloadButton.setEnabled(!mIsInTabSwitcherMode);
    }

    @Override
    void updateBookmarkButton(boolean isBookmarked, boolean editingAllowed) {
        if (isBookmarked) {
            mBookmarkButton.setImageResource(R.drawable.btn_star_filled);
            // TODO (huayinz): Ask UX whether night mode should have a white or blue star.
            // Non-incognito mode shows a blue filled star.
            ApiCompatibilityUtils.setImageTintList(mBookmarkButton,
                    useLight() ? getTint()
                               : AppCompatResources.getColorStateList(
                                       getContext(), R.color.blue_mode_tint));
            mBookmarkButton.setContentDescription(getContext().getString(R.string.edit_bookmark));
        } else {
            mBookmarkButton.setImageResource(R.drawable.btn_star);
            ApiCompatibilityUtils.setImageTintList(mBookmarkButton, getTint());
            mBookmarkButton.setContentDescription(
                    getContext().getString(R.string.accessibility_menu_bookmark));
        }
        mBookmarkButton.setEnabled(editingAllowed);
    }

    @Override
    void setTabSwitcherMode(
            boolean inTabSwitcherMode, boolean showToolbar, boolean delayAnimation) {

        if (mShowTabStack && inTabSwitcherMode) {
            mIsInTabSwitcherMode = true;
            mBackButton.setEnabled(false);
            mForwardButton.setEnabled(false);
            mReloadButton.setEnabled(false);
            mLocationBar.getContainerView().setVisibility(View.INVISIBLE);
            setAppMenuUpdateBadgeSuppressed(true);
        } else {
            mIsInTabSwitcherMode = false;
            mLocationBar.getContainerView().setVisibility(View.VISIBLE);

            setAppMenuUpdateBadgeSuppressed(false);
        }
    }

    @Override
    public void onTabCountChanged(int numberOfTabs, boolean isIncognito) {
        mAccessibilitySwitcherButton.setContentDescription(getResources().getQuantityString(
                R.plurals.accessibility_toolbar_btn_tabswitcher_toggle, numberOfTabs,
                numberOfTabs));
    }

    @Override
    void setTabCountProvider(TabCountProvider tabCountProvider) {
        tabCountProvider.addObserver(this);
        mAccessibilitySwitcherButton.setTabCountProvider(tabCountProvider);
    }

    @Override
    void onAccessibilityStatusChanged(boolean enabled) {
        mShowTabStack = enabled && isAccessibilityTabSwitcherPreferenceEnabled();
        updateSwitcherButtonVisibility(mShowTabStack);
    }

    @Override
    void setBookmarkClickHandler(OnClickListener listener) {
        mBookmarkListener = listener;
    }

    @Override
    void setOnTabSwitcherClickHandler(OnClickListener listener) {
        mAccessibilitySwitcherButton.setOnTabSwitcherClickHandler(listener);
    }

    @Override
    void setBraveShieldsClickHandler(OnClickListener listener) {
        mBraveShieldsListener = listener;
    }

    @Override
    void onHomeButtonUpdate(boolean homeButtonEnabled) {
        mHomeButton.setVisibility(homeButtonEnabled ? VISIBLE : GONE);
    }

    @Override
    public LocationBar getLocationBar() {
        return mLocationBar;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // After the first layout, button visibility changes should be animated. On the first
        // layout, the button visibility shouldn't be animated because the visibility may be
        // changing solely because Chrome was launched into multi-window.
        mShouldAnimateButtonVisibilityChange = true;

        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Hide or show toolbar buttons if needed. With the introduction of multi-window on
        // Android N, the Activity can be < 600dp, in which case the toolbar buttons need to be
        // moved into the menu so that the location bar is usable. The buttons must be shown
        // in onMeasure() so that the location bar gets measured and laid out correctly.
        setToolbarButtonsVisible(MeasureSpec.getSize(widthMeasureSpec)
                >= DeviceFormFactor.getNonMultiDisplayMinimumTabletWidthPx(getContext()));

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setToolbarButtonsVisible(boolean visible) {
        if (mToolbarButtonsVisible == visible) return;

        mToolbarButtonsVisible = visible;

        if (mShouldAnimateButtonVisibilityChange) {
            runToolbarButtonsVisibilityAnimation(visible);
        } else {
            for (ImageButton button : mToolbarButtons) {
                button.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            mLocationBar.setShouldShowButtonsWhenUnfocused(visible);
            setStartPaddingBasedOnButtonVisibility(visible);
        }
    }

    /**
     * Sets the toolbar start padding based on whether the buttons are visible.
     * @param buttonsVisible Whether the toolbar buttons are visible.
     */
    private void setStartPaddingBasedOnButtonVisibility(boolean buttonsVisible) {
        buttonsVisible = buttonsVisible || mHomeButton.getVisibility() == View.VISIBLE;

        ViewCompat.setPaddingRelative(this,
                buttonsVisible ? mStartPaddingWithButtons : mStartPaddingWithoutButtons,
                getPaddingTop(), ViewCompat.getPaddingEnd(this), getPaddingBottom());
    }

    /**
     * @return The difference in start padding when the buttons are visible and when they are not
     *         visible.
     */
    public int getStartPaddingDifferenceForButtonVisibilityAnimation() {
        // If the home button is visible then the padding doesn't change.
        return mHomeButton.getVisibility() == View.VISIBLE
                ? 0
                : mStartPaddingWithButtons - mStartPaddingWithoutButtons;
    }

    private void runToolbarButtonsVisibilityAnimation(boolean visible) {
        if (mButtonVisibilityAnimators != null) mButtonVisibilityAnimators.cancel();

        mButtonVisibilityAnimators =
                visible ? buildShowToolbarButtonsAnimation() : buildHideToolbarButtonsAnimation();
        mButtonVisibilityAnimators.start();
    }

    private AnimatorSet buildShowToolbarButtonsAnimation() {
        Collection<Animator> animators = new ArrayList<>();

        // Create animators for all of the toolbar buttons.
        for (ImageButton button : mToolbarButtons) {
            animators.add(mLocationBar.createShowButtonAnimator(button));
        }
        animators.add(mLocationBar.createShowButtonAnimator(mBraveShieldsButton));

        // Add animators for location bar.
        animators.addAll(mLocationBar.getShowButtonsWhenUnfocusedAnimators(
                getStartPaddingDifferenceForButtonVisibilityAnimation()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (ImageButton button : mToolbarButtons) {
                    button.setVisibility(View.VISIBLE);
                }
                mBraveShieldsButton.setVisibility(View.VISIBLE);
                // Set the padding at the start of the animation so the toolbar buttons don't jump
                // when the animation ends.
                setStartPaddingBasedOnButtonVisibility(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mButtonVisibilityAnimators = null;
            }
        });

        return set;
    }

    private AnimatorSet buildHideToolbarButtonsAnimation() {
        Collection<Animator> animators = new ArrayList<>();

        // Create animators for all of the toolbar buttons.
        for (ImageButton button : mToolbarButtons) {
            animators.add(mLocationBar.createHideButtonAnimator(button));
        }
        animators.add(mLocationBar.createHideButtonAnimator(mBraveShieldsButton));

        // Add animators for location bar.
        animators.addAll(mLocationBar.getHideButtonsWhenUnfocusedAnimators(
                getStartPaddingDifferenceForButtonVisibilityAnimation()));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Only set end visibility and alpha if the animation is ending because it's
                // completely finished and not because it was canceled.
                if (mToolbarButtons[0].getAlpha() == 0.f) {
                    for (ImageButton button : mToolbarButtons) {
                        button.setVisibility(View.GONE);
                        button.setAlpha(1.f);
                    }
                    mBraveShieldsButton.setVisibility(View.GONE);
                    mBraveShieldsButton.setAlpha(1.f);
                    // Set the padding at the end of the animation so the toolbar buttons don't jump
                    // when the animation starts.
                    setStartPaddingBasedOnButtonVisibility(false);
                }

                mButtonVisibilityAnimators = null;
            }
        });

        return set;
    }

    private boolean isAccessibilityTabSwitcherPreferenceEnabled() {
        return ChromePreferenceManager.getInstance().readBoolean(
                ChromePreferenceManager.ACCESSIBILITY_TAB_SWITCHER, true);
    }
}
