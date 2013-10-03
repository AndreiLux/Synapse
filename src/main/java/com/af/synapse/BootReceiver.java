/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 23/09/13.
 */
public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Utils.isUciSupport())
            return;

        Utils.initiateDatabase(context);
        Utils.loadSections();

        for (Object section : Utils.configSections) {
            JSONArray sectionElements = (JSONArray)((JSONObject)section).get("elements");
            for (Object sectionElement : sectionElements) {
                JSONObject elm = (JSONObject) sectionElement;
                String type = elm.keySet().toString().replace("[", "").replace("]", "");
                JSONObject parameters = (JSONObject) elm.get(type);
                if (parameters.containsKey("action")) {
                    String command = (String) parameters.get("action");
                    String value = Utils.db.getValue(command);
                    Utils.runCommand(command + " \"" + value + "\"");
                }
            }
        }

        Toast.makeText(context, "Synapse boot completed", Toast.LENGTH_LONG).show();
    }
}
