/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.graphics.Color;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionNotification;
import com.af.synapse.lib.ActionValueEvent;
import com.af.synapse.lib.ActionValueNotifierClient;
import com.af.synapse.lib.ActionValueNotifierHandler;
import com.af.synapse.lib.ActionValueUpdater;
import com.af.synapse.lib.ActivityListener;
import com.af.synapse.R;
import com.af.synapse.lib.ElementSelector;
import com.af.synapse.lib.Selectable;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;
import com.af.synapse.view.SmartSeeker;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Andrei on 31/08/13.
 */
public class SSeekBar extends BaseElement
                      implements SeekBar.OnSeekBarChangeListener,
                                 View.OnClickListener,
                                 ActionValueNotifierClient,
                                 ActivityListener,
                                 Selectable
{
    private View elementView = null;
    private SmartSeeker seekBar;

    private FrameLayout selectorFrame;

    private ImageButton minusButton;
    private ImageButton plusButton;

    private TextView defaultLabel;
    private TextView seekLabel;
    private TextView storedLabel;

    private STitleBar titleObj = null;

    private static int dfl_id = View.generateViewId();
    private static int sbl_id = View.generateViewId();
    private static int pbl_id = View.generateViewId();
    private static int mbl_id = View.generateViewId();

    private static final int buttonWidth = (int)Utils.mainActivity.getResources().
                                                getDimension(R.dimen.stepButtons_width);
    private final String command;
    private String unit = "";
    private double weight = 1;

    private Runnable resumeTask = null;
    private Runnable seekTask = null;

    private int offset = 0;
    private int step = 1;
    private int max;

    private boolean isListBound = false;
    private boolean absolute = false;
    private ArrayList<Integer> values = null;
    private boolean hasLabels = false;
    private ArrayList<String> labels;

    private int original = Integer.MIN_VALUE;
    private int stored = Integer.MIN_VALUE;

    private int maxSeek;
    private int lastProgress;

    private int lastSeek;
    private int lastLive;

    public SSeekBar(JSONObject element, LinearLayout layout,
                    MainActivity.TabSectionFragment fragment) throws ElementFailureException {
        super(element, layout, fragment);

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

            if (element.containsKey("step")) {
                this.step = (Integer) element.get("step");

                if (this.step < 1)
                    throw new IllegalArgumentException("Step-size must be greater than zero.");
            }
        } else
            if (element.containsKey("absolute"))
                this.absolute = (Boolean) element.get("absolute");

        if (element.containsKey("unit"))
            this.unit = (String) element.get("unit");

        if (element.containsKey("weight"))
            this.weight = (Double) element.get("weight");

        if (element.containsKey("title"))
            titleObj = new STitleBar(element, layout, fragment);

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

        ActionValueNotifierHandler.register(this);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        RelativeLayout v;
        LinearLayout descriptionFrame;

        if (Utils.useInflater) {
            v = (RelativeLayout) LayoutInflater.from(Utils.mainActivity)
                    .inflate(R.layout.template_seekbar, this.layout, false);
            assert v != null;
            elementView = v;

            seekBar = (SmartSeeker) v.findViewById(R.id.SSeekBar_seekBar);
            minusButton = (ImageButton) v.findViewById(R.id.SSeekBar_minusButton);
            plusButton = (ImageButton) v.findViewById(R.id.SSeekBar_plusButton);

            defaultLabel = (TextView) v.findViewById(R.id.SSeekBar_defaultLabel);
            seekLabel = (TextView) v.findViewById(R.id.SSeekBar_seekLabel);
            storedLabel = (TextView) v.findViewById(R.id.SSeekBar_storedLabel);

            descriptionFrame = (LinearLayout) v.findViewById(R.id.SSeekBar_descriptionFrame);
        } else {
            v = new RelativeLayout(Utils.mainActivity);
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            final int topPadding = (int) (2 * Utils.density + 0.5f);
            v.setPadding(0,topPadding,0,0);

            descriptionFrame = new LinearLayout(Utils.mainActivity);
            seekBar = new SmartSeeker(Utils.mainActivity);
            plusButton = new ImageButton(Utils.mainActivity);
            minusButton = new ImageButton(Utils.mainActivity);
            seekLabel = new TextView(Utils.mainActivity);
            defaultLabel = new TextView(Utils.mainActivity);
            storedLabel = new TextView(Utils.mainActivity);

            LayoutParams dfl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            descriptionFrame.setId(dfl_id);
            seekBar.setId(sbl_id);
            plusButton.setId(pbl_id);
            minusButton.setId(mbl_id);

            descriptionFrame.setOrientation(LinearLayout.VERTICAL);
            final int leftMargin = (int) (2 * Utils.density + 0.5f);
            dfl.setMargins(leftMargin,0,0,0);
            dfl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            descriptionFrame.setLayoutParams(dfl);

            final int minimumWidth = (int) (260 * Utils.density + 0.5f);
            seekBar.setMinimumWidth(minimumWidth);
            LayoutParams sbl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            sbl.addRule(RelativeLayout.CENTER_HORIZONTAL);
            sbl.addRule(RelativeLayout.BELOW, dfl_id);
            sbl.addRule(RelativeLayout.LEFT_OF, pbl_id);
            sbl.addRule(RelativeLayout.RIGHT_OF, mbl_id);
            seekBar.setLayoutParams(sbl);

            plusButton.setImageResource(R.drawable.navigation_next_item);
            plusButton.setBackground(null);
            plusButton.setAlpha((float)0.5);
            LayoutParams pbl = new LayoutParams(buttonWidth, LayoutParams.WRAP_CONTENT);
            pbl.addRule(RelativeLayout.ALIGN_TOP, sbl_id);
            pbl.addRule(RelativeLayout.ALIGN_BOTTOM, sbl_id);
            pbl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            plusButton.setLayoutParams(pbl);

            minusButton.setImageResource(R.drawable.navigation_previous_item);
            minusButton.setBackground(null);
            minusButton.setAlpha((float) 0.5);
            LayoutParams mbl = new LayoutParams(buttonWidth, LayoutParams.WRAP_CONTENT);
            mbl.addRule(RelativeLayout.ALIGN_TOP, sbl_id);
            mbl.addRule(RelativeLayout.ALIGN_BOTTOM, sbl_id);
            mbl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            minusButton.setLayoutParams(mbl);

            seekLabel.setTextAppearance(Utils.mainActivity,
                    android.R.style.TextAppearance_DeviceDefault_Small);
            LayoutParams skl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            skl.addRule(RelativeLayout.CENTER_HORIZONTAL);
            skl.addRule(RelativeLayout.BELOW, sbl_id);
            seekLabel.setLayoutParams(skl);

            defaultLabel.setVisibility(ImageView.INVISIBLE);
            defaultLabel.setTextAppearance(Utils.mainActivity,
                    android.R.style.TextAppearance_DeviceDefault_Small);
            LayoutParams dll = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            dll.addRule(RelativeLayout.CENTER_HORIZONTAL);
            dll.addRule(RelativeLayout.BELOW, sbl_id);
            dll.addRule(RelativeLayout.RIGHT_OF, mbl_id);
            defaultLabel.setLayoutParams(dll);

            storedLabel.setVisibility(ImageView.INVISIBLE);
            storedLabel.setTextAppearance(Utils.mainActivity,
                    android.R.style.TextAppearance_DeviceDefault_Small);
            LayoutParams sll = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            sll.addRule(RelativeLayout.CENTER_HORIZONTAL);
            sll.addRule(RelativeLayout.BELOW, sbl_id);
            sll.addRule(RelativeLayout.LEFT_OF, pbl_id);
            storedLabel.setLayoutParams(sll);

            v.addView(descriptionFrame);
            v.addView(seekBar);
            v.addView(plusButton);
            v.addView(minusButton);
            v.addView(seekLabel);
            v.addView(defaultLabel);
            v.addView(storedLabel);

            v.setOnLongClickListener(this);

            LinearLayout elementFrame = new LinearLayout(Utils.mainActivity);
            elementFrame.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            selectorFrame = new FrameLayout(Utils.mainActivity);
            LinearLayout.LayoutParams sfl = new LinearLayout.LayoutParams((int) (15 * Utils.density + 0.5f), LayoutParams.MATCH_PARENT);
            int margin = (int) (Utils.density + 0.5f);
            sfl.setMargins(0, margin, 0, margin);
            selectorFrame.setLayoutParams(sfl);

            selectorFrame.setBackgroundColor(Color.DKGRAY);
            selectorFrame.setVisibility(View.GONE);

            elementFrame.addView(selectorFrame);
            elementFrame.addView(v);
            elementView = elementFrame;
        }

        /**
         *  Nesting another element's view in our own for title and description.
         */


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
            maxSeek = values.size() - 1;
            if (!absolute)
                seekBar.setValues(values);
            else
                seekBar.setMax(maxSeek);
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

        return elementView;
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
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.SET);
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
     *  Selectable methods
     */

    private boolean selectable = false;

    public void setSelectable(boolean flag){
        selectable = flag;
    }

    @Override
    public boolean onLongClick(View view) {
        if (!selectable)
            return false;

        if (isSelected())
            deselect();
        else
            select();

        return false;
    }

    @Override
    public void select() {
        selectorFrame.setVisibility(View.VISIBLE);
        ElementSelector.addElement(this);
    }

    @Override
    public void deselect() {
        selectorFrame.setVisibility(View.GONE);
        ElementSelector.removeElement(this);
    }

    @Override
    public boolean isSelected() {
        return selectorFrame.getVisibility() == View.VISIBLE;
    }

    /**
     *  ActionValueNotifierClient methods
     */

    @Override
    public String getId() {
        return command;
    }

    private ArrayDeque<ActionNotification> queue = new ArrayDeque<ActionNotification>();
    private boolean jobRunning = false;

    public void handleNotifications() {
        jobRunning = true;
        while (queue.size() > 0) {
            ActionNotification current = queue.removeFirst();
            switch (current.notification) {
                case REFRESH:
                    try { refreshValue(); } catch (ElementFailureException e) { e.printStackTrace(); }
                    break;

                case CANCEL:
                    try { cancelValue(); } catch (ElementFailureException e) { e.printStackTrace(); }
                    break;

                case RESET:
                    setDefaults();
                    break;

                case APPLY:
                    try { applyValue(); } catch (ElementFailureException e) {  e.printStackTrace(); }
                    break;
            }
        }
        jobRunning = false;
    }

    private Runnable dequeJob = new Runnable() {
        @Override
        public void run() {
            handleNotifications();
        }
    };

    @Override
    public void onNotify(ActionValueNotifierClient source, ActionValueEvent notification) {
        queue.add(new ActionNotification(source, notification));

        if (queue.size() == 1 && !jobRunning)
            Synapse.handler.post(dequeJob);
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
        int oldLive = lastLive;
        String val = getLiveValue();
        if (lastLive != oldLive)
            setSeek(val);
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.REFRESH);
    }

    @Override
    public void setDefaults() {
        if (original != Integer.MIN_VALUE)
            setSeek(String.valueOf(original));
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.RESET);
    }

    private boolean commitValue() throws ElementFailureException {
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
    public void applyValue() throws ElementFailureException {
        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.APPLY);
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastSeek = lastLive = stored;
        if (isListBound)
            lastProgress = values.indexOf(lastSeek);

        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.CANCEL);
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onMainStart() throws ElementFailureException {
        if (!Utils.mainActivity.isChangingConfigurations() && Utils.appStarted)
            Synapse.executor.execute(resumeTask);

        if (!Utils.mainActivity.isChangingConfigurations() && !Utils.appStarted)
            try {
                ActionValueNotifierHandler.addNotifiers(this);
            } catch (Exception e) {
                Utils.createElementErrorView(new ElementFailureException(this, e));
            }
    }

    @Override
    public void onStart() {}

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}
}