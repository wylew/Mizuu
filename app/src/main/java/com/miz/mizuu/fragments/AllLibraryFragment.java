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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.github.ksoichiro.android.observablescrollview.ObservableGridView;
import com.miz.functions.AsyncTask;
import com.miz.functions.CoverItem;
import com.miz.functions.MediumMovie;
import com.miz.functions.MizLib;
import com.miz.mizuu.AddFileSource;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieDetails;
import com.miz.mizuu.R;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowDetails;
import com.miz.mizuu.Update;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.ViewUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;
import static com.miz.functions.PreferenceKeys.SHOW_TITLES_IN_GRID;

public class AllLibraryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private LoaderAdapter mAdapter;
    private ObservableGridView mGridView;
    private ProgressBar mProgressBar;
    private boolean mShowTitles, mLoading = true;
    private Picasso mPicasso;
    private Config mConfig;
    private List<Object> mFullResults = new ArrayList<>();
    private List<Object> mResults = new ArrayList<>();
    private String mSearchQuery = "";
    private View mEmptyLibraryLayout;
    private TextView mEmptyLibraryTitle, mEmptyLibraryDescription;
    private Button mAddFileSourceButton, mUpdateLibraryButton;

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

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_MOVIE_LIBRARY));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.UPDATE_TV_SHOW_LIBRARY));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadData();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progress);
        mGridView = (ObservableGridView) v.findViewById(R.id.gridView);
        
        mEmptyLibraryLayout = v.findViewById(R.id.empty_library_layout);
        mEmptyLibraryTitle = (TextView) v.findViewById(R.id.empty_library_title);
        mEmptyLibraryTitle.setTypeface(TypefaceUtils.getRobotoCondensedRegular(mContext));
        mEmptyLibraryDescription = (TextView) v.findViewById(R.id.empty_library_description);
        mEmptyLibraryDescription.setTypeface(TypefaceUtils.getRobotoLight(mContext));

        mAddFileSourceButton = (Button) v.findViewById(R.id.add_file_source_button);
        mAddFileSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), AddFileSource.class);
                i.putExtra("isMovie", true);
                startActivity(i);
            }
        });

        mUpdateLibraryButton = (Button) v.findViewById(R.id.update_library_button);
        mUpdateLibraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMenuAction(R.id.update);
            }
        });

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
            String query = mSearchQuery.toLowerCase(Locale.getDefault());
            for (Object obj : mFullResults) {
                String title = (obj instanceof MediumMovie) ? ((MediumMovie) obj).getTitle() : ((TvShow) obj).getTitle();
                if (title.toLowerCase(Locale.getDefault()).contains(query)) {
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
                showProgressBar();
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
                mFullResults = (objects != null) ? objects : new ArrayList<>();
                mLoading = false;
                filterResults();
                hideProgressBar();
            }
        }.execute();
    }

    private void showProgressBar() {
        mGridView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mLoading = true;
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
        mLoading = false;
        
        if (mAdapter.getCount() == 0) {
            showEmptyView();
        } else {
            hideEmptyView();
            mGridView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyView() {
        mGridView.setVisibility(View.GONE);
        mEmptyLibraryLayout.setVisibility(View.VISIBLE);

        if (!mSearchQuery.isEmpty()) {
            mEmptyLibraryTitle.setText(R.string.no_search_results);
            mEmptyLibraryDescription.setText(R.string.no_search_results_description);
            mAddFileSourceButton.setVisibility(View.GONE);
            mUpdateLibraryButton.setVisibility(View.GONE);
        } else {
            mEmptyLibraryTitle.setText(R.string.no_content);
            mEmptyLibraryDescription.setText(MizLib.isTablet(mContext) ?
                    R.string.no_movies_description_tablet : R.string.no_movies_description);
            
            if (MizLib.getFileSources(MizLib.TYPE_MOVIE, false).isEmpty() && 
                MizLib.getFileSources(MizLib.TYPE_SHOWS, false).isEmpty()) {
                mAddFileSourceButton.setVisibility(View.VISIBLE);
                mUpdateLibraryButton.setVisibility(View.GONE);
            } else {
                mAddFileSourceButton.setVisibility(View.GONE);
                mUpdateLibraryButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideEmptyView() {
        mEmptyLibraryLayout.setVisibility(View.GONE);
    }

    private void updateGridViewColumns() {
        if (mGridView != null) {
            mGridView.setNumColumns(ViewUtils.getGridViewNumColumns(mContext));
            mGridView.setColumnWidth(ViewUtils.getGridViewThumbSize(mContext));
        }
    }

    public void onMenuAction(int id) {
        if (id == R.id.update) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), Update.class);
            intent.putExtra("isAll", true);
            startActivityForResult(intent, 0);
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
            mPicasso.load(thumb).placeholder(R.drawable.bg).config(mConfig).into(holder);

            return convertView;
        }
        
        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            
            if (!mLoading) {
                if (getCount() == 0) {
                    showEmptyView();
                } else {
                    hideEmptyView();
                    mGridView.setVisibility(View.VISIBLE);
                }
            }
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
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}
