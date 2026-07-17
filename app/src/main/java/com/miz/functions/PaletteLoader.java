package com.miz.functions;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.palette.graphics.Palette;

import com.google.android.material.card.MaterialCardView;
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
    private int mSwatchColor = 0xFF333333; // Default dark grey
    private int mTitleTextColor = Color.WHITE;
    private int mBodyTextColor = Color.WHITE;

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

            // Try to find a dark vibrant swatch first for the background
            Palette.Swatch sw = result.getDarkVibrantSwatch();
            if (sw == null) sw = result.getDarkMutedSwatch();
            if (sw == null) sw = result.getVibrantSwatch();
            if (sw == null) sw = result.getMutedSwatch();
            if (sw == null) sw = result.getDominantSwatch();

            if (sw != null) {
                mSwatchColor = sw.getRgb();
                mTitleTextColor = sw.getTitleTextColor();
                mBodyTextColor = sw.getBodyTextColor();

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
            mFab.setImageTintList(ColorStateList.valueOf(mTitleTextColor));
        }
    }

    private void animate(final View v) {
        try {
            int startColor = Color.TRANSPARENT;
            if (v instanceof MaterialCardView) {
                ColorStateList csl = ((MaterialCardView) v).getCardBackgroundColor();
                if (csl != null) startColor = csl.getDefaultColor();
            } else if (v.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
                startColor = ((android.graphics.drawable.ColorDrawable) v.getBackground()).getColor();
            }

            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, getSwatchColor());
            colorAnimation.setDuration(500);
            colorAnimation.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                if (v instanceof MaterialCardView) {
                    ((MaterialCardView) v).setCardBackgroundColor(color);
                } else {
                    v.setBackgroundColor(color);
                }
            });
            colorAnimation.start();
            
            // Color child TextViews to ensure readability
            if (v instanceof ViewGroup) {
                colorTextViews((ViewGroup) v);
            } else if (v instanceof TextView) {
                ((TextView) v).setTextColor(mBodyTextColor);
            }
        } catch (Exception e) {
            // Some devices crash at runtime when using the ObjectAnimator
            if (v instanceof MaterialCardView) {
                ((MaterialCardView) v).setCardBackgroundColor(getSwatchColor());
            } else {
                v.setBackgroundColor(getSwatchColor());
            }
            if (v instanceof ViewGroup) {
                colorTextViews((ViewGroup) v);
            } else if (v instanceof TextView) {
                ((TextView) v).setTextColor(mBodyTextColor);
            }
        }
    }
    
    private void colorTextViews(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                // Determine which color to use based on view ID if necessary, 
                // but usually body text color is fine for all labels in these containers.
                ((TextView) child).setTextColor(mBodyTextColor);
            } else if (child instanceof ViewGroup) {
                colorTextViews((ViewGroup) child);
            }
        }
    }

    public int getSwatchColor() {
        return mSwatchColor;
    }

    public interface OnPaletteLoadedCallback {
        void onPaletteLoaded(int swatchColor);
    }
}
