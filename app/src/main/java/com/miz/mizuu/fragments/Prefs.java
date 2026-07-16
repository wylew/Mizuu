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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.miz.mizuu.R;
import com.miz.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import static com.miz.functions.PreferenceKeys.LANGUAGE_PREFERENCE;

public class Prefs extends PreferenceFragmentCompat {

	private Locale[] mSystemLocales;

	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		String resource = "root_preferences";
		if (getArguments() != null && getArguments().containsKey("resource")) {
			resource = getArguments().getString("resource");
		}
		
		int res = requireContext().getResources().getIdentifier(resource, "xml", requireContext().getPackageName());
		if (res > 0) {
			setPreferencesFromResource(res, rootKey);
		} else {
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
		}

		Preference copyDatabase = findPreference("prefsCopyDatabase");
		if (copyDatabase != null)
			copyDatabase.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					String path = FileUtils.copyDatabase(requireActivity());

					if (!TextUtils.isEmpty(path)) {
						Toast.makeText(requireActivity(), getString(R.string.database_copied) + "\n(" + path + ")", Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(requireActivity(), R.string.errorSomethingWentWrong, Toast.LENGTH_SHORT).show();
					}

					return true;
				}
			});

		Preference languagePref = findPreference(LANGUAGE_PREFERENCE);
		if (languagePref != null)
			languagePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {

					mSystemLocales = Locale.getAvailableLocales();
					String[] languageCodes = Locale.getISOLanguages();

					final ArrayList<Locale> temp = new ArrayList<Locale>();
					for (String code : languageCodes) {
						if (code.length() == 2) { // We're only interested in two character codes
							Locale l = new Locale(code);
							if (hasLocale(l))
								temp.add(l);
						}
					}

					Collections.sort(temp, new Comparator<Locale>() {
						@Override
						public int compare(Locale lhs, Locale rhs) {
							return lhs.getDisplayLanguage(Locale.getDefault()).compareToIgnoreCase(rhs.getDisplayLanguage(Locale.getDefault()));
						}
					});

					String[] items = new String[temp.size()];
					for (int i = 0; i < temp.size(); i++)
						items[i] = temp.get(i).getDisplayLanguage(Locale.getDefault());
					
					final String[] codes = new String[temp.size()];
					for (int i = 0; i < temp.size(); i++)
						codes[i] = temp.get(i).getLanguage();
					
					temp.clear();
					
					int checkedItem = getIndexForLocale(codes, PreferenceManager.getDefaultSharedPreferences(requireActivity()).getString(LANGUAGE_PREFERENCE, "en"));
					if (checkedItem == -1)
						checkedItem = getIndexForLocale(codes, "en"); // "en" by default
					
					AlertDialog.Builder bldr = new AlertDialog.Builder(requireActivity());
					bldr.setTitle(R.string.set_pref_language_title);
					bldr.setSingleChoiceItems(items, checkedItem, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							savePreference(LANGUAGE_PREFERENCE, codes[which]);
							dialog.dismiss();
						}
					});
					bldr.show();

					return true;
				}
			});
	}
	
	private void savePreference(String key, String value) {
		PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit().putString(key, value).apply();
	}
	
	private boolean hasLocale(Locale l) {
		for (Locale locale : mSystemLocales)
			if (locale.equals(l))
				return true;
		return false;
	}
	
	public int getIndexForLocale(String[] languages, String locale) {
		for (int i = 0; i < languages.length; i++)
			if (languages[i].equalsIgnoreCase(locale))
				return i;
		return -1;
	}
}
