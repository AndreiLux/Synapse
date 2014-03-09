/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Andrei on 07/03/14.
 */
public class SGeneric extends BaseElement
        implements ActionValueClient, ActivityListener, TextView.OnEditorActionListener {
    private View elementView = null;
    private EditText editText;
    private String command;
    private Runnable resumeTask = null;

    private String label;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private Object original = null;
    private Object stored = false;

    private Object lastEdit;
    private Object lastLive;

    public SGeneric(JSONObject element, LinearLayout layout) {
        super(element, layout);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SCheckBox has no action defined");

        if (this.element.containsKey("label"))
            this.label = Utils.localise(element.get("label"));

        if (element.containsKey("default"))
            this.original = element.get("default");

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
                    refreshValue();
                } catch (ElementFailureException e) {
                    Utils.createElementErrorView(e);
                }
            }
        };
    }

    private void prepareUI(){
        elementView = LayoutInflater.from(Utils.mainActivity)
                .inflate(R.layout.template_generic, this.layout, false);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        View v;
        /**
         *  SGeneric needs to inflate its View inside of the main UI thread because the
         *  spinner spawns a Looper handler which may not exist in auxiliary threads.
         *
         *  We use use a CountDownLatch for inter-thread synchronization.
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

        try { latch.await(); } catch (InterruptedException ignored) {}

        v = elementView;
        editText = (EditText) v.findViewById(R.id.SGeneric_editText);

        if (lastLive instanceof Integer)
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) v.findViewById(R.id.SGeneric_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        lastEdit = lastLive;
        editText.setOnEditorActionListener(this);
        editText.setText(lastLive.toString());

        valueCheck();

        elementView = v;
        return v;
    }

    private void valueCheck() {
        if (lastEdit.equals(lastLive) && lastEdit.equals(stored)) {
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
     *  OnKeyListener methods
     */

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE  ||
            event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            editText.clearFocus();
            Utils.imm.hideSoftInputFromWindow(elementView.getWindowToken(), 0);

            String text = v.getText().toString();
            try {
                lastEdit = Integer.parseInt(text.toString());
            } catch (NumberFormatException ignored) {
                lastEdit = text;
            }

            valueCheck();
        }

        return false;
    }

    /**
     *  ActionValueClient methods
     */

    @Override
    public String getLiveValue() throws ElementFailureException {
        try {
            String value = Utils.runCommand(command);
            try {
                lastLive = Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                lastLive = value;
            }
            return value;
        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public String getSetValue() {
        return lastEdit.toString();
    }

    @Override
    public String getStoredValue() {
        String value = Utils.db.getValue(command);
        if (value == null)
            return null;

        try {
            stored = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            stored = value;
        }

        return value;
    }

    @Override
    public void refreshValue() throws ElementFailureException {
        if (Utils.appStarted)
            getLiveValue();

        lastEdit = lastLive;
        Utils.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(lastLive.toString());
                valueCheck();
            }
        });
    }

    @Override
    public void setDefaults() {
        if (original != null) {
            lastLive = original;
            editText.setText(lastLive.toString());
            valueCheck();
        }
    }

    @Override
    public boolean commitValue() throws ElementFailureException {
        try {
            String target = getSetValue();
            Utils.runCommand(command + " " + target);
            String result = getLiveValue();

            if (!result.equals(getStoredValue()))
                Utils.db.setValue(command, result);

            try {
                stored = Integer.parseInt(result);
            } catch (NumberFormatException ignored) {
                stored = result;
            }

            lastLive = lastEdit = stored;
            editText.setText(lastLive.toString());
            valueCheck();

            return true;
        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        lastEdit = lastLive = stored;
        commitValue();
    }


    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() throws ElementFailureException {}

    @Override
    public void onResume() throws ElementFailureException {
        if (!Utils.mainActivity.isChangingConfigurations() && Utils.appStarted)
            Synapse.executor.execute(resumeTask);
    }

    @Override
    public void onPause() throws ElementFailureException {}

    @Override
    public void onStop() throws ElementFailureException {}
}
