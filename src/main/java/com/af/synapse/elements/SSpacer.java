package com.af.synapse.elements;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.af.synapse.MainActivity;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 26/03/14.
 */
public class SSpacer extends BaseElement {
    public SSpacer(JSONObject element, LinearLayout layout, MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        View v = layout.getChildAt(layout.getChildCount() - 1);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

        int height = 1;
        if (element.containsKey("height"))
            height = (Integer) element.get("height");

        lp.bottomMargin += (int) (12 * height * Utils.density + 0.5f);
    }
}
