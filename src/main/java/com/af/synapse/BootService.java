/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 04/10/13.
 */
public class BootService extends Service {
    public static final String BOOT_STABLE = "BOOT_IS_STABLE";
    public static final int BOOT_STABILITY_DELAY = 120;
    public static final int BOOT_FLAG_APPLY_DELAY = 20;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void stopSelf2() {
        L.d("Stopping service");
        stopSelf();
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (!Synapse.isValidEnvironment)
            return START_NOT_STICKY;

        if (!getBootFlag())
            return START_NOT_STICKY;

        setBootFlag(false);

        for (Object section : Utils.configSections) {
            JSONArray sectionElements = (JSONArray)((JSONObject)section).get("elements");
            for (Object sectionElement : sectionElements) {
                JSONObject elm = (JSONObject) sectionElement;
                String type = elm.keySet().toString().replace("[", "").replace("]", "");

                if (type.equals("SButton"))
                    continue;

                JSONObject parameters = (JSONObject) elm.get(type);
                if (parameters.containsKey("action")) {
                    String command = (String) parameters.get("action");
                    String value = Utils.db.getValue(command);
                    Utils.runCommand(command + " \"" + value + "\"");
                }
            }
        }

        setBootFlag(BOOT_STABILITY_DELAY, this);
        return START_NOT_STICKY;
    }

    public static void setBootFlag(int delaySeconds) {
        setBootFlag(delaySeconds, null);
    }

    public static void setBootFlag(int delaySeconds, final BootService callingService) {
        Runnable stabilityFlagSetter = new Runnable() {
            public void run() {
                setBootFlag(true);
                L.i("Synapse settings deemed stable");
                if(callingService != null)
                    callingService.stopSelf2();
            }
        };

        Synapse.handler.postDelayed(stabilityFlagSetter, delaySeconds * 1000);
    }

    public static void setBootFlag(boolean flag) {
        Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_STABLE, String.valueOf(flag));
    }

    public static boolean getBootFlag() {
        String flag = Utils.db.getValue(Utils.CONFIG_CONTEXT, BOOT_STABLE);

        return (flag != null && Boolean.valueOf(flag));
    }
}
