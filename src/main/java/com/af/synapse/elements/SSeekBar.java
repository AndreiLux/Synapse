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

import com.af.synapse.Synapse;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.R;
import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;
import com.af.synapse.view.SmartSeeker;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Andrei on 31/08/13.
 */
public class SSeekBar extends BaseElement
                      implements SeekBar.OnSeekBarChangeListener, View.OnClickListener,
                                 ActionValueClient, ActivityListener
{
    private View elementView = null;
    private SmartSeeker seekBar;
    private ImageButton minusButton;
    private ImageButton plusButton;

    private TextView defaultLabel;
    private TextView seekLabel;
    private TextView storedLabel;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private final String command;
    private String unit = "";
    private double weight = 1;

    private Runnable resumeTask = null;
    private Runnable seekTask = null;

    private int offset = 0;
    private int step = 1;
    private int max;

    private boolean isListBound = false;
    private ArrayList<Integer> values = null;
    private boolean hasLabels = false;
    private ArrayList<String> labels;

    private int original = Integer.MIN_VALUE;
    private int stored = Integer.MIN_VALUE;

    private int maxSeek;
    private int lastProgress;

    private int lastSeek;
    private int lastLive;

    public SSeekBar(JSONObject element, LinearLayout layout) throws ElementFailureException {
        super(element, layout);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SSeekBar has no action defined");

        if (element.containsKey("default"))
            this.original = (Integer) element.get("default");

        if (element.containsKey("values")) {
            this.isListBound = element.containsKey("listBound") ? (Boolean) element.get("listBound") : true;
            values = new ArrayList<Integer>();
            Object jsonValues = element.get("values");
            if (jsonValues instanceof JSONArray)
                for (Object value : (JSONArray)jsonValues)
                    values.add((Integer) value);
            else if (jsonValues instanceof JSONObject) {
                labels = new ArrayList<String>();

                /**
                 *  We expect integers as the map keys, but they're unsorted. Create a fancy
                 *  TreeMap with custom comparator to add the values to so that they get sorted.
                 */
                Map<String, Object> sorted = new TreeMap<String, Object>(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        int i1 = Integer.valueOf(o1);
                        int i2 = Integer.valueOf(o2);

                        if (i1 > i2)
                            return 1;

                        if (i1 < i2)
                            return -1;

                        return 0;
                    }
                });

                sorted.putAll((JSONObject) jsonValues);

                for (Map.Entry<String, Object> set : sorted.entrySet()) {
                    values.add(Integer.valueOf(set.getKey()));
                    labels.add(Utils.localise(set.getValue()));
                }

                if (values.isEmpty())
                    throw new IllegalArgumentException("Empty values given.");

                if (this.original != Integer.MIN_VALUE && values.indexOf(this.original) == -1 && this.isListBound)
                    throw new IllegalArgumentException("Default value not contained in given values.");

                hasLabels = true;
            }
        }

        if (!isListBound) {
            if (element.containsKey("max"))
                this.max = (Integer) element.get("max");
            else
                throw new IllegalArgumentException("Maximum value is not defined.");

            if (element.containsKey("min"))
                this.offset = (Integer) element.get("min");

            if (element.containsKey("step"))
                this.step = (Integer) element.get("step");
        }

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

        resumeTask = new Runnable() {
            @Override
            public void run() {
                    try {
                        setSeek(getLiveValue());
                    } catch (ElementFailureException e) {
                        Utils.createElementErrorView(e);
                    }
            }
        };

        seekTask = new Runnable() {
            @Override
            public void run() {
                if (isListBound)
                    seekBar.setProgress(values.indexOf(lastSeek));
                else
                    seekBar.setProgress((lastSeek - offset) / step);
            }
        };
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        View v = LayoutInflater.from(Utils.mainActivity)
                                     .inflate(R.layout.template_seekbar, this.layout, false);
        assert v != null;
        elementView = v;

        seekBar     = (SmartSeeker) v.findViewById(R.id.SSeekBar_seekBar);
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
            if (hasLabels && values.indexOf(this.original) != -1)
                defaultLabel.setText("D:" + labels.get(values.indexOf(original)));
            else
                defaultLabel.setText("D:" + Utils.df.format(original * weight) + unit);
        else
            defaultLabel.setVisibility(ImageView.INVISIBLE);

        seekBar.setOnSeekBarChangeListener(this);

        String initialLive = getLiveValue();
        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }

        if (isListBound) {
            seekBar.setValues(values);
            maxSeek = values.size() - 1;
            seekBar.setSaved(values.indexOf(stored));
        } else {
            maxSeek = (max - offset) / step;
            seekBar.setMax(maxSeek);
            seekBar.setSaved((stored - offset) / step);
        }

        minusButton.setOnClickListener(this);
        plusButton.setOnClickListener(this);

        if (hasLabels && values.indexOf(Integer.valueOf(initialLive)) != -1) {
            seekLabel.setText(labels.get(values.indexOf(Integer.valueOf(initialLive))));
            storedLabel.setText("S:" + labels.get(values.indexOf(stored)));
        } else {
            seekLabel.setText(Utils.df.format(Integer.valueOf(initialLive) * weight) + unit);
            storedLabel.setText("S:" + Utils.df.format(stored * weight) + unit);
        }

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
        Synapse.handler.post(seekTask);
    }

    /**
     *  OnSeekBarChangeListener methods
     */

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        lastProgress = progress;

        lastSeek = isListBound ? values.get(lastProgress) : offset + (progress * step);

        if (hasLabels && values.indexOf(lastSeek) != -1)
            seekLabel.setText(labels.get(values.indexOf(lastSeek)));
        else
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
    public String getLiveValue() throws ElementFailureException {
        try {
            String retValue = Utils.runCommand(command);
            lastLive = Integer.parseInt(retValue);
            return retValue;
        } catch (Exception e) { throw new ElementFailureException(this, e); }
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
    public void refreshValue() throws ElementFailureException {
        String val = getLiveValue();
        setSeek(val);
    }

    @Override
    public void setDefaults() {
        if (original != Integer.MIN_VALUE)
            setSeek(String.valueOf(original));
    }

    @Override
    public boolean commitValue() throws ElementFailureException {
        try {
            String target = getSetValue();
            Utils.runCommand(command + " " + target);
            String result = getLiveValue();

            if (!result.equals(getStoredValue())) {
                Utils.db.setValue(command, result);
                stored = Integer.parseInt(result);

                if (hasLabels && values.indexOf(stored) != -1)
                    storedLabel.setText("S:" + labels.get(values.indexOf(stored)));
                else
                    storedLabel.setText("S:" + Utils.df.format(stored * weight) + unit);
            }

            setSeek(result);

            seekBar.setSaved(isListBound ? lastProgress : (stored - offset) / step);
            valueCheck();

            return true;
        } catch (Exception e) { throw new ElementFailureException(this, e); }
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastSeek = lastLive = stored;
        if (isListBound)
            lastProgress = values.indexOf(lastSeek);
        commitValue();
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() throws ElementFailureException {}

    @Override
    public void onResume() {
        if (!Utils.mainActivity.isChangingConfigurations() && Utils.appStarted)
            Synapse.executor.execute(resumeTask);
    }

    @Override
    public void onPause() {}

    @Override
    public void onStop() {}
}