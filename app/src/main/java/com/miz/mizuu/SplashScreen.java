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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.miz.mizuu.fragments.ScheduledUpdatesFragment;
import com.miz.service.MovieLibraryUpdate;
import com.miz.service.TvShowsLibraryUpdate;

import java.util.ArrayList;
import java.util.List;

import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_MOVIE;
import static com.miz.functions.PreferenceKeys.SCHEDULED_UPDATES_TVSHOWS;

public class SplashScreen extends Activity {

	private static final int PERMISSION_REQUEST_CODE = 1000;
	private static final int MANAGE_STORAGE_REQUEST_CODE = 1001;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (checkPermissions()) {
			proceed();
		}
	}

	private boolean checkPermissions() {
		List<String> permissionsNeeded = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
			}
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
			}
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
			}
		} else {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}
		}

		if (!permissionsNeeded.isEmpty()) {
			ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
			return false;
		}

		// Handle MANAGE_EXTERNAL_STORAGE for Android 11+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (!Environment.isExternalStorageManager()) {
				try {
					Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					intent.addCategory("android.intent.category.DEFAULT");
					intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
					startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
				} catch (Exception e) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
					startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
				}
				return false;
			}
		}

		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSION_REQUEST_CODE) {
			// Even if some are denied, try to proceed or check MANAGE_EXTERNAL_STORAGE
			if (checkPermissions()) {
				proceed();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				if (Environment.isExternalStorageManager()) {
					proceed();
				} else {
					Toast.makeText(this, R.string.all_files_permission_required, Toast.LENGTH_LONG).show();
					proceed(); // Proceed anyway, though features might be limited
				}
			}
		}
	}

	private void proceed() {
		// Initialize the PreferenceManager variable and preference variable(s)
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		if (settings.getInt(SCHEDULED_UPDATES_MOVIE, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), MovieLibraryUpdate.class));

		if (settings.getInt(SCHEDULED_UPDATES_TVSHOWS, ScheduledUpdatesFragment.NOT_ENABLED) == ScheduledUpdatesFragment.AT_LAUNCH)
			getApplicationContext().startService(new Intent(getApplicationContext(), TvShowsLibraryUpdate.class));

		Intent i = new Intent();
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		i.setClass(getApplicationContext(), Main.class);

		startActivity(i);
		finish();
	}
}
