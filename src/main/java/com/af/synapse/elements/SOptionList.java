/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Andrei on 12/09/13.
 */
public class SOptionList extends BaseElement
                         implements AdapterView.OnItemSelectedListener,
                         ActionValueClient, ActivityListener, View.OnClickListener {
    private View elementView = null;
    private Spinner spinner;
    private ImageButton previousButton;
    private ImageButton nextButton;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private String command;
    Object values;
    List<String> items = new ArrayList<String>();
    List<String> labels = new ArrayList<String>();
    private String unit = "";

    private String original = null;
    private String stored = null;

    private String lastSelect = null;
    private String lastLive = null;

    public SOptionList(JSONObject element, Activity activity, LinearLayout layout) {
        super(element, activity, layout);

        if (element.containsKey("action"))
            this.command = element.get("action").toString();
        else
            throw new IllegalArgumentException("SOptionList has no action defined");

        if (element.containsKey("unit"))
            this.unit = element.get("unit").toString();

        if (element.containsKey("values")) {
            this.values = element.get("values");
            if (values instanceof JSONArray)
                for (Object value : (JSONArray)this.values) {
                    items.add(value.toString());
                    labels.add(value.toString() + unit);
                }
            else if (values instanceof JSONObject)
                for (Map.Entry<String, Object> set : ((JSONObject) values).entrySet()) {
                    items.add(set.getKey());
                    labels.add(set.getValue().toString());
                }
            else
                L.w("SOptionList without values detected!");
        } else
            L.w("SOptionList without values detected!");

        if (element.containsKey("default"))
            this.original = element.get("default").toString();


        /**
         *  Add a description element inside our own with the same JSON object
         */
        if (element.containsKey("description"))
            descriptionObj = new SDescription(element, activity, layout);

        if (element.containsKey("title"))
            titleObj = new STitleBar(element, activity, layout);
    }

    private void prepareUI(){
        elementView = LayoutInflater.from(this.activity)
                            .inflate(R.layout.template_optionlist, this.layout, false);
    }

    @Override
    public View getView() {
        if (elementView != null)
            return elementView;

        /**
         *  SOptionList needs to inflate its View inside of the main UI thread because the
         *  spinner spawns a Looper handler which may not exist in auxiliary threads.
         *
         *  We use use a CountDownLatch for inter-thread concurrency.
         */

        final CountDownLatch latch = new CountDownLatch(1);

        Utils.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepareUI();
                latch.countDown();
            }
        });

        String initialLive = getLiveValue();
        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }
        lastSelect = lastLive;

        try { latch.await(); } catch (InterruptedException ignored) {}

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) elementView.findViewById(R.id.SOptionList_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        /**
         *  Next and previous buttons
         */

        previousButton = (ImageButton)  elementView.findViewById(R.id.SOptionList_previousButton);
        nextButton = (ImageButton)  elementView.findViewById(R.id.SOptionList_nextButton);
        previousButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);

        /**
         *  The spinner itself
         */

        spinner = (Spinner) elementView.findViewById(R.id.SOptionList_spinner);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.activity,
                                                R.layout.template_optionlist_main_item, labels);
        adapter.setDropDownViewResource(R.layout.template_optionlist_list_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(items.indexOf(lastLive));

        return elementView;
    }

    private void valueCheck() {
        if (lastSelect.equals(lastLive) && lastSelect.equals(stored)) {
            elementView.setBackground(null);

            if (ActionValueUpdater.isRegistered(this))
                ActionValueUpdater.removeElement(this);
        } else {
            elementView.setBackgroundColor(activity.getResources()
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
        int i = spinner.getSelectedItemPosition();

        /* Keep in mind that the top item is of lower index */

        if (view == nextButton && i > 0)
            spinner.setSelection(i - 1);

        if (view == previousButton && i < (items.size() - 1))
            spinner.setSelection(i + 1);
    }

    /**
     *  OnItemSelectedListener methods
     */

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        ((TextView)adapterView.getChildAt(0)).setTextColor(activity.getResources().getColor(android.R.color.secondary_text_light_nodisable));
        lastSelect = items.get(i);
        valueCheck();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    /**
     *  ActionValueClient methods
     */

    @Override
    public String getLiveValue() {
        String retValue = Utils.runCommand(command);
        lastLive = retValue;
        return retValue;
    }

    @Override
    public String getSetValue() {
        return lastSelect;
    }

    @Override
    public String getStoredValue() {
        String value = Utils.db.getValue(command);
        if (value == null)
            return null;

        stored = value;
        return value;
    }

    @Override
    public void refreshValue() {
        getLiveValue();
        if (!items.contains(lastLive)) {
            items.add(lastLive);
            labels.add(lastLive + unit);
        }

        if (lastSelect.equals(lastLive))
            return;

        int selection = items.indexOf(lastLive);
        spinner.setSelection(selection);
        lastSelect = lastLive;
    }

    @Override
    public boolean commitValue() {
        Utils.runCommand(command + " " + lastSelect);
        getLiveValue();

        if (!lastLive.equals(stored))
            Utils.db.setValue(command, lastLive);

        lastSelect = stored = lastLive;
        int selection = items.indexOf(lastLive);
        spinner.setSelection(selection);
        valueCheck();

        return true;
    }

    @Override
    public void cancelValue() {
        lastSelect = lastLive = stored;
        commitValue();
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() {
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {

    }
}
