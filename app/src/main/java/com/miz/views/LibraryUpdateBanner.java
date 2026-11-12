/*
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

package com.miz.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.miz.mizuu.R;
import com.miz.utils.LibraryUpdateStatus;
import com.miz.utils.LibraryUpdateStatus.Snapshot;

/**
 * Drives the library update banner (layout/library_update_banner.xml).
 *
 * While an update runs it shows what's happening with a progress bar. When the update finishes
 * it shows a summary of what changed (added / removed) and hides itself after a few seconds.
 * Movies and TV shows can update at the same time, so the banner combines both.
 *
 * Call {@link #start()} from onStart() and {@link #stop()} from onStop().
 */
public class LibraryUpdateBanner {

    private static final long AUTO_HIDE_DELAY_MS = 6000;
    private static final long ANIMATION_DURATION_MS = 220;

    private final Context mContext;
    private final View mBanner;
    private final ImageView mIcon;
    private final TextView mTitle, mSubtitle;
    private final LinearProgressIndicator mProgress;

    private ObjectAnimator mSpinAnimator;
    private boolean mStarted;

    /** The user closed the banner mid-update: stay hidden until there's a result to show. */
    private boolean mDismissedWhileRunning;

    private final Runnable mAutoHide = this::dismiss;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            render();
        }
    };

    public LibraryUpdateBanner(@NonNull View banner) {
        mContext = banner.getContext();
        mBanner = banner;
        mIcon = banner.findViewById(R.id.library_update_icon);
        mTitle = banner.findViewById(R.id.library_update_title);
        mSubtitle = banner.findViewById(R.id.library_update_subtitle);
        mProgress = banner.findViewById(R.id.library_update_progress);

        MaterialButton dismiss = banner.findViewById(R.id.library_update_dismiss);
        dismiss.setOnClickListener(v -> dismiss());
    }

    public void start() {
        if (mStarted)
            return;
        mStarted = true;
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver,
                new IntentFilter(LibraryUpdateStatus.ACTION_STATUS_CHANGED));

        // An update may have started (or finished) while we weren't listening
        render();
    }

    public void stop() {
        if (!mStarted)
            return;
        mStarted = false;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        mBanner.removeCallbacks(mAutoHide);
        stopSpinning();
    }

    /**
     * Hide the banner. Finished results are marked as seen; running updates keep running
     * (the system notification still shows them), and the banner comes back for the result.
     */
    public void dismiss() {
        mBanner.removeCallbacks(mAutoHide);
        LibraryUpdateStatus.acknowledge(LibraryUpdateStatus.TYPE_MOVIES);
        LibraryUpdateStatus.acknowledge(LibraryUpdateStatus.TYPE_SHOWS);
        mDismissedWhileRunning = LibraryUpdateStatus.isAnyRunning();
        hide();
    }

    private void render() {
        Snapshot movies = LibraryUpdateStatus.get(LibraryUpdateStatus.TYPE_MOVIES);
        Snapshot shows = LibraryUpdateStatus.get(LibraryUpdateStatus.TYPE_SHOWS);

        if (movies.isRunning() || shows.isRunning()) {
            if (!mDismissedWhileRunning)
                renderRunning(movies, shows);
        } else if (movies.isDone() || shows.isDone()) {
            mDismissedWhileRunning = false;
            renderFinished(movies, shows);
        } else {
            mDismissedWhileRunning = false;
            hide();
        }
    }

    private void renderRunning(Snapshot movies, Snapshot shows) {
        mBanner.removeCallbacks(mAutoHide);

        boolean both = movies.isRunning() && shows.isRunning();
        Snapshot active = both
                ? (shows.detail != null ? shows : movies) // Prefer whichever has something to say
                : (movies.isRunning() ? movies : shows);

        mIcon.setImageResource(R.drawable.ic_sync_white_24dp);
        startSpinning();

        mTitle.setText(both ? R.string.library_update_running_both
                : movies.isRunning() ? R.string.library_update_running_movies
                : R.string.library_update_running_shows);

        mSubtitle.setText(!TextUtils.isEmpty(active.detail) ? active.detail
                : mContext.getString(R.string.library_update_preparing));
        mSubtitle.setVisibility(View.VISIBLE);

        mProgress.setVisibility(View.VISIBLE);
        if (both || active.progress == LibraryUpdateStatus.PROGRESS_UNKNOWN) {
            if (!mProgress.isIndeterminate()) {
                // Must be hidden to switch modes on older Material versions
                mProgress.setVisibility(View.INVISIBLE);
                mProgress.setIndeterminate(true);
                mProgress.setVisibility(View.VISIBLE);
            }
        } else {
            mProgress.setIndeterminate(false);
            mProgress.setProgressCompat(active.progress, true);
        }

        show();
    }

    private void renderFinished(Snapshot movies, Snapshot shows) {
        stopSpinning();
        mProgress.setVisibility(View.GONE);

        boolean cancelled = movies.state == LibraryUpdateStatus.STATE_CANCELLED
                || shows.state == LibraryUpdateStatus.STATE_CANCELLED;

        mIcon.setImageResource(cancelled ? R.drawable.ic_cancel_white_24dp : R.drawable.ic_done_white_24dp);
        mTitle.setText(cancelled ? R.string.library_update_cancelled : R.string.library_update_finished);

        String summary = buildSummary(movies, shows);
        String reason = movies.isDone() && !TextUtils.isEmpty(movies.detail) ? movies.detail
                : shows.isDone() && !TextUtils.isEmpty(shows.detail) ? shows.detail : null;
        if (reason != null)
            summary = TextUtils.isEmpty(summary) ? reason : reason + " · " + summary;
        if (TextUtils.isEmpty(summary))
            summary = mContext.getString(R.string.library_update_no_changes);

        mSubtitle.setText(summary);
        mSubtitle.setVisibility(View.VISIBLE);

        show();

        mBanner.removeCallbacks(mAutoHide);
        mBanner.postDelayed(mAutoHide, AUTO_HIDE_DELAY_MS);
    }

    /**
     * E.g. "Movies: added 2 movies · TV: added 5 episodes (2 shows), removed 1 episode".
     * Returns an empty string if nothing changed.
     */
    private String buildSummary(Snapshot movies, Snapshot shows) {
        Resources res = mContext.getResources();
        String movieText = movies.isDone() && movies.hasChanges()
                ? describe(res, movies.added, movies.removed, R.plurals.moviesInLibrary, 0) : null;
        String showText = shows.isDone() && shows.hasChanges()
                ? describe(res, shows.added, shows.removed, R.plurals.episodes, shows.showsAdded) : null;

        if (movieText != null && showText != null) {
            return mContext.getString(R.string.library_update_section_movies) + ": " + movieText + " · "
                    + mContext.getString(R.string.library_update_section_shows) + ": " + showText;
        }
        return movieText != null ? movieText : showText != null ? showText : "";
    }

    private String describe(Resources res, int added, int removed, int pluralRes, int showsAdded) {
        StringBuilder sb = new StringBuilder();
        if (added > 0) {
            sb.append(mContext.getString(R.string.library_update_added, res.getQuantityString(pluralRes, added, added)));
            if (showsAdded > 0)
                sb.append(" (").append(res.getQuantityString(R.plurals.showsInLibrary, showsAdded, showsAdded)).append(")");
        }
        if (removed > 0) {
            String removedText = mContext.getString(R.string.library_update_removed, res.getQuantityString(pluralRes, removed, removed));
            if (sb.length() > 0) {
                sb.append(", ").append(removedText);
            } else {
                // Capitalise when it starts the sentence ("Removed 2 episodes")
                sb.append(Character.toUpperCase(removedText.charAt(0))).append(removedText.substring(1));
            }
        }
        return sb.toString();
    }

    private void show() {
        if (mBanner.getVisibility() == View.VISIBLE && mBanner.getAlpha() == 1f)
            return;
        mBanner.animate().cancel();
        if (mBanner.getVisibility() != View.VISIBLE) {
            mBanner.setAlpha(0f);
            mBanner.setTranslationY(mBanner.getResources().getDisplayMetrics().density * 24);
            mBanner.setVisibility(View.VISIBLE);
        }
        mBanner.animate().alpha(1f).translationY(0f).setDuration(ANIMATION_DURATION_MS).start();
    }

    private void hide() {
        stopSpinning();
        if (mBanner.getVisibility() != View.VISIBLE)
            return;
        mBanner.animate().cancel();
        mBanner.animate()
                .alpha(0f)
                .translationY(mBanner.getResources().getDisplayMetrics().density * 24)
                .setDuration(ANIMATION_DURATION_MS)
                .withEndAction(() -> mBanner.setVisibility(View.GONE))
                .start();
    }

    private void startSpinning() {
        if (mSpinAnimator != null && mSpinAnimator.isRunning())
            return;
        mSpinAnimator = ObjectAnimator.ofFloat(mIcon, View.ROTATION, 0f, -360f);
        mSpinAnimator.setDuration(1200);
        mSpinAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mSpinAnimator.setInterpolator(new LinearInterpolator());
        mSpinAnimator.start();
    }

    private void stopSpinning() {
        if (mSpinAnimator != null) {
            mSpinAnimator.cancel();
            mSpinAnimator = null;
        }
        mIcon.setRotation(0f);
    }
}
