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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.MovieDetailsFragment;
import com.miz.utils.ViewUtils;

public class MovieDetails extends MizActivity {

    private static String TAG = "MovieDetailsFragment";
    private String mMovieId;
    private MaterialButton mBackButton, mMenuButton;
    private View mBottomControls;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_details;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ViewUtils.setupWindowFlagsForStatusbarOverlay(getWindow(), true);

        mBottomControls = findViewById(R.id.bottom_controls);
        mBackButton = findViewById(R.id.fab_back);
        mMenuButton = findViewById(R.id.fab_menu);

        mBackButton.setOnClickListener(v -> onBackPressed());

        mMenuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.movie_details, popup.getMenu());
            
            // Sync favorite state
            Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
            if (frag instanceof MovieDetailsFragment) {
                MovieDetailsFragment detailsFrag = (MovieDetailsFragment) frag;
                MenuItem favItem = popup.getMenu().findItem(R.id.movie_fav);
                if (favItem != null && detailsFrag.getMovie() != null) {
                    boolean isFav = detailsFrag.getMovie().isFavourite();
                    favItem.setIcon(isFav ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp);
                    favItem.setTitle(isFav ? R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);
                }
            }
            
            popup.setOnMenuItemClickListener(item -> {
                Fragment f = getSupportFragmentManager().findFragmentByTag(TAG);
                if (f != null) {
                    return f.onOptionsItemSelected(item);
                }
                return false;
            });
            popup.show();
        });

        ViewCompat.setOnApplyWindowInsetsListener(mBottomControls, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom + 16);
            return windowInsets;
        });

        setTitle(null);

        // Fetch the database ID of the movie to view
        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            mMovieId = getIntent().getStringExtra(SearchManager.EXTRA_DATA_KEY);
        } else if (getIntent().getExtras() != null) {
            mMovieId = getIntent().getExtras().getString("tmdbId");
        }

        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, MovieDetailsFragment.newInstance(mMovieId), TAG);
            ft.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 4) {
            Toast.makeText(this, getString(R.string.updatedMovie), Toast.LENGTH_SHORT).show();

            // Create a new Intent with the Bundle
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), MovieDetails.class);
            intent.putExtra("tmdbId", mMovieId);

            // Start the Intent for result
            startActivity(intent);

            finish();
        }
    }

    @Override 
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
                if (frag instanceof MovieDetailsFragment) {
                    ((MovieDetailsFragment) frag).onKeyDown(keyCode, event);
                }
        }
        return super.onKeyDown(keyCode, event);
    }
}
