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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.af.synapse.utils.L;
import com.af.synapse.R;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 31/08/13.
 */
public class SButton extends BaseElement implements View.OnClickListener {
    private Button button;
    private String command;

    public SButton(JSONObject element, Activity activity, LinearLayout layout) {
        super(element, activity, layout);

        if (element.containsKey("action"))
            this.command = element.get("action").toString();
        else
            L.w("Button without action detected!");
    }

    @Override
    public View getView() {
        this.button = (Button) LayoutInflater.from(this.activity)
                                            .inflate(R.layout.template_button, this.layout, false);

        if (this.element.containsKey("label"))
            this.button.setText(this.element.get("label").toString());

        this.button.setOnClickListener(this);

        return this.button;
    }

    @Override
    public void onClick(View view) {
        String result = Utils.runCommand(command);
        Toast.makeText(this.activity, result, Toast.LENGTH_LONG).show();
    }
}