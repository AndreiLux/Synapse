/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SDescription extends BaseElement{

    public SDescription(JSONObject element, LinearLayout layout) {
        super(element, layout);
    }

    @Override
    public View getView() {
        TextView v = (TextView) LayoutInflater.from(Utils.mainActivity)
                                     .inflate(R.layout.template_description, this.layout, false);
        assert v != null;

        Object description = this.element.get("description");

        String content = Utils.localise(description);
        if (content.contains("href=")) {
            v.setClickable(true);
            v.setMovementMethod(LinkMovementMethod.getInstance());
            v.setText(Html.fromHtml(content));
        } else
            v.setText(content);

        return v;
    }
}
