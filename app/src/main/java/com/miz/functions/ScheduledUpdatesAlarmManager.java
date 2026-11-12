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

package com.miz.functions;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;

import static com.miz.functions.PreferenceKeys.NEXT_SCHEDULED_MOVIE_UPDATE;
import static com.miz.functions.PreferenceKeys.NEXT_SCHEDULED_TVSHOWS_UPDATE;

public class ScheduledUpdatesAlarmManager {

	public static final int MOVIES = 1, SHOWS = 2;

	public static void cancelUpdate(int type, Context context) {
		try {
			AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

			PendingIntent piWakeUp = getUpdatePendingIntent(context, type, PendingIntent.FLAG_NO_CREATE);

			if(piWakeUp != null)
				alarmMan.cancel(piWakeUp);
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			Editor editor = settings.edit();
			editor.putLong((type == MOVIES) ? NEXT_SCHEDULED_MOVIE_UPDATE : NEXT_SCHEDULED_TVSHOWS_UPDATE, 0);
			editor.apply();
		} catch (Exception ignored) {}
	}

	public static void startUpdate(int type, Context context, long when) {
		AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		if (type == MOVIES) {
			// We don't want multiple instances of the movie library service to run at the same time
			if (MizLib.isMovieLibraryBeingUpdated(context))
				return;
		} else {
			// We don't want multiple instances of the TV show library service to run at the same time
			if (MizLib.isTvShowLibraryBeingUpdated(context))
				return;
		}
		PendingIntent piWakeUp = getUpdatePendingIntent(context, type, PendingIntent.FLAG_UPDATE_CURRENT);

		if (when > -1) {
			alarmMan.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + when, piWakeUp);
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			Editor editor = settings.edit();
			editor.putLong((type == MOVIES) ? NEXT_SCHEDULED_MOVIE_UPDATE : NEXT_SCHEDULED_TVSHOWS_UPDATE, System.currentTimeMillis() + when);
			editor.apply();
		}
	}

	/**
	 * Builds the PendingIntent that starts a library update service.
	 *
	 * - On Android 12+ every PendingIntent must say whether it's mutable; without FLAG_IMMUTABLE
	 *   this throws IllegalArgumentException (it used to crash at the end of every library update
	 *   when scheduled updates were enabled, because the services reschedule from onDestroy()).
	 * - On Android 8+ an alarm can't start a regular background service, so it has to be started
	 *   as a foreground service (the update services call startForeground() straight away).
	 */
	private static PendingIntent getUpdatePendingIntent(Context context, int type, int flags) {
		Intent intent = new Intent(context, (type == MOVIES) ? MovieLibraryUpdate.class : TvShowsLibraryUpdate.class);
		int requestCode = type; // MOVIES = 1, SHOWS = 2
		flags |= PendingIntent.FLAG_IMMUTABLE;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			return PendingIntent.getForegroundService(context, requestCode, intent, flags);
		return PendingIntent.getService(context, requestCode, intent, flags);
	}
}
