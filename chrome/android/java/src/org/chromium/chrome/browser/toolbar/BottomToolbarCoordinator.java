// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.toolbar;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.ActivityTabProvider;
import org.chromium.chrome.browser.appmenu.AppMenuButtonHelper;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.bookmarks.BookmarkUtils;
import org.chromium.chrome.browser.compositor.layouts.LayoutManager;
import org.chromium.chrome.browser.compositor.layouts.OverviewModeBehavior;
import org.chromium.chrome.browser.compositor.layouts.ToolbarSwipeLayout;
import org.chromium.chrome.browser.fullscreen.ChromeFullscreenManager;
import org.chromium.chrome.browser.history.HistoryManagerUtils;
import org.chromium.chrome.browser.modelutil.PropertyKey;
import org.chromium.chrome.browser.modelutil.PropertyModelChangeProcessor;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.toolbar.BottomToolbarViewBinder.ViewHolder;
import org.chromium.chrome.browser.toolbar.ToolbarButtonSlotData.ToolbarButtonData;
import org.chromium.chrome.browser.widget.TintedImageButton;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.resources.ResourceManager;

/**
 * The root coordinator for the bottom toolbar. It has two subcomponents. The browing mode bottom
 * toolbar and the tab switcher mode bottom toolbar.
 */
public class BottomToolbarCoordinator {
    /** The browsing mode bottom toolbar component */
    private final BrowsingModeBottomToolbarCoordinator mBrowsingModeCoordinator;

    /** The tab switcher mode bottom toolbar component */
    private TabSwitcherBottomToolbarCoordinator mTabSwitcherModeCoordinator;

    /** The tab switcher mode bottom toolbar stub that will be inflated when native is ready. */
    private final ViewStub mTabSwitcherModeStub;

    /** The bookmarks button component that lives in the bottom toolbar. */
    private final TintedImageButton mBookmarksButton;

    /** The history button component that lives in the bottom toolbar. */
    private final TintedImageButton mHistoryButton;

    /** The menu button that lives in the bottom toolbar. */
    private final MenuButton mMenuButton;

    /** The light mode tint to be used in bottom toolbar buttons. */
    private final ColorStateList mLightModeTint;

    /** The dark mode tint to be used in bottom toolbar buttons. */
    private final ColorStateList mDarkModeTint;

    /** The primary color to be used in normal mode. */
    private final int mNormalPrimaryColor;

    /** The primary color to be used in incognito mode. */
    private final int mIncognitoPrimaryColor;

    /** The toolbar model that tells us about the current toolbar state and data. */
    private final ToolbarModel mToolbarModel;

    /** The invoking activity. */
    private final ChromeActivity mActivity;

    /**
     * Build the coordinator that manages the bottom toolbar.
     * @param fullscreenManager A {@link ChromeFullscreenManager} to update the bottom controls
     *                          height for the renderer.
     * @param stub The bottom toolbar {@link ViewStub} to inflate.
     * @param tabProvider The {@link ActivityTabProvider} used for making the IPH.
     * @param homeButtonListener The {@link OnClickListener} for the home button.
     * @param searchAcceleratorListener The {@link OnClickListener} for the search accelerator.
     * @param shareButtonListener The {@link OnClickListener} for the share button.
     */
    public BottomToolbarCoordinator(ChromeFullscreenManager fullscreenManager, ViewGroup root,
            ToolbarButtonSlotData firstSlotData, ToolbarButtonSlotData secondSlotData, ChromeActivity activity, 
            ToolbarModel toolbarModel) {
        BottomToolbarModel model = new BottomToolbarModel();

        int shadowHeight =
                root.getResources().getDimensionPixelOffset(R.dimen.toolbar_shadow_height);

        // This is the Android view component of the views that constitute the bottom toolbar.
        View inflatedView = ((ViewStub) root.findViewById(R.id.bottom_toolbar)).inflate();
        final ScrollingBottomViewResourceFrameLayout toolbarRoot =
                (ScrollingBottomViewResourceFrameLayout) inflatedView;
        toolbarRoot.setTopShadowHeight(shadowHeight);

        PropertyModelChangeProcessor<BottomToolbarModel, ViewHolder, PropertyKey> processor =
                new PropertyModelChangeProcessor<>(
                        model, new ViewHolder(toolbarRoot), new BottomToolbarViewBinder());
        model.addObserver(processor);

        mTabSwitcherButtonCoordinator = new TabSwitcherButtonCoordinator(toolbarRoot);
        mBookmarksButton = toolbarRoot.findViewById(R.id.bookmarks_button);
        mHistoryButton = toolbarRoot.findViewById(R.id.history_button);
        mMenuButton = toolbarRoot.findViewById(R.id.menu_button_wrapper);

        mLightModeTint =
                AppCompatResources.getColorStateList(root.getContext(), R.color.light_mode_tint);
        mDarkModeTint =
                AppCompatResources.getColorStateList(root.getContext(), R.color.dark_mode_tint);

        mNormalPrimaryColor =
                ApiCompatibilityUtils.getColor(root.getResources(), R.color.modern_primary_color);
        mIncognitoPrimaryColor = ApiCompatibilityUtils.getColor(
                root.getResources(), R.color.incognito_modern_primary_color);

        mMediator = new BottomToolbarMediator(model, fullscreenManager, root.getResources(),
                firstSlotData, secondSlotData, mNormalPrimaryColor);
        mActivity = activity;
        mToolbarModel = toolbarModel;
    }

    /**
     * Initialize the bottom toolbar with the components that had native initialization
     * dependencies.
     * <p>
     * Calling this must occur after the native library have completely loaded.
     * @param resourceManager A {@link ResourceManager} for loading textures into the compositor.
     * @param layoutManager A {@link LayoutManager} to attach overlays to.
     * @param tabSwitcherListener An {@link OnClickListener} that is triggered when the
     *                            tab switcher button is clicked.
     * @param newTabClickListener An {@link OnClickListener} that is triggered when the
     *                            new tab button is clicked.
     * @param menuButtonHelper An {@link AppMenuButtonHelper} that is triggered when the
     *                         menu button is clicked.
     * @param tabModelSelector A {@link TabModelSelector} that incognito toggle tab layout uses to
                               switch between normal and incognito tabs.
     * @param overviewModeBehavior The overview mode manager.
     * @param windowAndroid A {@link WindowAndroid} for watching keyboard visibility events.
     * @param tabCountProvider Updates the tab count number in the tab switcher button and in the
     *                         incognito toggle tab layout.
     * @param incognitoStateProvider Notifies components when incognito mode is entered or exited.
     */
    public void initializeWithNative(ResourceManager resourceManager, LayoutManager layoutManager,
            OnClickListener tabSwitcherListener, OnClickListener newTabClickListener,
            AppMenuButtonHelper menuButtonHelper, TabModelSelector tabModelSelector,
            OverviewModeBehavior overviewModeBehavior, WindowAndroid windowAndroid,
            TabCountProvider tabCountProvider, IncognitoStateProvider incognitoStateProvider) {
        mBrowsingModeCoordinator.initializeWithNative(resourceManager, layoutManager,
                tabSwitcherListener, menuButtonHelper, overviewModeBehavior, windowAndroid,
                tabCountProvider, incognitoStateProvider, tabModelSelector);
        mTabSwitcherModeCoordinator = new TabSwitcherBottomToolbarCoordinator(mTabSwitcherModeStub,
                incognitoStateProvider, newTabClickListener, menuButtonHelper, tabModelSelector,
                overviewModeBehavior, tabCountProvider);
        // (Albert Wang): We're using history in favor of search acceleration
        // mMediator.setSearchAcceleratorListener(searchAcceleratorListener);
        mMediator.setLayoutManager(layoutManager);
        mMediator.setResourceManager(resourceManager);
        mMediator.setOverviewModeBehavior(overviewModeBehavior);
        mMediator.setToolbarSwipeHandler(layoutManager.getToolbarSwipeHandler());
        mMediator.setWindowAndroid(windowAndroid);
        setIncognito(isIncognito);
        mMediator.setTabSwitcherButtonData(
                firstSlotTabSwitcherButtonData, secondSlotTabSwitcherButtonData);

        mTabSwitcherButtonCoordinator.setTabSwitcherListener(tabSwitcherListener);
        mTabSwitcherButtonCoordinator.setTabModelSelector(tabModelSelector);

        mMenuButton.setTouchListener(menuButtonHelper);
        mMenuButton.setAccessibilityDelegate(menuButtonHelper);
        mBookmarksButtonCoordinator.setButtonListeners(bookmarksButtonListener, null);
        mBookmarksButtonCoordinator.setOverviewModeBehavior(
                overviewModeBehavior, ToolbarButtonCoordinator.ButtonVisibility.BROWSING_MODE);

        mBookmarksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BookmarkUtils.showBookmarkManager(mActivity);
            }
        });

        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryManagerUtils.showHistoryManager(mActivity, mToolbarModel.getTab());
            }
        });
    }

    /**
     * Show the update badge over the bottom toolbar's app menu.
     */
    public void showAppMenuUpdateBadge() {
        mBrowsingModeCoordinator.showAppMenuUpdateBadge();
        if (mTabSwitcherModeCoordinator != null) {
            mTabSwitcherModeCoordinator.showAppMenuUpdateBadge();
        }
    }

    /**
     * Remove the update badge.
     */
    public void removeAppMenuUpdateBadge() {
        mBrowsingModeCoordinator.removeAppMenuUpdateBadge();
        if (mTabSwitcherModeCoordinator != null) {
            mTabSwitcherModeCoordinator.removeAppMenuUpdateBadge();
        }
    }

    /**
     * @return Whether the update badge is showing.
     */
    public boolean isShowingAppMenuUpdateBadge() {
        return mBrowsingModeCoordinator.isShowingAppMenuUpdateBadge();
    }

    /**
     * @param layout The {@link ToolbarSwipeLayout} that the bottom toolbar will hook into. This
     *               allows the bottom toolbar to provide the layout with scene layers with the
     *               bottom toolbar's texture.
     */
    public void setToolbarSwipeLayout(ToolbarSwipeLayout layout) {
        mBrowsingModeCoordinator.setToolbarSwipeLayout(layout);
    }

    /**
     * @return The wrapper for the app menu button.
     */
    public MenuButton getMenuButtonWrapper() {
        if (mBrowsingModeCoordinator.isVisible()) {
            return mBrowsingModeCoordinator.getMenuButton();
        }
        if (mTabSwitcherModeCoordinator != null) {
            return mTabSwitcherModeCoordinator.getMenuButton();
        }
        return null;
    }

    /**
     * Clean up any state when the bottom toolbar is destroyed.
     */
    public void destroy() {
        mBrowsingModeCoordinator.destroy();
        if (mTabSwitcherModeCoordinator != null) {
            mTabSwitcherModeCoordinator.destroy();
            mTabSwitcherModeCoordinator = null;
        }
    }
}
