package com.af.synapse.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import com.af.synapse.R;
import com.af.synapse.utils.L;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Andrei on 29/09/13.
 */
public class SmartSeeker extends View {
    private static Bitmap thumbNormal = null;
    private static Bitmap thumbPressed = null;
    private static Bitmap thumbDisabled = null;

    private static Paint paint = null;

    private static int thumbHalfHeight;
    private static int thumbHalfWidth;

    private static boolean initialized = false;

    private static int barBackThickness;
    private static int barFillThickness;

    private static Paint barPaintBack;
    private static Paint barPaintFill;

    private RectF barRectFill = new RectF();
    private RectF barRectBack = new RectF();

    private int barLength;

    private int savedPointerPosition;
    private int seekerPointerPosition;

    private boolean isListBound = false;
    private boolean isMovingSeeker = false;

    private ArrayList<Integer> values = null;
    ArrayList<Integer> valuePoints = new ArrayList<Integer>();
    ArrayList<Integer> bounds = new ArrayList<Integer>();

    private int max = 10;

    private int indexNew = 5;
    private int indexLast = indexNew;
    private int indexOld = 7;

    private SeekBar.OnSeekBarChangeListener listener;

    public SmartSeeker(Context context) {
        super(context);
        if (!initialized)
            initialize(null, 0);
        setPointers();
    }

    public SmartSeeker(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!initialized)
            initialize(attrs, 0);
        setPointers();
    }

    public SmartSeeker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!initialized)
            initialize(attrs, defStyle);
        setPointers();
    }

    private void initialize(AttributeSet attrs, int defStyle) {
        thumbNormal     = BitmapFactory.decodeResource(getResources(),
                R.drawable.scrubber_control_normal_holo);
        thumbPressed    = BitmapFactory.decodeResource(getResources(),
                R.drawable.scrubber_control_pressed_holo);
        thumbDisabled   = BitmapFactory.decodeResource(getResources(),
                R.drawable.scrubber_control_disabled_holo);

        thumbHalfHeight = thumbPressed.getHeight() / 2;
        thumbHalfWidth = thumbPressed.getWidth() / 2;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TypedArray  a = getContext().obtainStyledAttributes(attrs,
                R.styleable.SmartSeeker, defStyle, 0);
        Resources   b = getContext().getResources();

        barBackThickness = a.getDimensionPixelSize(
                R.styleable.SmartSeeker_smartseeker_bar_back_thickness,
                b.getDimensionPixelSize(R.dimen.bar_back_thickness));

        barFillThickness = a.getDimensionPixelSize(
                R.styleable.SmartSeeker_smartseeker_bar_fill_thickness,
                b.getDimensionPixelSize(R.dimen.bar_fill_thickness));

        barLength = a.getDimensionPixelSize(R.styleable.ColorBars_bar_length,
                b.getDimensionPixelSize(R.dimen.bar_length));

        barPaintBack = new Paint();
        barPaintBack.setColor(Color.DKGRAY);

        barPaintFill = new Paint();
        barPaintFill.setColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
        initialized = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        barLength = widthSize - (thumbHalfWidth * 2);
        setMeasuredDimension((barLength + (thumbHalfWidth * 2)), (thumbHalfHeight * 2));
    }

    @Override
    protected void onSizeChanged(int newW, int newH, int oldW, int oldH) {
        super.onSizeChanged(newW, newH, oldW, oldH);

        /** newW is the width of the whole View. The bar fills the whole space, minus the extending
         *  radius of the picker halo extending on either side.
         */
        barLength = newW - (thumbHalfHeight * 2);
        resetValueCoordinates();
        setPointers();
    }

    private void setPointers() {
        if (isListBound && valuePoints.size() > 0) {
            seekerPointerPosition = valuePoints.get(indexNew);
            savedPointerPosition = valuePoints.get(indexOld);
        } else {
            seekerPointerPosition = Math.round(barLength * ((float)indexNew / max));
            savedPointerPosition = Math.round(barLength * ((float)indexOld / max));
        }

        indexLast = indexNew;

        /* The rectangle dimensions which defines the bar itself */
        barRectBack.set(seekerPointerPosition + thumbHalfWidth, // Start at the live seeker
                (thumbHalfHeight - (barBackThickness / 2)),     // Top is thumb radius, minus half thickness
                (barLength + thumbHalfWidth),                   // Right is total length plus halo radius
                (thumbHalfHeight + (barBackThickness / 2)));    // Bottom is thumb radius, plus half thickness


        /* The rectangle dimensions which defines the bar itself */
        barRectFill.set(thumbHalfWidth,                         // Start at the beginning plus seeker radius
                (thumbHalfHeight - (barFillThickness / 2)),     // Top is thumb radius, minus half thickness
                (seekerPointerPosition + thumbHalfWidth),       // Right is live seeker position plus seeker radius
                (thumbHalfHeight + (barFillThickness / 2)));    // Bottom is thumb radius, plus half thickness
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(barRectBack, barPaintBack);
        canvas.drawRect(barRectFill, barPaintFill);

        if (indexOld != indexNew)
            drawThumb(canvas, thumbDisabled, savedPointerPosition);

        drawThumb(canvas, isMovingSeeker ? thumbPressed : thumbNormal, seekerPointerPosition);
    }

    private void drawThumb(Canvas canvas, Bitmap thumb, float position) {
        canvas.drawBitmap(thumb, position, (float) ((getHeight() * 0.5f) - thumbHalfHeight), paint);
    }

    @Override
    public void invalidate() {
        setPointers();
        super.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /**
                 *  To avoid unnecessary touches we ignore any coordinates not within a thin
                 *  area surrounding the actual bar. Area radius is half the scrubber radius.
                 *
                 *  Let through whole height touches which surround the pointer.
                 */
                float topBoundary = ((getHeight() * 0.5f) - (thumbHalfHeight * 2));
                float bottomBoundary = ((getHeight() * 0.5f) + (thumbHalfHeight * 2));

                if ((y > topBoundary && y < bottomBoundary) &&
                        (x > (seekerPointerPosition - thumbHalfWidth * 2) &&
                         x < (seekerPointerPosition + thumbHalfWidth * 2))){
                    isMovingSeeker = true;
                    invalidate();
                } else
                    return false;
            case MotionEvent.ACTION_MOVE:
                if (isMovingSeeker)
                    getParent().requestDisallowInterceptTouchEvent(true);
                else
                    return false;

                /*  Normalize x coordinates to the bar dimensions */
                x -= thumbHalfWidth;

                if (isListBound) {
                    indexNew = 0;

                    for (int bound : bounds)
                        if (x > bound)
                            ++indexNew;
                        else
                            break;

                    indexNew = Math.max(0, Math.min(values.size() - 1, indexNew));
                } else
                    indexNew = Math.max(0, Math.min(max, Math.round((x / barLength) * max)));

                if (indexNew != indexLast) {
                    invalidate();
                    if(listener != null)
                        listener.onProgressChanged(null, indexNew, true);
                }

                break;
            case MotionEvent.ACTION_UP:
                isMovingSeeker = false;
                invalidate();
                break;
        }

        return true;
    }

    public void setMax(int maxValue) {
        this.max = maxValue;
    }

    private void resetValueCoordinates() {
        valuePoints.clear();
        bounds.clear();

        if (!isListBound)
            return;

        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;

        for (int value : this.values) {
            minimum = Math.min(value, minimum);
            maximum = Math.max(value, maximum);
        }

        int range = maximum - minimum;
        int offset = minimum * -1;

        for (int value : this.values)
            valuePoints.add(Math.round(barLength * ((float)(value + offset) / range)));

        bounds.add(0);

        for (int i = 1; i < valuePoints.size(); i++)
            bounds.add((valuePoints.get(i-1) + valuePoints.get(i)) / 2);

        bounds.add(barLength);
        bounds.remove(0);
    }

    public void setValues(ArrayList<Integer> values) {
        this.values = values;

        if ((values != null) && (values.size() >= 2) && (values.get(0) > values.get(1)))
            Collections.reverse(values);

        isListBound = values != null;
    }

    public void setProgress(int progress) {
        int val = Math.max(0, Math.min(progress, isListBound ? values.size() - 1 : max));

        if (val != indexNew) {
            indexNew = val;
            invalidate();

            if (listener != null)
                listener.onProgressChanged(null, indexNew, false);
        }
    }

    public void setSaved(int progress) {
        int val = Math.max(0, Math.min(progress, isListBound ? values.size() - 1 : max));

        if (val != indexOld) {
            indexOld = val;
            invalidate();
        }
    }

    public void incrementProgressBy(int progressDelta) {
        setProgress(indexNew + progressDelta);
    }

    public void setOnSeekBarChangeListener (SeekBar.OnSeekBarChangeListener listener) {
        this.listener = listener;
    }
}
