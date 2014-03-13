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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SPane extends BaseElement {

    public SPane(JSONObject elm, LinearLayout layout) throws ElementFailureException {
        super(elm, layout);

        if (elm.containsKey("title")) {
            TextView v = (TextView) LayoutInflater.from(Utils.mainActivity)
                    .inflate(R.layout.template_pane_titlebar, this.layout, false);
            assert v != null;

            v.setText(Utils.localise(element.get("title")));
            layout.addView(v);
        }

        if (elm.containsKey("description")) {
            BaseElement descriptionText = BaseElement.createObject("SDescription", elm, layout);
            layout.addView(descriptionText.getView());
        }
    }
}