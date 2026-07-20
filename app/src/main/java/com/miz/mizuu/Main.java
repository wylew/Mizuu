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
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.miz.base.MizActivity;
import com.miz.functions.PreferenceKeys;
import com.miz.loader.MovieLoader;
import com.miz.loader.TvShowLoader;
import com.miz.mizuu.fragments.AllLibraryFragment;
import com.miz.mizuu.fragments.MovieLibraryFragment;
import com.miz.mizuu.fragments.TvShowLibraryFragment;
import com.miz.utils.LocalBroadcastUtils;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.STARTUP_SELECTION;

@SuppressLint("NewApi")
public class Main extends MizActivity {

    public static final int ALL = 0, MOVIES = 1, SHOWS = 2;
    private int selectedIndex = -1, mStartup;
    private MaterialButtonToggleGroup mLibraryToggleGroup;
    private View mBottomControls;
    private MaterialButton mSearchButton, mMenuButton;
    private MaterialCardView mSearchCard, mToggleCard;
    private SearchView mSearchView;

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
        mSearchButton = findViewById(R.id.fab_search);
        mMenuButton = findViewById(R.id.fab_menu);
        mSearchCard = findViewById(R.id.search_card);
        mToggleCard = findViewById(R.id.toggle_card);
        mSearchView = findViewById(R.id.bottom_search_view);

        mLibraryToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int type = ALL;
                if (checkedId == R.id.btn_movies) type = MOVIES;
                else if (checkedId == R.id.btn_tv) type = SHOWS;
                
                if (selectedIndex != type) {
                    loadFragment(type);
                }
            }
        });

        mSearchButton.setOnClickListener(v -> toggleSearch(true));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment frag = getSupportFragmentManager().findFragmentByTag("lib_frag" + selectedIndex);
                if (frag instanceof AllLibraryFragment) {
                    ((AllLibraryFragment) frag).search(newText);
                } else if (frag instanceof MovieLibraryFragment) {
                    ((MovieLibraryFragment) frag).search(newText);
                } else if (frag instanceof TvShowLibraryFragment) {
                    ((TvShowLibraryFragment) frag).search(newText);
                }
                return true;
            }
        });

        mSearchView.setOnCloseListener(() -> {
            toggleSearch(false);
            return true;
        });

        mMenuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.main_menu_m3, popup.getMenu());
            popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
            popup.show();
        });

        // Handle insets for bottom controls
        ViewCompat.setOnApplyWindowInsetsListener(mBottomControls, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom + 16);
            return windowInsets;
        });

        // Back behavior handling
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mSearchCard.getVisibility() == View.VISIBLE) {
                    toggleSearch(false);
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
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

    private void toggleSearch(boolean show) {
        if (show) {
            mSearchCard.setVisibility(View.VISIBLE);
            mToggleCard.setVisibility(View.GONE);
            mSearchButton.setVisibility(View.GONE);
            mMenuButton.setVisibility(View.GONE);
            mSearchView.requestFocus();
            mSearchView.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(mSearchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);
        } else {
            mSearchCard.setVisibility(View.GONE);
            mToggleCard.setVisibility(View.VISIBLE);
            mSearchButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
            mSearchView.setQuery("", false);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
        }
    }

    private void loadFragment(int type) {
        selectedIndex = type;
        String tag = "lib_frag" + type;
        Fragment frag = getSupportFragmentManager().findFragmentByTag(tag);
        
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        
        if (frag == null) {
            switch (type) {
                case ALL:
                    frag = AllLibraryFragment.newInstance();
                    break;
                case MOVIES:
                    frag = MovieLibraryFragment.newInstance(MovieLoader.ALL_MOVIES);
                    break;
                case SHOWS:
                    frag = TvShowLibraryFragment.newInstance(TvShowLoader.ALL_SHOWS);
                    break;
            }
        }
        
        ft.replace(R.id.content_frame, frag, tag);
        ft.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            int titleRes = R.string.all;
            if (type == MOVIES) titleRes = R.string.chooserMovies;
            else if (type == SHOWS) titleRes = R.string.chooserTVShows;
            getSupportActionBar().setTitle(titleRes);
        }
        
        // Clear search when switching tabs
        if (mSearchCard != null && mSearchCard.getVisibility() == View.VISIBLE) {
            toggleSearch(false);
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);

        if (newIntent.hasExtra("startup")) {
            int type = Integer.parseInt(newIntent.getStringExtra("startup"));
            int checkId = R.id.btn_all;
            if (type == MOVIES) checkId = R.id.btn_movies;
            else if (type == SHOWS) checkId = R.id.btn_tv;
            mLibraryToggleGroup.check(checkId);
            loadFragment(type);
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
        }

        // Handle grid size changes
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (id == R.id.cover_size_small) {
            editor.putString(GRID_ITEM_SIZE, "small").apply();
            return true;
        } else if (id == R.id.cover_size_normal) {
            editor.putString(GRID_ITEM_SIZE, "normal").apply();
            return true;
        } else if (id == R.id.cover_size_large) {
            editor.putString(GRID_ITEM_SIZE, "large").apply();
            return true;
        }

        // Pass actions to the current fragment
        Fragment frag = getSupportFragmentManager().findFragmentByTag("lib_frag" + selectedIndex);
        if (frag instanceof MovieLibraryFragment) {
            ((MovieLibraryFragment) frag).onMenuAction(id);
        } else if (frag instanceof TvShowLibraryFragment) {
            ((TvShowLibraryFragment) frag).onMenuAction(id);
        }

        return super.onOptionsItemSelected(item);
    }
}
