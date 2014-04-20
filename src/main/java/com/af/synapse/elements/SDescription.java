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
import android.graphics.Typeface;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.utils.LinkMovementMethod;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class SDescription extends BaseElement{
    private TextView v = null;

    public SDescription(JSONObject element, LinearLayout layout,
                        MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);
    }

    @Override
    public View getView() {
        if (v != null)
            return v;

        if (Utils.useInflater) {
            v = (TextView) LayoutInflater.from(Utils.mainActivity)
                    .inflate(R.layout.template_description, this.layout, false);
            assert v != null;
        } else {
            v = new TextView(Utils.mainActivity);
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            v.setGravity(Gravity.FILL_HORIZONTAL);
            v.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
            v.setTextColor(Color.parseColor("#AAAAAA"));
        }
        if (!this.element.containsKey("description"))
            return v;

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
