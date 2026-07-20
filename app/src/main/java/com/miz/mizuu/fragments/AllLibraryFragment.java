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

package com.miz.mizuu.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.miz.functions.CoverItem;
import com.miz.functions.MediumMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowDetails;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class AllLibraryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private LoaderAdapter mAdapter;
    private ObservableGridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles;
    private Picasso mPicasso;
    private Config mConfig;
    private List<Object> mFullResults = new ArrayList<>();
    private List<Object> mResults = new ArrayList<>();
    private String mSearchQuery = "";

    public AllLibraryFragment() {}

    public static AllLibraryFragment newInstance() {
        return new AllLibraryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mShowTitles = mSharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
        mPicasso = MizuuApplication.getPicasso(mContext);
        mConfig = MizuuApplication.getBitmapConfig();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        mGridView = (ObservableGridView) v.findViewById(R.id.gridView);
        mAdapter = new LoaderAdapter();
        mGridView.setAdapter(mAdapter);
        updateGridViewColumns();

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Object item = mAdapter.getItem(arg2);
                Intent intent = new Intent();
                if (item instanceof MediumMovie) {
                    intent.putExtra("tmdbId", ((MediumMovie) item).getTmdbId());
                    intent.setClass(getActivity(), MovieDetails.class);
                } else {
                    intent.putExtra("showId", ((TvShow) item).getId());
                    intent.setClass(getActivity(), TvShowDetails.class);
                }

                if (arg1 != null) {
                    Pair<View, String> pair = new Pair<>(arg1.findViewById(R.id.cover), "cover");
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), pair);
                    ActivityCompat.startActivityForResult(getActivity(), intent, 0, options.toBundle());
                } else {
                    startActivityForResult(intent, 0);
                }
            }
        });

        loadData();

        return v;
    }

    public void search(String query) {
        mSearchQuery = query;
        filterResults();
    }

    private void filterResults() {
        if (mSearchQuery.isEmpty()) {
            mResults = new ArrayList<>(mFullResults);
        } else {
            mResults = new ArrayList<>();
            for (Object obj : mFullResults) {
                String title = (obj instanceof MediumMovie) ? ((MediumMovie) obj).getTitle() : ((TvShow) obj).getTitle();
                if (title.toLowerCase().contains(mSearchQuery.toLowerCase())) {
                    mResults.add(obj);
                }
            }
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void loadData() {
        new AsyncTask<Void, Void, List<Object>>() {
            @Override
            protected void onPreExecute() {
                mProgressBar.setVisibility(View.VISIBLE);
                mGridView.setVisibility(View.GONE);
            }

            @Override
            protected List<Object> doInBackground(Void... voids) {
                List<Object> combined = new ArrayList<>();
                
                // Fetch movies
                combined.addAll(MizuuApplication.getMovieAdapter().listFromCursor(MizuuApplication.getMovieAdapter().getAllMovies()));
                
                // Fetch TV shows
                combined.addAll(MizuuApplication.getTvDbAdapter().listFromCursor(MizuuApplication.getTvDbAdapter().getAllShows()));

                Collections.sort(combined, new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        String t1 = (o1 instanceof MediumMovie) ? ((MediumMovie) o1).getTitle() : ((TvShow) o1).getTitle();
                        String t2 = (o2 instanceof MediumMovie) ? ((MediumMovie) o2).getTitle() : ((TvShow) o2).getTitle();
                        return t1.compareToIgnoreCase(t2);
                    }
                });

                return combined;
            }

            @Override
            protected void onPostExecute(List<Object> objects) {
                mFullResults = objects;
                filterResults();
                mProgressBar.setVisibility(View.GONE);
                mGridView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    private void updateGridViewColumns() {
        if (mGridView != null) {
            mGridView.setNumColumns(ViewUtils.getGridViewNumColumns(mContext));
            mGridView.setColumnWidth(ViewUtils.getGridViewThumbSize(mContext));
        }
    }

    private class LoaderAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mResults.size();
        }

        @Override
        public Object getItem(int position) {
            return mResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);
            CoverItem holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.grid_cover, parent, false);
                holder = new CoverItem();
                holder.cardview = (CardView) convertView.findViewById(R.id.card);
                holder.cover = (ImageView) convertView.findViewById(R.id.cover);
                holder.text = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(holder);
            } else {
                holder = (CoverItem) convertView.getTag();
            }

            String title = (item instanceof MediumMovie) ? ((MediumMovie) item).getTitle() : ((TvShow) item).getTitle();
            java.io.File thumb = (item instanceof MediumMovie) ? ((MediumMovie) item).getThumbnail() : ((TvShow) item).getThumbnail();

            if (!mShowTitles) {
                holder.text.setVisibility(View.GONE);
            } else {
                holder.text.setVisibility(View.VISIBLE);
                holder.text.setText(title);
            }

            holder.cover.setImageResource(R.color.card_background_dark);
            mPicasso.load(thumb).placeholder(R.drawable.bg).config(mConfig).into(holder.cover);

            return convertView;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(GRID_ITEM_SIZE)) {
            updateGridViewColumns();
            mAdapter.notifyDataSetChanged();
        } else if (key.equals(SHOW_TITLES_IN_GRID)) {
            mShowTitles = sharedPreferences.getBoolean(SHOW_TITLES_IN_GRID, true);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}
