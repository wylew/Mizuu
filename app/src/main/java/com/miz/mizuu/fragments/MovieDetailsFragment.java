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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.miz.apis.tmdb.TMDbMovieService;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterMovies;
import com.miz.functions.Actor;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.functions.Movie;
import com.miz.functions.PaletteLoader;
import com.miz.mizuu.EditMovie;
import com.miz.mizuu.IdentifyMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.MovieCoverFanartBrowser;
import com.miz.mizuu.R;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.MovieDatabaseUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.miz.functions.PreferenceKeys.SHOW_FILE_LOCATION;

public class MovieDetailsFragment extends Fragment {

    private Activity mContext;
    private Movie mMovie;
    private DbAdapterMovies mDatabase;
    private TextView mTitle, mPlot, mSrc, mGenre, mRuntime, mReleaseDate, mRating, mTagline, mCertification;
    private View mDetailsArea, mTextContainerCard;
    private ObservableScrollView mScrollView;
    private HorizontalCardLayout mActorsLayout;
    private boolean mShowFileLocation;
    private ImageView mBackground, mCover;
    private Picasso mPicasso;
    private Typeface mLight, mMedium, mCondensedRegular;
    private int mToolbarColor = 0;
    private Toolbar mToolbar;
    private FloatingActionButton mFab;
    private int mImageThumbSize, mImageThumbSpacing;
    private PaletteLoader mPaletteLoader;

    public MovieDetailsFragment() {}

    public static MovieDetailsFragment newInstance(String tmdbId) {
        MovieDetailsFragment pageFragment = new MovieDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("tmdbId", tmdbId);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mContext = getActivity();
        mShowFileLocation = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(SHOW_FILE_LOCATION, true);

        mLight = TypefaceUtils.getRobotoLight(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);
        mPicasso = MizuuApplication.getPicasso(mContext);
        mDatabase = MizuuApplication.getMovieAdapter();

        Cursor cursor = mDatabase.fetchMovie(getArguments().getString("tmdbId"));
        try {
            if (cursor != null && cursor.moveToFirst()) {
                mMovie = new Movie(mContext,
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TAGLINE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TMDB_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_IMDB_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RELEASEDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_CERTIFICATION)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_RUNTIME)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TRAILER)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_GENRES)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_FAVOURITE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_ACTORS)),
                        MizuuApplication.getCollectionsAdapter().getCollection(cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID))),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_COLLECTION_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_TO_WATCH)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_HAS_WATCHED)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterMovies.KEY_DATE_ADDED))
                );
            }
        } catch (Exception e) {} finally {
            if (cursor != null) cursor.close();
        }

        if (mMovie == null) {
            getActivity().finish();
            return;
        }

        LocalBroadcastManager.getInstance(mContext).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadImages();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mContext != null)
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mMovie == null)
            return;

        mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);
        ((MizActivity) getActivity()).setSupportActionBar(mToolbar);

        mDetailsArea = view.findViewById(R.id.details_card);
        mTextContainerCard = view.findViewById(R.id.text_container_card);
        mBackground = view.findViewById(R.id.imageBackground);
        mTitle = view.findViewById(R.id.movieTitle);
        mPlot = view.findViewById(R.id.textView2);
        mSrc = view.findViewById(R.id.textView3);
        mGenre = view.findViewById(R.id.textView7);
        mRuntime = view.findViewById(R.id.textView9);
        mReleaseDate = view.findViewById(R.id.textReleaseDate);
        mRating = view.findViewById(R.id.textView12);
        mTagline = view.findViewById(R.id.textView6);
        mCertification = view.findViewById(R.id.textView11);
        mCover = view.findViewById(R.id.traktIcon);
        mActorsLayout = view.findViewById(R.id.horizontal_card_layout);
        mScrollView = view.findViewById(R.id.observableScrollView);
        mFab = view.findViewById(R.id.fab);

        mFab.setOnClickListener(v -> playMovie());

        final int height = MizLib.getActionBarAndStatusBarHeight(mContext);
        mScrollView.setOnScrollChangedListener((who, l, t, oldl, oldt) ->
                ViewUtils.handleOnScrollChangedEvent(getActivity(), view, mBackground, mMovie.getTitle(), height, t, mToolbar, mToolbarColor));

        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, view, mBackground, mScrollView, this);
            }
        });

        // UI Setup
        mTitle.setText(mMovie.getTitle());
        mGenre.setVisibility(View.GONE);

        String rawRuntime = mMovie.getRuntime().replaceAll("[^0-9]", "");
        if (!TextUtils.isEmpty(rawRuntime)) {
            mRuntime.setText(MizLib.getPrettyRuntime(mContext, Integer.parseInt(rawRuntime)));
        } else {
            mRuntime.setVisibility(View.GONE);
        }

        mReleaseDate.setText(mMovie.getReleasedate());
        mRating.setText(Html.fromHtml(mMovie.getRawRating() + "<small>/10</small>"));
        mTagline.setText(mMovie.getTagline());
        mCertification.setText(mMovie.getCertification());

        if (TextUtils.isEmpty(mMovie.getPlot())) {
            mPlot.setText("No plot available.");
        } else {
            mPlot.setText(mMovie.getPlot());
        }

        loadImages();

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mActorsLayout.setTitle(R.string.detailsActors);
        mActorsLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mActorsLayout.getWidth() > 0) {
                    int numColumns = (int) Math.floor(mActorsLayout.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                    if (numColumns <= 0) numColumns = 1;
                    mImageThumbSize = (mActorsLayout.getWidth() - (numColumns * mImageThumbSpacing)) / numColumns;

                    loadActors(numColumns);
                    MizLib.removeViewTreeObserver(mActorsLayout.getViewTreeObserver(), this);
                }
            }
        });
    }

    private void loadImages() {
        mPicasso.load(mMovie.getBackdrop()).into(mBackground);
        mPicasso.load(mMovie.getPoster()).into(mCover, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.fromFile(mMovie.getPoster()), (swatchColor, accentColor) -> {
                        mToolbarColor = swatchColor;
                        mActorsLayout.setTitleColor(accentColor);
                        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, mMovie.getTitle(), mToolbarColor);
                    });

                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mTextContainerCard);
                    mPaletteLoader.setFab(mFab);
                    mPaletteLoader.execute();
                } else {
                    mPaletteLoader.clearViews();
                    mPaletteLoader.addView(mDetailsArea);
                    mPaletteLoader.addView(mTextContainerCard);
                    mPaletteLoader.setFab(mFab);
                    mPaletteLoader.colorViews();
                }
            }

            @Override public void onError(Exception e) {}
        });
    }

    private void loadActors(final int capacity) {
        new AsyncTask<Void, Void, List<Actor>>() {
            @Override
            protected List<Actor> doInBackground(Void... params) {
                return TMDbMovieService.getInstance(mContext).getActors(mMovie.getTmdbId());
            }

            @Override
            protected void onPostExecute(List<Actor> actors) {
                if (isAdded() && actors != null && actors.size() > 0) {
                    mActorsLayout.loadItems(mContext, mPicasso, capacity, mImageThumbSize, actors, HorizontalCardLayout.ACTORS, mToolbarColor);
                } else {
                    mActorsLayout.setVisibility(View.GONE);
                }
            }
        }.execute();
    }

    public Movie getMovie() {
        return mMovie;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.movie_fav) {
            favAction();
        } else if (id == R.id.delete_movie) {
            deleteMovie();
        } else if (id == R.id.change_cover) {
            searchCover();
        } else if (id == R.id.identify) {
            identifyMovie();
        } else if (id == R.id.editMovie) {
            editMovie();
        } else if (id == R.id.share) {
            shareMovie();
        } else if (id == R.id.trailer) {
            VideoUtils.playTrailer(getActivity(), mMovie);
        }
        return super.onOptionsItemSelected(item);
    }

    private void identifyMovie() {
        Intent i = new Intent(mContext, IdentifyMovie.class);
        i.putExtra("fileName", mMovie.getFilepaths().get(0).getFullFilepath());
        i.putExtra("currentMovieId", mMovie.getTmdbId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(i, 0);
    }

    private void editMovie() {
        Intent i = new Intent(mContext, EditMovie.class);
        i.putExtra("movieId", mMovie.getTmdbId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(i, 1);
    }

    private void searchCover() {
        Intent i = new Intent(mContext, MovieCoverFanartBrowser.class);
        i.putExtra("tmdbId", mMovie.getTmdbId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivity(i);
    }

    private void shareMovie() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/movie/" + mMovie.getTmdbId());
        startActivity(intent);
    }

    public void favAction() {
        mMovie.setFavourite(!mMovie.isFavourite());
        if (mDatabase.updateMovieSingleItem(mMovie.getTmdbId(), DbAdapterMovies.KEY_FAVOURITE, mMovie.getFavourite())) {
            getActivity().invalidateOptionsMenu();
            Toast.makeText(mContext, getString(mMovie.isFavourite() ? R.string.addedToFavs : R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
            LocalBroadcastUtils.updateMovieLibrary(mContext);
        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            if (getActivity() != null) {
                ArrayList<Movie> movies = new ArrayList<>();
                movies.add(mMovie);
                Trakt.movieFavorite(movies, getActivity().getApplicationContext());
            }
        }).start();
    }

    private void deleteMovie() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.areYouSure)
                .setTitle(R.string.removeMovie)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    MovieDatabaseUtils.deleteMovie(mContext, mMovie.getTmdbId());
                    LocalBroadcastUtils.updateMovieLibrary(mContext);
                    getActivity().finish();
                })
                .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.cancel())
                .show();
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playMovie();
                break;
        }
    }

    private void playMovie() {
        ArrayList<Filepath> paths = mMovie.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (mMovie.hasOfflineCopy(path)) {
                VideoUtils.playVideo(mContext, mMovie.getOfflineCopyUri(path), FileSource.FILE, mMovie);
            } else {
                VideoUtils.playVideo(mContext, path.getFilepath(), path.getType(), mMovie);
            }
        } else if (paths.size() > 1) {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (mMovie.hasOfflineCopy(path)) {
                    VideoUtils.playVideo(mContext, mMovie.getOfflineCopyUri(path), FileSource.FILE, mMovie);
                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                MizLib.showSelectFileDialog(mContext, mMovie.getFilepaths(), new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Filepath path = mMovie.getFilepaths().get(which);
                        VideoUtils.playVideo(mContext, path.getFilepath(), path.getType(), mMovie);
                    }
                });
            }
        }
    }
}
