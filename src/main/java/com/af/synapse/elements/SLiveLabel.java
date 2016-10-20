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

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActivityListener;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.RootFailureException;
import com.af.synapse.utils.RunCommandFailedException;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 12/01/14.
 */
public class SLiveLabel extends BaseElement implements ActivityListener {
    private View elementView = null;

    private STitleBar titleObj = null;

    private TextView liveLabel;

    private final String command;
    private String style = null;

    private int refreshInterval = 2500;
    private Runnable resumeTask = null;

    public SLiveLabel(JSONObject element, LinearLayout layout,
                      MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (element.containsKey("action"))
            this.command = (String) element.get("action");
        else
            throw new IllegalArgumentException("SSeekBar has no action defined");

        if (element.containsKey("refresh")) {
            refreshInterval = (Integer) element.get("refresh");
            if (refreshInterval != 0 && refreshInterval < 50)
                refreshInterval = 50;
        }

        if (element.containsKey("style"))
            style = (String) element.get("style");

        resumeTask = new Runnable() {
            @Override
            public void run() {
                try {
                    liveLabel.setText(Utils.runCommand(command).replace("@n", "\n"));
                    if (refreshInterval > 0)
                        Synapse.handler.postDelayed(this, refreshInterval);
                } catch (Exception e) {
                    liveLabel.setText(e.getMessage());
                }
            }
        };

        if (element.containsKey("title"))
            titleObj = new STitleBar(element, layout, fragment);
    }

    @Override
    public View getView() throws ElementFailureException {
        if (elementView != null)
            return elementView;

        LinearLayout v = (LinearLayout) LayoutInflater.from(Utils.mainActivity)
                .inflate(R.layout.template_livelabel, this.layout, false);

        assert v != null;
        elementView = v;

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

        try {
            liveLabel.setText(Utils.runCommand(command).replace("@n", "\n"));
        } catch (Exception e) {
            throw new ElementFailureException(this, e);
        }

        return v;
    }

    /**
     *  ActivityListener methods
     */

    @Override
    public void onMainStart() {}

    @Override
    public void onStart() {}

    @Override
    public void onResume() {
        if (refreshInterval > 0)
            Synapse.handler.postDelayed(resumeTask, refreshInterval);
    }

    @Override
    public void onPause() {
        Synapse.handler.removeCallbacks(resumeTask);
    }
}