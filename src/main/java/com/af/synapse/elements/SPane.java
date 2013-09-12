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
import android.widget.LinearLayout;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SPane extends BaseElement {

    public SPane(JSONObject elm, Activity activity, LinearLayout layout) {
        super(elm, activity, layout);

        if (elm.containsKey("title") && !elm.get("title").toString().isEmpty()) {
            BaseElement titleBar = BaseElement.createObject("STitleBar", elm,
                                                            activity, layout);
            layout.addView(titleBar.getView());
        }

        if (elm.containsKey("description") && !elm.get("description").toString().isEmpty()) {
            BaseElement descriptionText = BaseElement.createObject("SDescription", elm,
                                                                    activity, layout);
            layout.addView(descriptionText.getView());
        }
    }
}