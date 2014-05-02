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
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionNotification;
import com.af.synapse.lib.ActionValueEvent;
import com.af.synapse.lib.ActionValueNotifierClient;
import com.af.synapse.lib.ActionValueNotifierHandler;
import com.af.synapse.lib.ActionValueUpdater;
import com.af.synapse.lib.ActivityListener;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

import java.util.ArrayDeque;

/**
 * Created by Andrei on 07/03/14.
 */
public class SGeneric extends BaseElement
        implements ActionValueNotifierClient, ActivityListener,
                   TextView.OnEditorActionListener, View.OnClickListener {
    private LinearLayout elementView = null;
    private TextView textView;
    private static EditText editText;
    private static int tv_id = View.generateViewId();
    private String command;
    private Runnable resumeTask = null;

    private String label;

    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private Object original = null;
    private Object stored = false;

    private Object lastEdit;
    private Object lastLive;

    public SGeneric(JSONObject element, LinearLayout layout,
                    MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

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
            descriptionObj = new SDescription(element, layout, fragment);

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

        LinearLayout v = new LinearLayout(Utils.mainActivity);
        v.setOrientation(LinearLayout.VERTICAL);
        elementView = v;

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame;
        if (Utils.useInflater)
            descriptionFrame = (LinearLayout) v.findViewById(R.id.SGeneric_descriptionFrame);
        else {
            descriptionFrame = new LinearLayout(Utils.mainActivity);
            LayoutParams dfl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            descriptionFrame.setOrientation(LinearLayout.VERTICAL);
            final int leftMargin = (int) (2 * Utils.density + 0.5f);
            dfl.setMargins(leftMargin, 0, 0, 0);
            descriptionFrame.setLayoutParams(dfl);
            ((LinearLayout)elementView).addView(descriptionFrame);
        }

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        textView = new TextView(Utils.mainActivity);
        textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setTextColor(Color.parseColor("#AAAAAA"));
        textView.setId(tv_id);
        elementView.addView(textView);

        String initialLive = getLiveValue();
        if (getStoredValue() == null) {
            Utils.db.setValue(command, initialLive);
            stored = lastLive;
        }

        lastEdit = lastLive;

        if (original == null)
            original = lastLive;

        textView.setOnClickListener(this);
        textView.setText(lastLive.toString());

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
                lastEdit = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                lastEdit = text;
            }

            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
            elementView.removeView(editText);
            valueCheck();
            ActionValueNotifierHandler.propagate(this, ActionValueEvent.SET);
        }

        return false;
    }

    /**
     *  OnClickListener methods
     */

    @Override
    public void onClick(View view) {
        if (editText == null) {
            editText = new EditText(Utils.mainActivity);
            editText.setGravity(Gravity.CENTER);
        }

        if (lastLive instanceof Integer)
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);

        textView.setVisibility(View.GONE);
        if (editText.getParent() != null) {
            ((LinearLayout)editText.getParent()).findViewById(tv_id).setVisibility(View.VISIBLE);
            ((LinearLayout)editText.getParent()).removeView(editText);
        }

        elementView.addView(editText);
        editText.setOnEditorActionListener(this);
        editText.setText(lastEdit.toString());
        editText.requestFocus();
        editText.setSelection(0, editText.getText().length());
        Utils.imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
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

        if (lastEdit.equals(lastLive))
            return;

        lastEdit = lastLive;
        final ActionValueNotifierClient t = this;
        Utils.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(lastLive.toString());
                valueCheck();
                ActionValueNotifierHandler.propagate(t, ActionValueEvent.REFRESH);
            }
        });
    }

    @Override
    public void setDefaults() {
        lastEdit = lastLive = original;
        textView.setText(lastLive.toString());
        if (editText.getParent() != null)
            if (((LinearLayout)editText.getParent()).findViewById(tv_id) == textView)
                editText.setText(lastLive.toString());
        valueCheck();
        ActionValueNotifierHandler.propagate(this, ActionValueEvent.RESET);
    }

    private boolean commitValue() throws ElementFailureException {
        try {
            Utils.runCommand(command + " \"" + lastEdit.toString() + '"');
            String result = getLiveValue();

            if (!result.equals(getStoredValue()))
                Utils.db.setValue(command, result);

            try {
                stored = Integer.parseInt(result);
            } catch (NumberFormatException ignored) {
                stored = result;
            }

            lastLive = lastEdit = stored;
            textView.setText(lastLive.toString());
            if (editText != null && editText.getParent() != null)
                if (((LinearLayout)editText.getParent()).findViewById(tv_id) == textView)
                    editText.setText(lastLive.toString());
            valueCheck();

            return true;
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
        lastEdit = lastLive = stored;
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
