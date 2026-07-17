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

package com.miz.base;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;
import com.miz.mizuu.R;

public abstract class MizActivity extends AppCompatActivity {

    public Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MizuuApplication.setupTheme(this);
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (getLayoutResource() > 0) {
            setContentView(getLayoutResource());

            mToolbar = (Toolbar) findViewById(R.id.toolbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
                
                // Handle insets for the toolbar
                ViewCompat.setOnApplyWindowInsetsListener(mToolbar, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
                    
                    // Adjust height to include inset
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    if (lp != null) {
                        int actionBarHeight = MizLib.getActionBarHeight(this);
                        if (actionBarHeight > 0) {
                            lp.height = actionBarHeight + insets.top;
                            v.setLayoutParams(lp);
                        }
                    }
                    
                    return windowInsets;
                });
            }
            
            // Handle insets for the root view to avoid overlap with navigation bar
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, 0, 0, insets.bottom);
                    return windowInsets;
                });
            }
        }
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        try {
            // Remove manual elevation as M3 uses surface color/tonal palettes
            toolbar.setElevation(0f);
            super.setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // Samsung pls...
        }
    }

    protected abstract int getLayoutResource();
}
