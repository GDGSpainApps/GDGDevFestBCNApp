/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gdgdevfest.android.apps.devfestbcn.ui;

import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.LOGD;
import static com.gdgdevfest.android.apps.devfestbcn.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.gdgdevfest.android.apps.devfestbcn.R;
import com.gdgdevfest.android.apps.devfestbcn.provider.ScheduleContract;
import com.gdgdevfest.android.apps.devfestbcn.sync.SyncHelper;
import com.gdgdevfest.android.apps.devfestbcn.util.AccountUtils;
import com.gdgdevfest.android.apps.devfestbcn.util.HelpUtils;
import com.gdgdevfest.android.apps.devfestbcn.util.UIUtils;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.auth.GoogleAuthUtil;

public class HomeActivity extends BaseActivity implements
        ActionBar.TabListener,
        ViewPager.OnPageChangeListener {

    private static final String TAG = makeLogTag(HomeActivity.class);

    public static final String EXTRA_DEFAULT_TAB
            = "com.gdgdevfest.android.apps.devfestbcn.extra.DEFAULT_TAB";
    public static final String TAB_EXPLORE = "explore";

    private Object mSyncObserverHandle;

    private SocialStreamFragment mSocialStreamFragment;

    private ViewPager mViewPager;
    private Menu mOptionsMenu;
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT > 9) { StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); StrictMode.setThreadPolicy(policy); }
        UIUtils.enableDisableActivitiesByFormFactor(this);
        setContentView(R.layout.activity_home);
        FragmentManager fm = getSupportFragmentManager();
        setTitle(R.string.app_name);
        
        mViewPager = (ViewPager) findViewById(R.id.pager);
        String homeScreenLabel;
        if (mViewPager != null) {
            // Phone setup
            mViewPager.setAdapter(new HomePagerAdapter(getSupportFragmentManager()));
            mViewPager.setOnPageChangeListener(this);
            mViewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
            mViewPager.setPageMargin(getResources()
                    .getDimensionPixelSize(R.dimen.page_margin_width));

            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_my_schedule)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_explore)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_stream)
                    .setTabListener(this));
            setHasTabs();

            if (getIntent() != null
                    && TAB_EXPLORE.equals(getIntent().getStringExtra(EXTRA_DEFAULT_TAB))
                    && savedInstanceState == null) {
                mViewPager.setCurrentItem(1);
            }

            homeScreenLabel = getString(R.string.title_my_schedule);

        } else {
            mSocialStreamFragment = (SocialStreamFragment) fm.findFragmentById(R.id.fragment_stream);

            homeScreenLabel = "Home";
        }
        getSupportActionBar().setHomeButtonEnabled(false);
        triggerRefresh();
        
        EasyTracker.getTracker().sendView(homeScreenLabel);
        LOGD("Tracker", homeScreenLabel);

        }


    @Override
    protected void onDestroy() {
        super.onDestroy();

           }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {
    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);

        int titleId = -1;
        switch (position) {
            case 0:
                titleId = R.string.title_my_schedule;
                break;
            case 1:
                titleId = R.string.title_explore;
                break;
            case 2:
                titleId = R.string.title_stream;
                break;
        }

        String title = getString(titleId);
        EasyTracker.getTracker().sendView(title);
        LOGD("Tracker", title);

    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Since the pager fragments don't have known tags or IDs, the only way to persist the
        // reference is to use putFragment/getFragment. Remember, we're not persisting the exact
        // Fragment instance. This mechanism simply gives us a way to persist access to the
        // 'current' fragment instance for the given fragment (which changes across orientation
        // changes).
        //
        // The outcome of all this is that the "Refresh" menu button refreshes the stream across
        // orientation changes.
        if (mSocialStreamFragment != null) {
            getSupportFragmentManager().putFragment(outState, "stream_fragment",
                    mSocialStreamFragment);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mSocialStreamFragment == null) {
            mSocialStreamFragment = (SocialStreamFragment) getSupportFragmentManager()
                    .getFragment(savedInstanceState, "stream_fragment");
        }
    }

    private class HomePagerAdapter extends FragmentPagerAdapter {
        public HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ScheduleFragment();

                case 1:
                    return new ExploreFragment();

                case 2:
                    return (mSocialStreamFragment = new SocialStreamFragment());
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.home, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setQueryRefinementEnabled(true);
            }
        }
       
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                triggerRefresh();
                return true;

            case R.id.menu_search:
                if (!UIUtils.hasHoneycomb()) {
                    startSearch(null, false, Bundle.EMPTY, false);
                    return true;
                }
                break;

            case R.id.menu_about:
                HelpUtils.showAbout(this);
                return true;

          
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_sign_out:
                AccountUtils.signOut(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerRefresh() {
        SyncHelper.requestManualSync(AccountUtils.getChosenAccount(this));
        if (mSocialStreamFragment != null) {
            mSocialStreamFragment.refresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);

   
        // Refresh options menu.  Menu item visibility could be altered by user preferences.
        supportInvalidateOptionsMenu();
    }

    void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
            } else {
                MenuItemCompat.setActionView(refreshItem, null);
            }
        }
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String accountName = AccountUtils.getChosenAccountName(HomeActivity.this);
                    if (TextUtils.isEmpty(accountName)) {
                        setRefreshActionButtonState(false);
                        return;
                    }

                    Account account = new Account(accountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, ScheduleContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };
}
