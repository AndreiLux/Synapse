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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
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
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

import java.util.ArrayDeque;

/**
 * Created by Andrei on 30/08/13.
 */
public class SCheckBox extends BaseElement
                       implements CompoundButton.OnCheckedChangeListener,
                                  ActionValueNotifierClient,
                                  ActivityListener,
                                  Selectable
{
    private View elementView = null;
    private FrameLayout selectorFrame;

    private CheckBox checkBox;
    private String command;
    private Runnable resumeTask = null;

    private String label;

    private STitleBar titleObj = null;

    private int original = Integer.MIN_VALUE;
    private boolean stored = false;

    private boolean lastCheck;
    private boolean lastLive;

    private boolean onCheckedChangedIgnore = false;

    public SCheckBox(JSONObject element, LinearLayout layout,
                     MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SCheckBox has no action defined");

        if (this.element.containsKey("label"))
            this.label = Utils.localise(element.get("label"));

        if (element.containsKey("default"))
            this.original = (Integer) element.get("default");


        if (element.containsKey("title"))
            titleObj = new STitleBar(element, layout, fragment);

        resumeTask = new Runnable() {
            @Override
            public void run() {
                try {
                    refreshValue();
                } catch (ElementFailureException e) {
                    Utils.createElementErrorView(e);
                }
            }
        };

        ActionValueNotifierHandler.register(this);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        View v = LayoutInflater.from(Utils.mainActivity)
                                        .inflate(R.layout.template_checkbox, this.layout, false);
        assert v != null;
        elementView = v;

        checkBox = (CheckBox) v.findViewById(R.id.SCheckBox);

        v.setOnLongClickListener(this);
        checkBox.setOnLongClickListener(this);

        LinearLayout elementFrame = new LinearLayout(Utils.mainActivity);
        elementFrame.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        selectorFrame = new FrameLayout(Utils.mainActivity);
        LayoutParams sfl = new LayoutParams((int) (15 * Utils.density + 0.5f), LayoutParams.MATCH_PARENT);
        int margin = (int) (Utils.density + 0.5f);
        sfl.setMargins(0, margin, 0, margin);
        selectorFrame.setLayoutParams(sfl);

        selectorFrame.setBackgroundColor(Color.DKGRAY);
        selectorFrame.setVisibility(View.GONE);

        elementFrame.addView(selectorFrame);
        elementFrame.addView(v);

        elementView = elementFrame;

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) v.findViewById(R.id.SCheckBox_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        String initialLive = getLiveValue();
        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }

        lastCheck = lastLive;
        checkBox.setOnCheckedChangeListener(this);
        checkBox.setChecked(lastLive);

        return elementView;
    }

    private void valueCheck() {
        if (lastCheck == lastLive && lastCheck == stored) {
            elementView.setBackground(null);

            if (ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.removeElement(this);
        } else {
            elementView.setBackgroundColor(Utils.mainActivity.getResources()
                    .getColor(R.color.element_value_changed));

            if (!ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.registerElement(this);
        }
    }

    /**
     *  OnCheckedChangeListener methods
     */

    @Override
    public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
        if (onCheckedChangedIgnore) {
            onCheckedChangedIgnore = false;
            checkBox.setChecked(lastCheck);
        } else {
            lastCheck = isChecked;
            valueCheck();
            ActionValueNotifierHandler.propagate(this, ActionValueEvent.SET);
        }
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

        return true;
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
        L.d(notification.toString());
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
            lastLive = !retValue.equals("0");
            return retValue;
        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public String getSetValue() {
        return lastCheck ? "1" : "0";
    }

    @Override
    public String getStoredValue() {
        String value = Utils.db.getValue(command);
        if (value == null)
            return null;

        stored = !value.equals("0");
        return value;
    }

    @Override
    public void refreshValue() throws ElementFailureException {
        if (Utils.appStarted)
            getLiveValue();

        if (lastCheck == lastLive)
            return;

        lastCheck = lastLive;
        final ActionValueNotifierClient t = this;
        Synapse.handler.post(new Runnable() {
            @Override
            public void run() {
                checkBox.setChecked(lastLive);
                valueCheck();
                ActionValueNotifierHandler.propagate(t, ActionValueEvent.REFRESH);
            }
        });
    }

    @Override
    public void setDefaults() {
        if (original != Integer.MIN_VALUE) {
            lastCheck = original != 0;
            checkBox.setChecked(lastCheck);
            valueCheck();
            ActionValueNotifierHandler.propagate(this, ActionValueEvent.RESET);
        }
    }

    private void commitValue() throws ElementFailureException {
        try {
            String target = getSetValue();
            Utils.runCommand(command + " " + target);
            String result = getLiveValue();

            if (!result.equals(getStoredValue()))
                Utils.db.setValue(command, result);

            lastLive = lastCheck = stored = !result.equals("0");
            checkBox.setChecked(lastLive);
            valueCheck();

        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public void applyValue() throws ElementFailureException {
        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.APPLY);
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastCheck = lastLive = stored;
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
    public void onStart() {
        onCheckedChangedIgnore = true;
        checkBox.setText(label);
    }

    @Override
    public void onResume() {
        onCheckedChangedIgnore = false;
    }

    @Override
    public void onPause() {
        onCheckedChangedIgnore = true;
    }
}
