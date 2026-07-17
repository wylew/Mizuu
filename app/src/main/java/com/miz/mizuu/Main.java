/*
 * Copyright (C) 2014 Michell Bak
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

package com.miz.mizuu;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.miz.base.MizActivity;
import com.miz.loader.MovieLoader;
import com.miz.loader.TvShowLoader;
import com.miz.mizuu.fragments.AllLibraryFragment;
import com.miz.mizuu.fragments.MovieLibraryFragment;
import com.miz.mizuu.fragments.TvShowLibraryFragment;
import com.miz.utils.LocalBroadcastUtils;

import static com.miz.functions.PreferenceKeys.STARTUP_SELECTION;

@SuppressLint("NewApi")
public class Main extends MizActivity {

    public static final int ALL = 0, MOVIES = 1, SHOWS = 2;
    private int selectedIndex, mStartup;
    private MaterialButtonToggleGroup mLibraryToggleGroup;
    private View mBottomControls;
    private MaterialButton mSearchButton, mMenuButton;

    @Override
    protected int getLayoutResource() {
        return R.layout.menu_drawer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Mizuu_Theme_Overview);
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        mStartup = Integer.valueOf(settings.getString(STARTUP_SELECTION, "1"));

        mBottomControls = findViewById(R.id.bottom_controls);
        mLibraryToggleGroup = findViewById(R.id.library_toggle_group);
        mSearchButton = findViewById(R.id.search_button);
        mMenuButton = findViewById(R.id.menu_button);

        mLibraryToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_all) {
                    loadFragment(ALL);
                } else if (checkedId == R.id.btn_movies) {
                    loadFragment(MOVIES);
                } else if (checkedId == R.id.btn_tv) {
                    loadFragment(SHOWS);
                }
            }
        });

        mSearchButton.setOnClickListener(v -> {
            onSearchRequested();
        });

        mMenuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.main_menu_m3, popup.getMenu());
            popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
            popup.show();
        });

        // Handle insets for bottom controls ONLY.
        // We do NOT add padding to the content frame so that the list scrolls UNDER the floating buttons.
        ViewCompat.setOnApplyWindowInsetsListener(mBottomControls, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom + 16);
            return windowInsets;
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("selectedIndex")) {
            selectedIndex = savedInstanceState.getInt("selectedIndex");
        } else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("startup")) {
            selectedIndex = Integer.parseInt(getIntent().getExtras().getString("startup"));
        } else {
            selectedIndex = mStartup;
        }

        // Sync toggle group state
        int checkId = R.id.btn_all;
        if (selectedIndex == MOVIES) checkId = R.id.btn_movies;
        else if (selectedIndex == SHOWS) checkId = R.id.btn_tv;
        
        mLibraryToggleGroup.check(checkId);
        loadFragment(selectedIndex);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_LIBRARY));
    }

    private void loadFragment(int type) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag("lib_frag" + type);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
            switch (type) {
                case ALL:
                    ft.replace(R.id.content_frame, AllLibraryFragment.newInstance(), "lib_frag" + type);
                    break;
                case MOVIES:
                    ft.replace(R.id.content_frame, MovieLibraryFragment.newInstance(MovieLoader.ALL_MOVIES), "lib_frag" + type);
                    break;
                case SHOWS:
                    ft.replace(R.id.content_frame, TvShowLibraryFragment.newInstance(TvShowLoader.ALL_SHOWS), "lib_frag" + type);
                    break;
            }
            ft.commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            int titleRes = R.string.all;
            if (type == MOVIES) titleRes = R.string.chooserMovies;
            else if (type == SHOWS) titleRes = R.string.chooserTVShows;
            getSupportActionBar().setTitle(titleRes);
        }

        selectedIndex = type;
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);

        if (newIntent.hasExtra("startup")) {
            selectedIndex = Integer.parseInt(newIntent.getStringExtra("startup"));
            int checkId = R.id.btn_all;
            if (selectedIndex == MOVIES) checkId = R.id.btn_movies;
            else if (selectedIndex == SHOWS) checkId = R.id.btn_tv;
            mLibraryToggleGroup.check(checkId);
            loadFragment(selectedIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedIndex", selectedIndex);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Library updated
        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            startActivity(new Intent(getApplicationContext(), Preferences.class));
            return true;
        } else if (id == R.id.unidentified_files) {
            if (selectedIndex == MOVIES) {
                startActivity(new Intent(getApplicationContext(), UnidentifiedMovies.class));
            } else {
                startActivity(new Intent(getApplicationContext(), UnidentifiedTvShows.class));
            }
            return true;
        } else if (id == R.id.fileSources) {
            startActivity(new Intent(getApplicationContext(), FileSources.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
