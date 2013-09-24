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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class STitleBar extends BaseElement {

    public STitleBar(JSONObject element, Activity activity, LinearLayout layout) {
        super(element, activity, layout);
    }

    @Override
    public View getView() {
        TextView v = (TextView) LayoutInflater.from(this.activity)
                                        .inflate(R.layout.template_titlebar, this.layout, false);
        assert v != null;

        String title = (String) element.get("title");
        v.setText(title);

        return v;
    }
}
