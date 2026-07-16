package com.miz.functions;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;

import androidx.palette.graphics.Palette;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.miz.mizuu.MizuuApplication;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PaletteLoader extends AsyncTask<Void, Void, Palette> {

    private final Picasso mPicasso;
    private final String mKey;
    private final Uri mImage;
    private final List<View> mViews;
    private final OnPaletteLoadedCallback mOnPaletteLoadedCallback;

    private FloatingActionButton mFab;
    private int mSwatchColor;

    public PaletteLoader(Picasso picasso, Uri image, OnPaletteLoadedCallback callback) {
        mPicasso = picasso;
        mImage = image;
        mKey = image.toString();
        mViews = new ArrayList<View>();
        mOnPaletteLoadedCallback = callback;
    }

    private String getPaletteKey() {
        return mKey;
    }

    private Uri getImage() {
        return mImage;
    }

    public void addView(View view) {
        mViews.add(view);
    }

    private List<View> getViews() {
        return mViews;
    }

    public void setFab(FloatingActionButton fab) {
        mFab = fab;
    }

    private FloatingActionButton getFab() {
        return mFab;
    }

    public void clearViews() {
        mViews.clear();
        mFab = null;
    }

    @Override
    protected Palette doInBackground(Void... params) {
        Palette palette = MizuuApplication.getPalette(getPaletteKey());

        if (palette == null) {
            try {
                palette = Palette.from(mPicasso.load(getImage()).get()).generate();
            } catch (Exception e) {}
        }

        return palette;
    }

    @Override
    protected void onPostExecute(Palette result) {
        if (result != null) {
            // Add it to the Palette cache
            MizuuApplication.addToPaletteCache(getPaletteKey(), result);

            Palette.Swatch sw = result.getDarkVibrantSwatch();

            if (sw == null)
                sw = result.getDarkMutedSwatch();

            if (sw == null)
                sw = result.getVibrantSwatch();

            if (sw != null) {
                // Set the found color
                mSwatchColor = sw.getRgb();

                // Color the views
                colorViews();

                mOnPaletteLoadedCallback.onPaletteLoaded(getSwatchColor());
            }
        }
    }

    public void colorViews() {
        // Animate the color change for all set views
        for (View v : getViews())
            animate(v);

        // Set the FAB color, if a FAB has been set
        if (getFab() != null) {
            mFab.setBackgroundTintList(ColorStateList.valueOf(getSwatchColor()));
        }
    }

    private void animate(View v) {
        try {
            ObjectAnimator backgroundColorAnimator = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), 0xFF666666, getSwatchColor());
            backgroundColorAnimator.setDuration(500);
            backgroundColorAnimator.start();
        } catch (Exception e) {
            // Some devices crash at runtime when using the ObjectAnimator
            v.setBackgroundColor(getSwatchColor());
        }
    }

    public int getSwatchColor() {
        return mSwatchColor;
    }

    public interface OnPaletteLoadedCallback {
        void onPaletteLoaded(int swatchColor);
    }

}
