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

package com.miz.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import androidx.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.miz.functions.Actor;
import com.miz.functions.GridSeason;
import com.miz.functions.MizLib;
import com.miz.functions.WebMovie;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;
import com.miz.views.ObservableScrollView;
import com.squareup.picasso.Picasso;

import java.util.List;

import static com.miz.functions.PreferenceKeys.GRID_ITEM_SIZE;

public class ViewUtils {

    private ViewUtils() {} // No instantiation

    /**
     * Returns a actor card with name, character, image and click listener.
     */
    @SuppressLint("InflateParams")
    public static View setupActorCard(final Activity context, Picasso picasso, final Actor actor) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(actor.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.noactor).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(actor.getName());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(actor.getCharacter());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorIntent(context, actor));
            }
        });

        return v;
    }

    /**
     * Returns a movie card with title, release date, image and click listener.
     */
    @SuppressLint("InflateParams")
    public static View setupMovieCard(final Activity context, Picasso picasso, final WebMovie movie) {
        final View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(movie.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(movie.getTitle());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(movie.getSubtitle());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(context, v.findViewById(R.id.cover), "cover");
                ActivityCompat.startActivity(context, IntentUtils.getTmdbMovieDetails(context, movie), options.toBundle());
            }
        });

        return v;
    }

    /**
     * Returns a TV show card with title, release date, image and click listener.
     */
    @SuppressLint("InflateParams")
    public static View setupTvShowCard(final Context context, Picasso picasso, final WebMovie show) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(show.getUrl()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(show.getTitle());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(show.getSubtitle());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getTmdbTvShowLink(context, show));
            }
        });

        return v;
    }

    /**
     * Returns a TV show season card with title, release date, image and click listener.
     */
    @SuppressLint("InflateParams")
    public static View setupTvShowSeasonCard(final Activity context, Picasso picasso, final GridSeason season, final int toolbarColor) {
        final View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small, null);

        // Load image
        picasso.load(season.getCover()).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(
            MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set title
        ((TextView) v.findViewById(R.id.text)).setText(season.getHeaderText());
        ((TextView) v.findViewById(R.id.text)).setTypeface(TypefaceUtils.getRobotoMedium(context));

        // Set subtitle
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setText(season.getSimpleSubtitleText());
        ((TextView) v.findViewById(R.id.gridCoverSubtitle)).setSingleLine(true);

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivityForResult(IntentUtils.getTvShowSeasonIntent(context, season.getShowId(), season.getSeason(), season.getEpisodeCount(), toolbarColor), 0);
            }
        });

        return v;
    }

    /**
     * Returns a photo card.
     */
    @SuppressLint("InflateParams")
    public static View setupPhotoCard(final Activity context, Picasso picasso, final String url, final List<String> photos, final int index) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_no_text, null);

        // Load image
        picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorPhotoIntent(context, photos, index));
            }
        });

        return v;
    }

    /**
     * Returns a tagged photo card.
     */
    @SuppressLint("InflateParams")
    public static View setupTaggedPhotoCard(final Activity context, Picasso picasso, final String url, final List<String> photos, final int index) {
        View v = LayoutInflater.from(context).inflate(R.layout.horizontal_grid_item_small_no_text, null);

        // Load image
        picasso.load(url).placeholder(R.color.card_background_dark).error(R.drawable.loading_image).config(MizuuApplication.getBitmapConfig()).into(((ImageView) v.findViewById(R.id.cover)));

        // Set click listener
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(IntentUtils.getActorTaggedPhotoIntent(context, photos, index));
            }
        });

        return v;
    }

    /**
     * Animates the transition when changing the maxLines attribute of a TextView.
     */
    public static void animateTextViewMaxLines(TextView text, int maxLines) {
        try {
            ObjectAnimator animation = ObjectAnimator.ofInt(text, "maxLines", maxLines);
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setDuration(200);
            animation.start();
        } catch (Exception e) {
            text.setMaxLines(maxLines);
        }
    }

    public static void animateFabJump(View fab, Animator.AnimatorListener listener) {
        try {
            ObjectAnimator animation = ObjectAnimator.ofFloat(fab, "translationY", -10f, -5f, 0f, 5f, 10f, 5f, 0f, -5f, -10f, -5f, 0f);
            animation.setDuration(350);
            animation.addListener(listener);
            animation.start();
        } catch (Exception e) {}
    }

    private static int defaultTitleTextColor = -1;

    /**
     * Update the Toolbar background color and title.
     */
    public static void updateToolbarBackground(Activity activity, Toolbar toolbar,
                                               int alpha, String title, int color) {
        if (defaultTitleTextColor == -1) {
            try {
                TypedValue typedValue = new TypedValue();
                activity.getTheme().resolveAttribute(androidx.appcompat.R.attr.actionMenuTextColor, typedValue, true);
                int[] textColorAttr = new int[]{androidx.appcompat.R.attr.actionMenuTextColor};
                TypedArray a = activity.obtainStyledAttributes(typedValue.data, textColorAttr);
                defaultTitleTextColor = a.getColor(0, Color.WHITE);
                a.recycle();
            } catch (Exception e) {
                defaultTitleTextColor = Color.WHITE;
            }
        }
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(adjustAlpha(defaultTitleTextColor, alpha));
        int toolbarColor = adjustAlpha(color, alpha);
        toolbar.setBackgroundColor(toolbarColor);
    }

    public static int adjustAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static void setupWindowFlagsForStatusbarOverlay(Window window, boolean setBackgroundResource) {
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (setBackgroundResource)
            window.setBackgroundDrawableResource(R.drawable.bg);
    }

    public static void setProperToolbarSize(Context context, Toolbar toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = MizLib.getActionBarHeight(context) + insets.top;
            v.setLayoutParams(lp);
            return windowInsets;
        });
    }

    public static void setLayoutParamsForDetailsEmptyView(Context context, View layout, ImageView background, ObservableScrollView scrollView, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (!MizLib.isPortrait(context)) {
            View empty = layout.findViewById(R.id.empty_view);
            if (empty == null) return;
            int fullHeight = background.getHeight();
            int contentHeight = context.getResources().getDimensionPixelSize(R.dimen.content_details_main_height);
            empty.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullHeight - contentHeight));
        }
        MizLib.removeViewTreeObserver(scrollView.getViewTreeObserver(), listener);
    }

    public static void handleOnScrollChangedEvent(Activity activity, View layout, View background, String title, int height, int t, Toolbar toolbar, int toolbarColor) {
        final int headerHeight = (MizLib.isPortrait(activity) ? background.getHeight() : layout.findViewById(R.id.empty_view).getHeight()) - height;
        final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
        final int newAlpha = (int) (ratio * 255);

        updateToolbarBackground(activity, toolbar, newAlpha, title, toolbarColor);

        if (MizLib.isPortrait(activity)) {
            background.setPadding(0, (int) (t / 1.5), 0, 0);
        }
    }

    public static void setToolbarAndStatusBarColor(ActionBar actionBar, Window window, int color) {
        if (color != 0) {
            actionBar.setBackgroundDrawable(new ColorDrawable(color));
            window.setStatusBarColor(color);
        }
    }

    public static int getGridViewNumColumns(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String gridChoice = prefs.getString(GRID_ITEM_SIZE, "normal");
        
        if (gridChoice.equals("small")) {
            return 5;
        } else if (gridChoice.equals("large")) {
            return 3;
        } else {
            return 4; // normal
        }
    }

    public static int getGridViewThumbSize(Context context) {
        int numCols = getGridViewNumColumns(context);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;

        int spacing = context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        // Width = (TotalWidth - (numCols + 1) * spacing) / numCols
        return (screenWidth - (numCols + 1) * spacing) / numCols;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
