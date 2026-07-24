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

import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.miz.base.MizActivity;
import com.miz.mizuu.fragments.ActorDetailsFragment;
import com.miz.utils.ViewUtils;

public class ActorDetails extends MizActivity {

    private static String TAG = "ActorDetailsFragment";
    private MaterialButton mBackButton, mMenuButton;
    private View mBottomControls;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_details;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set theme
        setTheme(R.style.Mizuu_Theme_NoBackground);

        ViewUtils.setupWindowFlagsForStatusbarOverlay(getWindow(), true);

        mBottomControls = findViewById(R.id.bottom_controls);
        mBackButton = findViewById(R.id.fab_back);
        mMenuButton = findViewById(R.id.fab_menu);

        // Actors don't have a specific menu in current logic, so hide it but keep back button
        mMenuButton.setVisibility(View.GONE);
        mBackButton.setOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(mBottomControls, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom + 16);
            return windowInsets;
        });

        setTitle(null);

        String actorId = getIntent().getExtras().getString("actorID");

        Fragment frag = getSupportFragmentManager().findFragmentByTag(TAG);
        if (frag == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, ActorDetailsFragment.newInstance(actorId), TAG);
            ft.commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
