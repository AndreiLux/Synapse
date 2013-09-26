/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.L;
import com.af.synapse.R;
import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 31/08/13.
 */
public class SSeekBar extends BaseElement
                      implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,
                                 ActionValueClient, ActivityListener
{
    private View elementView = null;
    private SeekBar seekBar;
    private ImageButton minusButton;
    private ImageButton plusButton;

    private TextView defaultLabel; //TODO method to reset to default
    private TextView seekLabel;
    private TextView storedLabel;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private final String command;
    private String unit = "";
    private double weight = 1;
    private int offset = 0;
    private int step = 1;
    private int max;
    private int original = Integer.MIN_VALUE;
    private int stored = Integer.MIN_VALUE;

    private int maxSeek;
    private int lastProgress;

    private int lastSeek;
    private int lastLive;

    private boolean progressBlock = true;

    public SSeekBar(JSONObject element, LinearLayout layout) {
        super(element, layout);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SSeekBar has no action defined");

        if (element.containsKey("max"))
            this.max = (Integer) element.get("max");
        else
            throw new IllegalArgumentException("SSeekBar has no maximum defined");

        if (element.containsKey("min"))
            this.offset = (Integer) element.get("min");

        if (element.containsKey("step"))
            this.step = (Integer) element.get("step");

        if (element.containsKey("default"))
            this.original = (Integer) element.get("default");

        if (element.containsKey("unit"))
            this.unit = (String) element.get("unit");

        if (element.containsKey("weight"))
            this.weight = (Double) element.get("weight");

        /**
         *  Add a description element inside our own with the same JSON object
         */
        if (element.containsKey("description"))
            descriptionObj = new SDescription(element, layout);

        if (element.containsKey("title"))
            titleObj = new STitleBar(element, layout);
    }

    @Override
    public View getView() {
        if (elementView != null)
            return elementView;

        View v = LayoutInflater.from(Utils.mainActivity)
                                     .inflate(R.layout.template_seekbar, this.layout, false);
        assert v != null;
        elementView = v;

        seekBar     = (SeekBar) v.findViewById(R.id.SSeekBar_seekBar);
        minusButton = (ImageButton)  v.findViewById(R.id.SSeekBar_minusButton);
        plusButton  = (ImageButton)  v.findViewById(R.id.SSeekBar_plusButton);

        defaultLabel    = (TextView) v.findViewById(R.id.SSeekBar_defaultLabel);
        seekLabel       = (TextView) v.findViewById(R.id.SSeekBar_seekLabel);
        storedLabel     = (TextView) v.findViewById(R.id.SSeekBar_storedLabel);

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) v.findViewById(R.id.SSeekBar_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        /**
         *  Rest of initializers
         */

        if (original != Integer.MIN_VALUE)
            defaultLabel.setText("D:" + Utils.df.format(original * weight) + unit);
        else
            defaultLabel.setVisibility(ImageView.INVISIBLE);

        maxSeek = (max - offset) / step;

        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(maxSeek);

        String initialLive = getLiveValue();
        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }

        storedLabel.setText("S:" + Utils.df.format(stored * weight) + unit);

        minusButton.setOnClickListener(this);
        plusButton.setOnClickListener(this);

        seekLabel.setText(Utils.df.format(Integer.valueOf(initialLive) * weight) + unit);
        setSeek(initialLive);

        return v;
    }

    private void valueCheck() {
        if (lastSeek == lastLive && lastSeek == stored) {
            elementView.setBackground(null);
            storedLabel.setVisibility(View.GONE);

            if (original != Integer.MIN_VALUE)
                defaultLabel.setVisibility(View.GONE);

            if (ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.removeElement(this);
        } else {
            elementView.setBackgroundColor(Utils.mainActivity.getResources()
                                                        .getColor(R.color.element_value_changed));
            storedLabel.setVisibility(View.VISIBLE);

            if (original != Integer.MIN_VALUE)
                defaultLabel.setVisibility(View.VISIBLE);

            if (!ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.registerElement(this);
        }
    }

    private void setSeek(String value) {
        lastSeek = Integer.valueOf(value);
        int newProgress = (Integer.parseInt(value) - offset) / step;
        seekBar.setProgress(newProgress);
    }

    /**
     *  OnSeekBarChangeListener methods
     */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        /**
         *  The progressBlock is there to avoid capturing the input from a (to me) unknown
         *  entity which keeps on setting the progress to a value of 40 for all SeekBars and
         *  derivatives during/after an Activity configuration change; this includes a layout
         *  orientation change. This seem to be a quirk only affecting ProgressBar and none
         *  of the other elements.
         *
         *  D/ProgressBar: setProgress = 40
         *                                ^- Why??!! Where are you coming from? Fuck yourself.
         *  D/ProgressBar: setProgress = 40, fromUser = false
         *  D/ProgressBar: mProgress = 15mIndeterminate = false, mMin = 0, mMax = 19
         *                             ^- Correct value!
         */

        if (progressBlock)
            return;

        lastProgress = progress;
        lastSeek = offset + (progress * step);
        seekLabel.setText(Utils.df.format(lastSeek * weight) + unit);
        valueCheck();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    /**
     *  OnClickListener methods
     */

    @Override
    public void onClick(View view) {
        if (view == this.minusButton && lastProgress > 0)
            seekBar.incrementProgressBy(-1);

        if (view == plusButton && lastProgress < maxSeek)
            seekBar.incrementProgressBy(1);
    }

    /**
     *  ActionValueClient methods
     */

    @Override
    public String getLiveValue() {
        String retValue = Utils.runCommand(command);
        lastLive = Integer.parseInt(retValue);
        return retValue;
    }

    @Override
    public String getSetValue() {
        return String.valueOf(lastSeek);
    }

    @Override
    public String getStoredValue() {
        String value = Utils.db.getValue(command);
        if (value == null)
            return null;

        stored = Integer.parseInt(value);
        return value;
    }

    @Override
    public void refreshValue() {
        String val = getLiveValue();
        setSeek(val);
    }

    @Override
    public boolean commitValue() {
        String target = getSetValue();
        Utils.runCommand(command + " " + target);
        String result = getLiveValue();

        if (!result.equals(getStoredValue())) {
            Utils.db.setValue(command, result);
            stored = Integer.parseInt(result);
            storedLabel.setText("S:" + Utils.df.format(stored * weight) + unit);
        }

        setSeek(result);
        valueCheck();

        return true;
    }

    @Override
    public void cancelValue() {
        lastSeek = lastLive = stored;
        commitValue();
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() {
        progressBlock = false;
        setSeek(getLiveValue());
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onStop() {
        progressBlock = true;
    }
}