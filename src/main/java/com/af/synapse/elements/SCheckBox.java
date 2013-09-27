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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.L;
import com.af.synapse.R;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SCheckBox extends BaseElement
                       implements View.OnClickListener, ActionValueClient, ActivityListener

{
    private View elementView = null;
    private CheckBox checkBox;
    private String command;

    private String label;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private boolean original = false;
    private boolean stored = false;

    private boolean lastCheck;
    private boolean lastLive;

    public SCheckBox(JSONObject element, LinearLayout layout) {
        super(element, layout);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SCheckBox has no action defined");

        if (this.element.containsKey("label"))
            this.label = (String) element.get("label");

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
                                        .inflate(R.layout.template_checkbox, this.layout, false);
        assert v != null;
        elementView = v;

        checkBox = (CheckBox) v.findViewById(R.id.SCheckBox);

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
        checkBox.setOnClickListener(this);

        elementView = v;

        return v;
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
     *  OnClickListener methods
     */

    @Override
    public void onClick(View view) {
        lastCheck = checkBox.isChecked();
        valueCheck();
    }

    /**
     *  ActionValueClient methods
     */

    @Override
    public String getLiveValue() {
        String retValue = Utils.runCommand(command);
        lastLive = !retValue.equals("0");
        return retValue;
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
    public void refreshValue() {
        getLiveValue();
        checkBox.setChecked(lastLive);
        lastCheck = lastLive;
    }

    @Override
    public void setDefaults() {

    }

    @Override
    public boolean commitValue() {
        String target = getSetValue();
        Utils.runCommand(command + " " + target);
        String result = getLiveValue();

        if (!result.equals(getStoredValue()))
            Utils.db.setValue(command, result);

        lastLive = lastCheck = stored = !result.equals("0");
        checkBox.setChecked(lastLive);
        valueCheck();

        return true;
    }

    @Override
    public void cancelValue() {
        lastCheck = lastLive = stored;
        commitValue();
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() {
        checkBox.setText(label);
        refreshValue();
        valueCheck();
    }

    @Override
    public void onResume() {}

    @Override
    public void onPause() {}

    @Override
    public void onStop() {}
}
