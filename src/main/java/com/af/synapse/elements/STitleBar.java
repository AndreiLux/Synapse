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
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
public class STitleBar extends BaseElement {
    private static Drawable background = null;
    private static int paddingLeft = Integer.MIN_VALUE;
    private static int paddingBottom = Integer.MIN_VALUE;

    public STitleBar(JSONObject element, LinearLayout layout, MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (background == null)
            background = Utils.mainActivity.getResources().getDrawable(R.drawable.holo_gradient_dark_0);

        if (paddingLeft == Integer.MIN_VALUE)
            paddingLeft = (int) (3 * Utils.density + 0.5f);

        if (paddingBottom == Integer.MIN_VALUE)
            paddingBottom = (int) (1 * Utils.density + 0.5f);
    }

    @Override
    public View getView() {
        TextView v;

        if (Utils.useInflater) {
            v = (TextView) LayoutInflater.from(Utils.mainActivity)
                    .inflate(R.layout.template_titlebar, this.layout, false);
            assert v != null;
        } else {
            v = new TextView(Utils.mainActivity);
            v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            v.setTypeface(Typeface.DEFAULT_BOLD);
            v.setTextColor(Color.WHITE);
            v.setBackground(background);
            v.setPadding(paddingLeft, 0, 0, paddingBottom);
        }

        if (element.containsKey("background"))
            v.setBackground(null);

        Object title = element.get("title");
        v.setText(Utils.localise(title));

        return v;
    }
}
