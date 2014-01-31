/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 12/01/14.
 */
public class SLiveLabel extends BaseElement implements ActivityListener {
    private STitleBar titleObj = null;
    private SDescription descriptionObj = null;

    private TextView liveLabel;

    private final String command;
    private String style = null;

    private int refreshInterval = 2500;
    private Runnable runTask = null;

    public SLiveLabel(JSONObject element, LinearLayout layout) {
        super(element, layout);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SSeekBar has no action defined");

        if (element.containsKey("refresh"))
            refreshInterval = Math.max(50, (Integer) element.get("refresh"));

        if (element.containsKey("style"))
            style = (String) element.get("style");

        runTask = new Runnable() {
            @Override
            public void run() {
                liveLabel.setText(Utils.runCommand(command).replace("@n", "\n"));
                Synapse.handler.postDelayed(this, refreshInterval);
            }
        };

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
        LinearLayout v = (LinearLayout) LayoutInflater.from(Utils.mainActivity)
                .inflate(R.layout.template_livelabel, this.layout, false);
        assert v != null;

        /**
         *  Nesting another element's view in our own for title and description.
         */

        LinearLayout descriptionFrame = (LinearLayout) v.findViewById(R.id.SLiveLabel_descriptionFrame);

        if (titleObj != null) {
            TextView titleView = (TextView)titleObj.getView();
            titleView.setBackground(null);
            descriptionFrame.addView(titleView);
        }

        if (descriptionObj != null)
            descriptionFrame.addView(descriptionObj.getView());

        liveLabel = (TextView) v.findViewById(R.id.SLiveLabel_textView);
        liveLabel.setText(Utils.runCommand(command));

        if (style != null) {
            if (style.contains("bold") && style.contains("italic")) {
                liveLabel.setTypeface(liveLabel.getTypeface(), Typeface.BOLD_ITALIC);
                return v;
            }

            if (style.contains("bold"))
                liveLabel.setTypeface(liveLabel.getTypeface(), Typeface.BOLD);

            if (style.contains("italic"))
                liveLabel.setTypeface(liveLabel.getTypeface(), Typeface.ITALIC);
        }

        return v;
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onStart() {
        Synapse.handler.post(runTask);
    }

    @Override
    public void onResume() {
        Synapse.handler.postDelayed(runTask, refreshInterval);
    }

    @Override
    public void onPause() {
        Synapse.handler.removeCallbacks(runTask);
    }

    @Override
    public void onStop() {
        Synapse.handler.removeCallbacks(runTask);
    }
}