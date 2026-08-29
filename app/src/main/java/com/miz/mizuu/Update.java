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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.miz.base.MizActivity;
import com.miz.functions.MizLib;
import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;
import com.miz.utils.TypefaceUtils;

import static com.miz.functions.PreferenceKeys.CLEAR_LIBRARY_MOVIES;
import static com.miz.functions.PreferenceKeys.CLEAR_LIBRARY_TVSHOWS;
import static com.miz.functions.PreferenceKeys.REMOVE_UNAVAILABLE_FILES_MOVIES;
import static com.miz.functions.PreferenceKeys.REMOVE_UNAVAILABLE_FILES_TVSHOWS;

public class Update extends MizActivity {

    private Button mFileSourcesButton, mUpdateLibraryButton;
    private TextView mFileSourcesDescription, mUpdateLibraryDescription;
    private CheckBox mClearLibrary, mRemoveUnavailableFiles;
    private Editor mEditor;
    private SharedPreferences mSharedPreferences;
    private boolean mIsMovie, mIsAll;
    private Typeface mTypeface;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mIsMovie = extras != null && extras.getBoolean("isMovie", true);
        mIsAll = extras != null && extras.getBoolean("isAll", false);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mIsAll) {
            mToolbar.setTitle(getString(R.string.menuUpdateTitle));
        } else if (!mIsMovie) {
            mToolbar.setTitle(getString(R.string.updateTvShowsTitle));
        } else {
            mToolbar.setTitle(getString(R.string.updateMoviesTitle));
        }

        setSupportActionBar(mToolbar);

        mTypeface = TypefaceUtils.getRobotoLight(this);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mClearLibrary = (CheckBox) findViewById(R.id.checkBox);
        mClearLibrary.setTypeface(mTypeface);
        mClearLibrary.setChecked(false);
        mClearLibrary.setOnCheckedChangeListener(getOnCheckedChangeListener(true));

        mRemoveUnavailableFiles = (CheckBox) findViewById(R.id.checkBox2);
        mRemoveUnavailableFiles.setTypeface(mTypeface);
        mRemoveUnavailableFiles.setChecked(false);
        mRemoveUnavailableFiles.setOnCheckedChangeListener(getOnCheckedChangeListener(false));

        mFileSourcesDescription = (TextView) findViewById(R.id.file_sources_description);
        mFileSourcesDescription.setTypeface(mTypeface);
        mUpdateLibraryDescription = (TextView) findViewById(R.id.update_library_description);
        mUpdateLibraryDescription.setTypeface(mTypeface);

        mFileSourcesButton = (Button) findViewById(R.id.select_file_sources_button);
        mFileSourcesButton.setTypeface(mTypeface);
        mUpdateLibraryButton = (Button) findViewById(R.id.start_update_button);
        mUpdateLibraryButton.setTypeface(mTypeface);

        if (mIsAll) {
            mFileSourcesButton.setVisibility(View.GONE);
            mFileSourcesDescription.setVisibility(View.GONE);
            View rowTitle = findViewById(R.id.row_title_movies_online);
            if (rowTitle != null) {
                rowTitle.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.update_layout;
    }

    private OnCheckedChangeListener getOnCheckedChangeListener(final boolean clearLibrary) {
        return new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mEditor = mSharedPreferences.edit();
                if (mIsAll) {
                    if (clearLibrary) {
                        mEditor.putBoolean(CLEAR_LIBRARY_MOVIES, isChecked);
                        mEditor.putBoolean(CLEAR_LIBRARY_TVSHOWS, isChecked);
                    } else {
                        mEditor.putBoolean(REMOVE_UNAVAILABLE_FILES_MOVIES, isChecked);
                        mEditor.putBoolean(REMOVE_UNAVAILABLE_FILES_TVSHOWS, isChecked);
                    }
                } else {
                    if (clearLibrary)
                        mEditor.putBoolean(mIsMovie ? CLEAR_LIBRARY_MOVIES : CLEAR_LIBRARY_TVSHOWS, isChecked);
                    else
                        mEditor.putBoolean(mIsMovie ? REMOVE_UNAVAILABLE_FILES_MOVIES : REMOVE_UNAVAILABLE_FILES_TVSHOWS, isChecked);
                }
                mEditor.apply();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsAll) {
            mClearLibrary.setChecked(mSharedPreferences.getBoolean(CLEAR_LIBRARY_MOVIES, false) && mSharedPreferences.getBoolean(CLEAR_LIBRARY_TVSHOWS, false));
            mRemoveUnavailableFiles.setChecked(mSharedPreferences.getBoolean(REMOVE_UNAVAILABLE_FILES_MOVIES, false) && mSharedPreferences.getBoolean(REMOVE_UNAVAILABLE_FILES_TVSHOWS, false));
        } else {
            mClearLibrary.setChecked(mSharedPreferences.getBoolean(mIsMovie ? CLEAR_LIBRARY_MOVIES : CLEAR_LIBRARY_TVSHOWS, false));
            mRemoveUnavailableFiles.setChecked(mSharedPreferences.getBoolean(mIsMovie ? REMOVE_UNAVAILABLE_FILES_MOVIES : REMOVE_UNAVAILABLE_FILES_TVSHOWS, false));
        }
    }

    public void startUpdate(View v) {
        if (mIsAll) {
            if (!MizLib.isMovieLibraryBeingUpdated(this))
                getApplicationContext().startService(new Intent(getApplicationContext(), MovieLibraryUpdate.class));
            if (!MizLib.isTvShowLibraryBeingUpdated(this))
                getApplicationContext().startService(new Intent(getApplicationContext(), TvShowsLibraryUpdate.class));
        } else {
            if (mIsMovie && !MizLib.isMovieLibraryBeingUpdated(this))
                getApplicationContext().startService(new Intent(getApplicationContext(), MovieLibraryUpdate.class));
            else if (!mIsMovie && !MizLib.isTvShowLibraryBeingUpdated(this))
                getApplicationContext().startService(new Intent(getApplicationContext(), TvShowsLibraryUpdate.class));
        }
        setResult(1); // end activity and reload Main activity

        finish(); // Leave the Update screen once the update has been started
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    public void selectSources(View v) {
        Intent intent = new Intent(this, FileSources.class);
        intent.putExtra("fromUpdate", true);
        intent.putExtra("isMovie", mIsMovie);
        intent.putExtra("completeScan", mClearLibrary.isChecked());
        startActivity(intent);
    }
}
