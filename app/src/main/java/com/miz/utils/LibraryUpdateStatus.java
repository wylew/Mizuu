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

package com.miz.utils;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Single source of truth for "is a library update running, and what did the last one change?".
 *
 * The update services write to it, and the UI (the banner in {@link com.miz.mizuu.Main}) reads it.
 * The latest state is kept in memory as well as broadcast, so a screen that opens while an update
 * is already running (e.g. an update started from the splash screen) can still show it.
 */
public final class LibraryUpdateStatus {

    public static final String ACTION_STATUS_CHANGED = "mizuu-library-update-status";

    public static final int TYPE_MOVIES = 0, TYPE_SHOWS = 1;

    public static final int STATE_IDLE = 0, STATE_RUNNING = 1, STATE_FINISHED = 2, STATE_CANCELLED = 3;

    /** Progress value meaning "we don't know how far along we are yet". */
    public static final int PROGRESS_UNKNOWN = -1;

    private static final long PROGRESS_THROTTLE_MS = 250;

    private static final Snapshot[] sSnapshots = { Snapshot.idle(TYPE_MOVIES), Snapshot.idle(TYPE_SHOWS) };
    private static final long[] sLastProgressBroadcast = new long[2];

    private LibraryUpdateStatus() {} // No instantiation

    /**
     * Immutable view of one library's update state.
     */
    public static final class Snapshot {
        public final int type, state, progress;
        public final String detail;
        /** For movies: movies. For TV: episodes. */
        public final int added, removed;
        /** For TV only: number of distinct shows that got new episodes. */
        public final int showsAdded;

        Snapshot(int type, int state, String detail, int progress, int added, int removed, int showsAdded) {
            this.type = type;
            this.state = state;
            this.detail = detail;
            this.progress = progress;
            this.added = added;
            this.removed = removed;
            this.showsAdded = showsAdded;
        }

        static Snapshot idle(int type) {
            return new Snapshot(type, STATE_IDLE, null, PROGRESS_UNKNOWN, 0, 0, 0);
        }

        public boolean isRunning() {
            return state == STATE_RUNNING;
        }

        public boolean isDone() {
            return state == STATE_FINISHED || state == STATE_CANCELLED;
        }

        public boolean hasChanges() {
            return added > 0 || removed > 0;
        }
    }

    public static synchronized Snapshot get(int type) {
        return sSnapshots[type];
    }

    public static boolean isAnyRunning() {
        return get(TYPE_MOVIES).isRunning() || get(TYPE_SHOWS).isRunning();
    }

    public static void started(Context context, int type) {
        set(context, new Snapshot(type, STATE_RUNNING, null, PROGRESS_UNKNOWN, 0, 0, 0), true);
    }

    /**
     * Report progress. Calls are throttled, so it's fine to call this for every file.
     *
     * @param progress 0-100, or {@link #PROGRESS_UNKNOWN}
     */
    public static void progress(Context context, int type, String detail, int progress, int added, int showsAdded) {
        boolean broadcast;
        synchronized (LibraryUpdateStatus.class) {
            long now = SystemClock.elapsedRealtime();
            broadcast = now - sLastProgressBroadcast[type] >= PROGRESS_THROTTLE_MS;
            if (broadcast)
                sLastProgressBroadcast[type] = now;
        }
        int clamped = progress == PROGRESS_UNKNOWN ? PROGRESS_UNKNOWN : Math.max(0, Math.min(100, progress));
        set(context, new Snapshot(type, STATE_RUNNING, detail, clamped, added, 0, showsAdded), broadcast);
    }

    public static void finished(Context context, int type, boolean cancelled, int added, int removed, int showsAdded) {
        finished(context, type, cancelled, added, removed, showsAdded, null);
    }

    /**
     * @param reason Optional explanation shown in the banner, e.g. why an update was cancelled.
     */
    public static void finished(Context context, int type, boolean cancelled, int added, int removed, int showsAdded, String reason) {
        set(context, new Snapshot(type, cancelled ? STATE_CANCELLED : STATE_FINISHED, reason, 100, added, removed, showsAdded), true);
    }

    /**
     * Called by the UI once the user has seen (or dismissed) the result of an update.
     * Running updates are left alone.
     */
    public static synchronized void acknowledge(int type) {
        if (sSnapshots[type].isDone())
            sSnapshots[type] = Snapshot.idle(type);
    }

    private static void set(Context context, Snapshot snapshot, boolean broadcast) {
        synchronized (LibraryUpdateStatus.class) {
            sSnapshots[snapshot.type] = snapshot;
        }
        if (broadcast && context != null) {
            LocalBroadcastManager.getInstance(context.getApplicationContext())
                    .sendBroadcast(new Intent(ACTION_STATUS_CHANGED));
        }
    }
}
