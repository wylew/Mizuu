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

import android.animation.Animator;
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
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.miz.apis.tmdb.TMDbTvShowService;
import com.miz.apis.trakt.Trakt;
import com.miz.base.MizActivity;
import com.miz.db.DbAdapterTvShowEpisodeMappings;
import com.miz.db.DbAdapterTvShowEpisodes;
import com.miz.db.DbAdapterTvShows;
import com.miz.functions.Actor;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.FileSource;
import com.miz.functions.Filepath;
import com.miz.functions.GridSeason;
import com.miz.functions.IntentKeys;
import com.miz.functions.MizLib;
import com.miz.functions.PaletteLoader;
import com.miz.functions.SimpleAnimatorListener;
import com.miz.mizuu.EditTvShow;
import com.miz.mizuu.IdentifyTvShow;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.mizuu.ShowCoverFanartBrowser;
import com.miz.mizuu.TvShow;
import com.miz.mizuu.TvShowEpisode;
import com.miz.utils.FileUtils;
import com.miz.utils.IntentUtils;
import com.miz.utils.LocalBroadcastUtils;
import com.miz.utils.TypefaceUtils;
import com.miz.utils.VideoUtils;
import com.miz.utils.ViewUtils;
import com.miz.views.HorizontalCardLayout;
import com.miz.views.ObservableScrollView;
import com.miz.views.ObservableScrollView.OnScrollChangedListener;
import com.squareup.otto.Bus;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TvShowDetailsFragment extends Fragment {

    private static final String LOG_TAG = "TvShowDetailsFragment";

    private Activity mContext;
    private DbAdapterTvShows dbHelper;
    private TvShow thisShow;
    private TextView textTitle, textPlot, textGenre, textRuntime, textReleaseDate, textRating, textCertification;
    private ImageView background, cover;
    private ObservableScrollView mScrollView;
    private View mDetailsArea, mTextContainerCard;
    private Picasso mPicasso;
    private Typeface mMediumItalic, mMedium, mBold, mCondensedRegular;
    private Bus mBus;
    private HorizontalCardLayout mSeasonsLayout, mActorsLayout;
    private int mImageThumbSize, mImageThumbSpacing, mToolbarColor = 0;
    private Toolbar mToolbar;
    private View mFab;
    private PaletteLoader mPaletteLoader;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public TvShowDetailsFragment() {}

    public static TvShowDetailsFragment newInstance(String showId) {
        TvShowDetailsFragment pageFragment = new TvShowDetailsFragment();
        Bundle b = new Bundle();
        b.putString("showId", showId);
        pageFragment.setArguments(b);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mContext = getActivity();
        mBus = MizuuApplication.getBus();

        mMediumItalic = TypefaceUtils.getRobotoMediumItalic(mContext);
        mMedium = TypefaceUtils.getRobotoMedium(mContext);
        mBold = TypefaceUtils.getRobotoBold(mContext);
        mCondensedRegular = TypefaceUtils.getRobotoCondensedRegular(mContext);

        // Create and open database
        dbHelper = MizuuApplication.getTvDbAdapter();

        mPicasso = MizuuApplication.getPicasso(getActivity());

        loadShowDetails();

        if (thisShow == null) {
            Log.e(LOG_TAG, "Could not load TV show with id: " + getArguments().getString("showId"));
            Toast.makeText(mContext, R.string.errorOccured, Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(LocalBroadcastUtils.CLEAR_IMAGE_CACHE));
    }

    private void loadShowDetails() {
        String showId = getArguments().getString("showId");
        if (TextUtils.isEmpty(showId)) return;

        Cursor cursor = dbHelper.getShow(showId);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                thisShow = new TvShow(getActivity(),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ID)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_TITLE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_PLOT)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RATING)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_GENRES)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_ACTORS)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_CERTIFICATION)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FIRST_AIRDATE)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_RUNTIME)),
                        cursor.getString(cursor.getColumnIndex(DbAdapterTvShows.KEY_SHOW_FAVOURITE)),
                        MizuuApplication.getTvEpisodeDbAdapter().getLatestEpisodeAirdate(showId)
                );
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to read TV show " + showId + " from the database", e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadImages();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mContext = getActivity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null)
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_and_tv_show_details, container, false);
    }

    public void onViewCreated(final View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (thisShow == null)
            return;

        mToolbar = (Toolbar) v.findViewById(R.id.toolbar);
        mToolbar.setBackgroundResource(android.R.color.transparent);
        ViewUtils.setProperToolbarSize(mContext, mToolbar);

        if (getActivity() instanceof MizActivity) {
            ((MizActivity) getActivity()).setSupportActionBar(mToolbar);
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.horizontal_grid_item_width);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mDetailsArea = v.findViewById(R.id.details_card);
        mTextContainerCard = v.findViewById(R.id.text_container_card);

        background = (ImageView) v.findViewById(R.id.imageBackground);
        textTitle = (TextView) v.findViewById(R.id.movieTitle);
        textPlot = (TextView) v.findViewById(R.id.textView2);
        textGenre = (TextView) v.findViewById(R.id.textView7);
        textRuntime = (TextView) v.findViewById(R.id.textView9);
        textReleaseDate = (TextView) v.findViewById(R.id.textReleaseDate);
        textRating = (TextView) v.findViewById(R.id.textView12);
        textCertification = (TextView) v.findViewById(R.id.textView11);
        cover = (ImageView) v.findViewById(R.id.traktIcon);
        mSeasonsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout);
        mActorsLayout = (HorizontalCardLayout) v.findViewById(R.id.horizontal_card_layout_extra);
        mActorsLayout.setVisibility(View.VISIBLE);
        mScrollView = (ObservableScrollView) v.findViewById(R.id.observableScrollView);
        mFab = v.findViewById(R.id.fab);

        mFab.setOnClickListener(v1 -> ViewUtils.animateFabJump(v1, new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                playFirstEpisode();
            }
        }));

        final int height = MizLib.getActionBarAndStatusBarHeight(mContext);

        mScrollView.setOnScrollChangedListener((who, l, t, oldl, oldt) -> ViewUtils.handleOnScrollChangedEvent(
                getActivity(), v, background, thisShow.getTitle(),
                height, t, mToolbar, mToolbarColor));
        
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.setLayoutParamsForDetailsEmptyView(mContext, v,
                        background, mScrollView, this);
            }
        });

        textTitle.setVisibility(View.VISIBLE);
        textTitle.setText(thisShow.getTitle());
        textTitle.setTypeface(mCondensedRegular);

        textPlot.setTypeface(mCondensedRegular);
        textRuntime.setTypeface(mMedium);
        textRating.setTypeface(mMedium);
        textCertification.setTypeface(mMedium);

        textPlot.setBackgroundResource(R.drawable.selectable_background);
        textPlot.setMaxLines(getActivity().getResources().getInteger(R.integer.show_details_max_lines));
        textPlot.setTag(true);
        textPlot.setOnClickListener(v12 -> {
            if (((Boolean) textPlot.getTag())) {
                ViewUtils.animateTextViewMaxLines(textPlot, 50);
                textPlot.setTag(false);
            } else {
                ViewUtils.animateTextViewMaxLines(textPlot, getResources().getInteger(R.integer.show_details_max_lines));
                textPlot.setTag(true);
            }
        });
        textPlot.setEllipsize(TextUtils.TruncateAt.END);
        textPlot.setFocusable(true);

        if (MizLib.isTablet(getActivity()))
            textPlot.setLineSpacing(0, 1.15f);

        textPlot.setText(thisShow.getDescription());

        textGenre.setTypeface(mMediumItalic);
        if (!TextUtils.isEmpty(thisShow.getGenres())) {
            textGenre.setText(thisShow.getGenres());
        } else {
            textGenre.setVisibility(View.GONE);
        }

        String runtime = thisShow.getRuntime();
        if (!TextUtils.isEmpty(runtime) && MizLib.isNumber(runtime)) {
            textRuntime.setText(MizLib.getPrettyRuntime(getActivity(), Integer.parseInt(runtime)));
        } else {
            textRuntime.setVisibility(View.GONE);
        }

        textReleaseDate.setTypeface(mMedium);
        textReleaseDate.setText(MizLib.getPrettyDate(getActivity(), thisShow.getFirstAirdate()));

        if (!thisShow.getRating().equals("0.0")) {
            try {
                int rating = (int) (Double.parseDouble(thisShow.getRating()) * 10);
                textRating.setText(Html.fromHtml(rating + "<small> %</small>"));
            } catch (Exception e) {
                textRating.setText(thisShow.getRating());
            }
        } else {
            textRating.setText(R.string.stringNA);
        }

        if (!TextUtils.isEmpty(thisShow.getCertification())) {
            textCertification.setText(thisShow.getCertification());
        } else {
            textCertification.setText(R.string.stringNA);
        }

        mSeasonsLayout.setTitle(R.string.seasons);
        mSeasonsLayout.setSeeMoreVisibility(true);
        mSeasonsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mSeasonsLayout.getWidth();
                        if (width > 0) {
                            int numColumns = getColumnCount(width);
                            loadSeasons(numColumns, getCardWidth(width, numColumns));
                            MizLib.removeViewTreeObserver(mSeasonsLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mSeasonsLayout.setSeeMoreOnClickListener(v13 -> startActivity(IntentUtils.getTvShowSeasonsIntent(mContext, thisShow.getTitle(), thisShow.getId(), mToolbarColor)));

        mActorsLayout.setTitle(R.string.detailsActors);
        mActorsLayout.setSeeMoreVisibility(true);
        mActorsLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = mActorsLayout.getWidth();
                        if (width > 0) {
                            int numColumns = getColumnCount(width);
                            loadActors(numColumns, getCardWidth(width, numColumns));
                            MizLib.removeViewTreeObserver(mActorsLayout.getViewTreeObserver(), this);
                        }
                    }
                });
        mActorsLayout.setSeeMoreOnClickListener(v14 -> startActivity(IntentUtils.getActorBrowserTvShows(mContext, thisShow.getTitle(), thisShow.getId(), mToolbarColor)));

        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, thisShow.getTitle(), mToolbarColor);

        loadImages();
    }

    private void loadImages() {
        if (thisShow == null) return;

        mPicasso.load(thisShow.getCoverPhoto()).error(R.drawable.loading_image).placeholder(R.drawable.loading_image).into(cover, new Callback() {
            @Override
            public void onSuccess() {
                if (mPaletteLoader == null) {
                    mPaletteLoader = new PaletteLoader(mPicasso, Uri.fromFile(thisShow.getCoverPhoto()), (swatchColor, accentColor) -> {
                        mToolbarColor = swatchColor;
                        mActorsLayout.setTitleColor(accentColor);
                        mSeasonsLayout.setTitleColor(accentColor);
                        ViewUtils.updateToolbarBackground(getActivity(), mToolbar, 0, thisShow.getTitle(), mToolbarColor);
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

        if (!MizLib.isPortrait(getActivity())) {
            mPicasso.load(new File(thisShow.getBackdrop())).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).error(R.drawable.bg).placeholder(R.drawable.bg).into(background);
        } else {
            mPicasso.load(new File(thisShow.getBackdrop())).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).placeholder(R.drawable.bg).into(background, new Callback() {
                @Override
                public void onError(Exception e) {
                    if (isAdded())
                        mPicasso.load(thisShow.getThumbnail()).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).placeholder(R.drawable.bg).error(R.drawable.bg).into(background);
                }
                @Override public void onSuccess() {}
            });
        }
    }

    /**
     * Number of cards that fit in a horizontal card row of the given width.
     */
    private int getColumnCount(int rowWidth) {
        int numColumns = rowWidth / Math.max(1, mImageThumbSize + mImageThumbSpacing);
        return Math.max(1, numColumns);
    }

    /**
     * Card width that fills a row of the given width with {@code numColumns} cards.
     * Computed per row, so the seasons and actors rows don't overwrite each other's size.
     */
    private int getCardWidth(int rowWidth, int numColumns) {
        return Math.max(1, (rowWidth - (numColumns * mImageThumbSpacing)) / numColumns);
    }

    private void loadActors(final int capacity, final int cardWidth) {
        final String showId = thisShow.getId();
        final android.content.Context appContext = mContext.getApplicationContext();
        new AsyncTask<Void, Void, List<Actor>>() {
            @Override
            protected List<Actor> doInBackground(Void... params) {
                try {
                    return TMDbTvShowService.getInstance(appContext).getActors(showId);
                } catch (Exception e) {
                    // Never let a network / parsing problem take down the whole app
                    Log.e(LOG_TAG, "Failed to load actors for show " + showId, e);
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<Actor> actors) {
                if (!isAdded() || mActorsLayout == null)
                    return;

                if (actors != null && !actors.isEmpty()) {
                    mActorsLayout.loadItems(mContext, mPicasso, capacity, cardWidth, actors, HorizontalCardLayout.ACTORS, mToolbarColor);
                } else {
                    mActorsLayout.setVisibility(View.GONE);
                }
            }
        }.execute();
    }

    private void loadSeasons(final int capacity, final int cardWidth) {
        final String showId = thisShow.getId();
        final android.content.Context appContext = mContext.getApplicationContext();
        new AsyncTask<Void, Void, List<GridSeason>>() {
            @Override
            protected List<GridSeason> doInBackground(Void... params) {
                List<GridSeason> seasonsList = new ArrayList<>();
                try {
                    HashMap<String, EpisodeCounter> seasons = MizuuApplication.getTvEpisodeDbAdapter().getSeasons(showId);

                    for (String key : seasons.keySet()) {
                        EpisodeCounter counter = seasons.get(key);
                        if (counter == null)
                            continue;

                        File temp = FileUtils.getTvShowSeason(appContext, showId, key);
                        int seasonNum = MizLib.getInteger(key);

                        seasonsList.add(new GridSeason(appContext, showId, seasonNum, counter.getEpisodeCount(), counter.getWatchedCount(),
                                temp.exists() ? temp : FileUtils.getTvShowThumb(appContext, showId)));
                    }
                    Collections.sort(seasonsList);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to load seasons for show " + showId, e);
                }
                return seasonsList;
            }

            @Override
            protected void onPostExecute(List<GridSeason> results) {
                if (!isAdded() || mSeasonsLayout == null)
                    return;

                if (results != null && !results.isEmpty()) {
                    mSeasonsLayout.loadItems(mContext, mPicasso, capacity, cardWidth, results, HorizontalCardLayout.SEASONS, mToolbarColor);
                    mSeasonsLayout.setSeeMoreVisibility(true);
                } else {
                    mSeasonsLayout.setVisibility(View.GONE);
                }
            }
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (thisShow == null) return;
        inflater.inflate(R.menu.tv_show_details, menu);

        if (MizLib.isTablet(mContext)) {
            MenuItem shareItem = menu.findItem(R.id.share);
            if (shareItem != null) shareItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        MenuItem favItem = menu.findItem(R.id.show_fav);
        if (favItem != null) {
            favItem.setIcon(thisShow.isFavorite() ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                    .setTitle(thisShow.isFavorite() ? R.string.menuFavouriteTitleRemove : R.string.menuFavouriteTitle);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.show_fav) {
            favAction();
        } else if (id == R.id.menuDeleteShow) {
            deleteShow();
        } else if (id == R.id.change_cover) {
            searchCover();
        } else if (id == R.id.identify_show) {
            identifyShow();
        } else if (id == R.id.editTvShow) {
            editTvShow();
        } else if (id == R.id.share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "http://www.themoviedb.org/tv/" + thisShow.getId());
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void identifyShow() {
        ArrayList<String> files = new ArrayList<>();
        Cursor cursor = MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getAllFilepaths(thisShow.getId());
        if (cursor != null) {
            while (cursor.moveToNext())
                files.add(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodeMappings.KEY_FILEPATH)));
            cursor.close();
        }

        Intent i = new Intent(mContext, IdentifyTvShow.class);
        i.putExtra("showTitle", thisShow.getTitle());
        i.putExtra("showId", thisShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(i, 0);
    }

    private void editTvShow() {
        Intent intent = new Intent(mContext, EditTvShow.class);
        intent.putExtra("showId", thisShow.getId());
        intent.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivityForResult(intent, 1);
    }

    public void favAction() {
        thisShow.setFavorite(!thisShow.isFavorite());
        if (dbHelper.updateShowSingleItem(thisShow.getId(), DbAdapterTvShows.KEY_SHOW_FAVOURITE, thisShow.getFavorite())) {
            getActivity().invalidateOptionsMenu();
            Toast.makeText(mContext, getString(thisShow.isFavorite() ? R.string.addedToFavs : R.string.removedFromFavs), Toast.LENGTH_SHORT).show();
            LocalBroadcastUtils.updateTvShowLibrary(mContext);
        } else Toast.makeText(mContext, getString(R.string.errorOccured), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            if (getActivity() != null) {
                ArrayList<TvShow> show = new ArrayList<>();
                show.add(thisShow);
                Trakt.tvShowFavorite(show, getActivity().getApplicationContext());
            }
        }).start();
    }

    private void searchCover() {
        Intent i = new Intent(mContext, ShowCoverFanartBrowser.class);
        i.putExtra("id", thisShow.getId());
        i.putExtra(IntentKeys.TOOLBAR_COLOR, mToolbarColor);
        startActivity(i);
    }

    private void deleteShow() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.areYouSure)
                .setTitle(R.string.removeShow)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    MizLib.deleteShow(getActivity().getApplicationContext(), thisShow, true);
                    LocalBroadcastUtils.updateTvShowLibrary(getActivity().getApplicationContext());
                    getActivity().finish();
                })
                .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.cancel())
                .show();
    }

    private void playFirstEpisode() {
        DbAdapterTvShowEpisodes dbAdapter = MizuuApplication.getTvEpisodeDbAdapter();
        Cursor cursor = dbAdapter.getEpisodes(thisShow.getId());
        TvShowEpisode episode = null;

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    if (MizLib.getInteger(cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON))) > 0) {
                        if (episode == null) {
                            episode = new TvShowEpisode(getActivity(),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                            );

                            episode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));
                        }

                        if (cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)).equals("0")) {
                            episode = new TvShowEpisode(getActivity(),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_TITLE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_PLOT)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_DIRECTOR)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_WRITER)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_GUESTSTARS)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE_RATING)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_HAS_WATCHED)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_FAVOURITE))
                            );

                            episode.setFilepaths(MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFilepathsForEpisode(
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SHOW_ID)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_SEASON)),
                                    cursor.getString(cursor.getColumnIndex(DbAdapterTvShowEpisodes.KEY_EPISODE))
                            ));
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {} finally {
                cursor.close();
            }

            if (episode != null) {
                play(episode);
                Toast.makeText(mContext, String.format(mContext.getString(R.string.playing_season_episode),
                        thisShow.getTitle(), episode.getSeason(), episode.getEpisode(), episode.getTitle()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, R.string.no_episodes_to_play, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void play(final TvShowEpisode episode) {
        ArrayList<Filepath> paths = episode.getFilepaths();
        if (paths.size() == 1) {
            Filepath path = paths.get(0);
            if (episode.hasOfflineCopy(path)) {
                if (VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode)) {
                    checkIn(episode);
                }
            } else {
                if (VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode)) {
                    checkIn(episode);
                }
            }
        } else {
            boolean hasOfflineCopy = false;
            for (Filepath path : paths) {
                if (episode.hasOfflineCopy(path)) {
                    if (VideoUtils.playVideo(getActivity(), episode.getOfflineCopyUri(path), FileSource.FILE, episode)) {
                        checkIn(episode);
                    }
                    hasOfflineCopy = true;
                    break;
                }
            }

            if (!hasOfflineCopy) {
                MizLib.showSelectFileDialog(getActivity(), episode.getFilepaths(), (dialog, which) -> {
                    Filepath path = episode.getFilepaths().get(which);
                    if (VideoUtils.playVideo(getActivity(), path.getFilepath(), path.getType(), episode)) {
                        checkIn(episode);
                    }
                });
            }
        }
    }

    private void checkIn(final TvShowEpisode episode) {
        new Thread(() -> {
            if (getActivity() != null)
                Trakt.performEpisodeCheckin(episode, getActivity());
        }).start();
    }

    public TvShow getShow() {
        return thisShow;
    }
}
