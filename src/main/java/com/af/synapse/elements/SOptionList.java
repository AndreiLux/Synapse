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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionNotification;
import com.af.synapse.lib.ActionValueClient;
import com.af.synapse.lib.ActionValueEvent;
import com.af.synapse.lib.ActionValueNotifierClient;
import com.af.synapse.lib.ActionValueNotifierHandler;
import com.af.synapse.lib.ActionValueUpdater;
import com.af.synapse.lib.ActivityListener;
import com.af.synapse.lib.ElementSelector;
import com.af.synapse.lib.Selectable;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Andrei on 12/09/13.
 */
public class SOptionList extends BaseElement
                         implements AdapterView.OnItemSelectedListener,
                                    View.OnClickListener,
                                    ActionValueNotifierClient,
                                    ActionValueClient,
                                    ActivityListener,
                                    Selectable
{
    private View elementView = null;
    private FrameLayout selectorFrame;

    private Spinner spinner;
    private ImageButton previousButton;
    private ImageButton nextButton;

    private STitleBar titleObj = null;

    private static int dfl_id = View.generateViewId();
    private static int spl_id = View.generateViewId();
    private static int nbl_id = View.generateViewId();
    private static int pbl_id = View.generateViewId();

    private static final int buttonWidth = (int)Utils.mainActivity.getResources().
            getDimension(R.dimen.stepButtons_width);

    private String command;
    private Runnable resumeTask = null;

    List<String> items = new ArrayList<String>();
    List<String> labels = new ArrayList<String>();
    private String unit = "";

    private String original = null;
    private String stored = null;

    private String lastSelect = null;
    private String lastLive = null;

    private boolean onItemSelectedIgnored = false;

    public SOptionList(JSONObject element, LinearLayout layout,
                       MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SOptionList has no action defined");

        if (element.containsKey("unit"))
            this.unit = (String) element.get("unit");

        if (element.containsKey("default"))
            this.original = element.get("default").toString();

        if (element.containsKey("values")) {
            Object values = element.get("values");
            if (values instanceof JSONArray)
                for (Object value : (JSONArray)values) {
                    items.add(value.toString());
                    labels.add(value.toString() + unit);
                }
            else if (values instanceof JSONObject)
                for (Map.Entry<String, Object> set : ((JSONObject) values).entrySet()) {
                    items.add(set.getKey());
                    labels.add(Utils.localise(set.getValue()));
                }
        } else
            throw new IllegalArgumentException("No values given.");

        if (this.original != null && !items.contains(original))
            throw new IllegalArgumentException("Default value not contained in given values");

        if (element.containsKey("title"))
            titleObj = new STitleBar(element, layout, fragment);

        resumeTask = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean tmp = onItemSelectedIgnored;
                    onItemSelectedIgnored = false;
                    refreshValue();
                    onItemSelectedIgnored = tmp;
                } catch (ElementFailureException e) {
                    Utils.createElementErrorView(e);
                }
            }
        };

        ActionValueNotifierHandler.register(this);
    }

    private void prepareUI(){
        elementView = LayoutInflater.from(Utils.mainActivity)
                            .inflate(R.layout.template_optionlist, this.layout, false);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        /**
         *  SOptionList needs to inflate its View inside of the main UI thread because the
         *  spinner spawns a Looper handler which may not exist in auxiliary threads.
         *
         *  We use use a CountDownLatch for inter-thread concurrency.
         */

        final CountDownLatch latch = new CountDownLatch(1);

        LinearLayout descriptionFrame = null;

        Synapse.handler.post(new Runnable() {
            @Override
            public void run() {
                if (Utils.useInflater)
                    prepareUI();
                else
                    spinner = new Spinner(Utils.mainActivity);
                latch.countDown();
            }
        });

        String initialLive = getLiveValue();

        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }

        lastSelect = lastLive;

        if (!Utils.useInflater) {
            RelativeLayout v = new RelativeLayout(Utils.mainActivity);

            descriptionFrame = new LinearLayout(Utils.mainActivity);
            previousButton = new ImageButton(Utils.mainActivity);
            nextButton = new ImageButton(Utils.mainActivity);

            LayoutParams dfl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            final int leftMargin = (int) (2 * Utils.density + 0.5f);
            dfl.setMargins(leftMargin,0,0,0);
            dfl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            descriptionFrame.setOrientation(LinearLayout.VERTICAL);
            descriptionFrame.setLayoutParams(dfl);
            descriptionFrame.setId(dfl_id);

            LayoutParams spl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            spl.addRule(RelativeLayout.CENTER_HORIZONTAL);
            spl.addRule(RelativeLayout.BELOW, dfl_id);
            spl.addRule(RelativeLayout.LEFT_OF, nbl_id);
            spl.addRule(RelativeLayout.RIGHT_OF, pbl_id);

            nextButton.setImageResource(R.drawable.navigation_next_item);
            nextButton.setBackground(null);
            nextButton.setAlpha((float)0.5);
            LayoutParams nbl = new LayoutParams(buttonWidth, LayoutParams.WRAP_CONTENT);
            nbl.addRule(RelativeLayout.ALIGN_TOP, spl_id);
            nbl.addRule(RelativeLayout.ALIGN_BOTTOM, spl_id);
            nbl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            nextButton.setLayoutParams(nbl);
            nextButton.setId(nbl_id);

            previousButton.setImageResource(R.drawable.navigation_previous_item);
            previousButton.setBackground(null);
            previousButton.setAlpha((float)0.5);
            LayoutParams pbl = new LayoutParams(buttonWidth, LayoutParams.WRAP_CONTENT);
            pbl.addRule(RelativeLayout.ALIGN_TOP, spl_id);
            pbl.addRule(RelativeLayout.ALIGN_BOTTOM, spl_id);
            pbl.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            previousButton.setLayoutParams(pbl);
            previousButton.setId(pbl_id);

            try { latch.await(); } catch (InterruptedException ignored) {}
            spinner.setLayoutParams(spl);
            spinner.setId(spl_id);

            v.addView(descriptionFrame);
            v.addView(previousButton);
            v.addView(nextButton);
            v.addView(spinner);

            v.setOnLongClickListener(this);
            spinner.setOnLongClickListener(this);

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
        }

        if (Utils.useInflater)
            try { latch.await(); } catch (InterruptedException ignored) {}

        /**
         *  Nesting another element's view in our own for title and description.
         */

        if (Utils.useInflater)
            descriptionFrame = (LinearLayout) elementView.findViewById(R.id.SOptionList_descriptionFrame);

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

        if (Utils.useInflater) {
            previousButton = (ImageButton) elementView.findViewById(R.id.SOptionList_previousButton);
            nextButton = (ImageButton) elementView.findViewById(R.id.SOptionList_nextButton);
        }
        previousButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);

        /**
         *  The spinner itself
         */

        if (Utils.useInflater)
            spinner = (Spinner) elementView.findViewById(R.id.SOptionList_spinner);

        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(Utils.mainActivity,
                                                R.layout.template_optionlist_main_item, labels);
        adapter.setDropDownViewResource(R.layout.template_optionlist_list_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(items.indexOf(lastLive));
        valueCheck();

        return elementView;
    }

    private void valueCheck() {
        if (lastSelect.equals(lastLive) && lastSelect.equals(stored)) {
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
        int i = spinner.getSelectedItemPosition();

        /* Keep in mind that the top item is of lower index */

        if (view == nextButton && i > 0)
            spinner.setSelection(i - 1);

        if (view == previousButton && i < (items.size() - 1))
            spinner.setSelection(i + 1);
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
     *  OnItemSelectedListener methods
     */

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (onItemSelectedIgnored) {
            onItemSelectedIgnored = false;
            spinner.setSelection(items.indexOf(lastSelect));
        } else {
            ((TextView) adapterView.getChildAt(0)).setTextColor(Utils.mainActivity.getResources().getColor(android.R.color.secondary_text_light_nodisable));
            lastSelect = items.get(i);
            valueCheck();
            ActionValueNotifierHandler.propagate(this, ActionValueEvent.SET);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

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
            lastLive = retValue;
            return retValue;
        } catch (Exception e) { throw new ElementFailureException(this, e); }
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
    public void refreshValue() throws ElementFailureException {
        getLiveValue();
        if (!items.contains(lastLive)) {
            items.add(lastLive);
            labels.add(lastLive + unit);
        }

        if (lastSelect.equals(lastLive))
            return;

        final int selection = items.indexOf(lastLive);
        lastSelect = lastLive;

        Synapse.handler.post(new Runnable() {
            @Override
            public void run() {
                spinner.setSelection(selection);
                valueCheck();
            }
        });

        ActionValueNotifierHandler.propagate(this, ActionValueEvent.REFRESH);
    }

    @Override
    public void setDefaults() {
        if (original != null) {
            spinner.setSelection(items.indexOf(original));
            valueCheck();
            ActionValueNotifierHandler.propagate(this, ActionValueEvent.RESET);
        }
    }

    private boolean commitValue() throws ElementFailureException {
        try {
            Utils.runCommand(command + " " + lastSelect);
        } catch (Exception e) { throw new ElementFailureException(this, e); }

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
    public void applyValue() throws ElementFailureException {
        commitValue();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.APPLY);
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastSelect = lastLive = stored;
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
        onItemSelectedIgnored = true;
    }

    int resumeCount = 0;

    @Override
    public void onResume() {
        /*
         * This may very well be the most idiotic and unbelieveable hack in the whole of the app.
         * Re: Spinner resumes twice in a row when fragments jump beyond the nearest neighbour,
         * Causing the spinner to again reset to the last Spinner's index inside of the fragment.
         * Fuck everything about Google's asinine framework.
         */
        if (++resumeCount > 1)
            onItemSelectedIgnored = !onItemSelectedIgnored;
        else
            onItemSelectedIgnored = false;
    }

    @Override
    public void onPause() {
        resumeCount = 0;
        onItemSelectedIgnored = true;
    }
}
