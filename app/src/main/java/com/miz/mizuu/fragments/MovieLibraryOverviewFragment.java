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

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.miz.functions.MizLib;
import com.miz.loader.MovieLoader;
import com.miz.mizuu.R;

public class MovieLibraryOverviewFragment extends Fragment {

    private ViewPager mViewPager;
    private TabLayout mTabs;

    public MovieLibraryOverviewFragment() {} // Empty constructor

    public static MovieLibraryOverviewFragment newInstance() {
        return new MovieLibraryOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.viewpager_with_tabs, container, false);

        mViewPager = (ViewPager) v.findViewById(R.id.awesomepager);
        mViewPager.setPageMargin(MizLib.convertDpToPixels(getActivity(), 16));

        mTabs = (TabLayout) v.findViewById(R.id.tabs);

        mViewPager.setAdapter(new PagerAdapter(getChildFragmentManager()));
        mTabs.setupWithViewPager(mViewPager);

        return v;
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {getString(R.string.choiceAllMovies), getString(R.string.choiceFavorites), getString(R.string.choiceCollections)};

        public PagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public Fragment getItem(int index) {
            switch (index) {
                case 0:
                    return MovieLibraryFragment.newInstance(MovieLoader.ALL_MOVIES);
                case 1:
                    return MovieLibraryFragment.newInstance(MovieLoader.FAVORITES);
                case 2:
                    return MovieLibraryFragment.newInstance(MovieLoader.COLLECTIONS);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }
}
