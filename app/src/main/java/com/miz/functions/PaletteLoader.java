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
    private int mContrastTextColor = Color.WHITE;
    private int mAccentColor = 0xFFE91E63; // Default standout color

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
        if (view != null) {
            mViews.add(view);
        }
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

            // Background swatch for cards
            Palette.Swatch sw = result.getDarkVibrantSwatch();
            if (sw == null) sw = result.getDarkMutedSwatch();
            if (sw == null) sw = result.getVibrantSwatch();
            if (sw == null) sw = result.getMutedSwatch();
            if (sw == null) sw = result.getDominantSwatch();

            if (sw != null) {
                mSwatchColor = sw.getRgb();
                mContrastTextColor = getContrastColor(mSwatchColor);
                
                // Standout accent color for the Play Button
                Palette.Swatch accentSw = result.getVibrantSwatch();
                if (accentSw == null || accentSw == sw) accentSw = result.getLightVibrantSwatch();
                
                if (accentSw != null) {
                    mAccentColor = accentSw.getRgb();
                } else {
                    // Fallback to a color that stands out from mSwatchColor
                    mAccentColor = getOffsetColor(mSwatchColor);
                }

                // Color the views
                colorViews();

                mOnPaletteLoadedCallback.onPaletteLoaded(getSwatchColor());
            }
        }
    }

    public void colorViews() {
        // Animate the color change for background containers
        for (View v : getViews())
            animate(v);

        // Color the Play Button with the accent color
        if (getFab() != null) {
            mFab.setBackgroundTintList(ColorStateList.valueOf(mAccentColor));
            mFab.setImageTintList(ColorStateList.valueOf(getContrastColor(mAccentColor)));
        }
    }

    private void animate(final View v) {
        if (v == null) return;
        
        try {
            int startColor = Color.TRANSPARENT;
            if (v instanceof MaterialCardView) {
                ColorStateList csl = ((MaterialCardView) v).getCardBackgroundColor();
                if (csl != null) startColor = csl.getDefaultColor();
            } else if (v.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
                startColor = ((android.graphics.drawable.ColorDrawable) v.getBackground()).getColor();
            }

            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, mSwatchColor);
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
                ((TextView) v).setTextColor(mContrastTextColor);
            }
        } catch (Exception e) {
            if (v instanceof MaterialCardView) {
                ((MaterialCardView) v).setCardBackgroundColor(mSwatchColor);
            } else {
                v.setBackgroundColor(mSwatchColor);
            }
            if (v instanceof ViewGroup) {
                colorTextViews((ViewGroup) v);
            } else if (v instanceof TextView) {
                ((TextView) v).setTextColor(mContrastTextColor);
            }
        }
    }
    
    private void colorTextViews(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(mContrastTextColor);
            } else if (child instanceof ViewGroup) {
                colorTextViews((ViewGroup) child);
            }
        }
    }

    private int getContrastColor(int color) {
        // WCAG Relative Luminance
        double red = Color.red(color) / 255.0;
        double green = Color.green(color) / 255.0;
        double blue = Color.blue(color) / 255.0;

        red = red <= 0.03928 ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        green = green <= 0.03928 ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        blue = blue <= 0.03928 ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);

        double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
        return (luminance > 0.179) ? Color.BLACK : Color.WHITE;
    }

    private int getOffsetColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        // Shift hue by 180 degrees and bump saturation/value for standout
        hsv[0] = (hsv[0] + 180) % 360;
        hsv[1] = Math.max(hsv[1], 0.7f);
        hsv[2] = Math.max(hsv[2], 0.8f);
        return Color.HSVToColor(hsv);
    }

    public int getSwatchColor() {
        return mSwatchColor;
    }

    public interface OnPaletteLoadedCallback {
        void onPaletteLoaded(int swatchColor);
    }
}
