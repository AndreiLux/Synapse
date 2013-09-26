/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.widget.LinearLayout;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SPane extends BaseElement {

    public SPane(JSONObject elm, LinearLayout layout) {
        super(elm, layout);

        if (elm.containsKey("title") && !((String)elm.get("title")).isEmpty()) {
            BaseElement titleBar = BaseElement.createObject("STitleBar", elm, layout);
            layout.addView(titleBar.getView());
        }

        if (elm.containsKey("description") && !((String)elm.get("description")).isEmpty()) {
            BaseElement descriptionText = BaseElement.createObject("SDescription", elm, layout);
            layout.addView(descriptionText.getView());
        }
    }
}