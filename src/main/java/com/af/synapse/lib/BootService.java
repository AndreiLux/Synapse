/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.af.synapse.R;
import com.af.synapse.Settings;
import com.af.synapse.Synapse;
import com.af.synapse.elements.SButton;
import com.af.synapse.elements.STreeDescriptor;
import com.af.synapse.elements.SLiveLabel;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 04/10/13.
 */
public class BootService extends Service {
    public static final String BOOT_STABLE = "BOOT_IS_STABLE";
    public static final String BOOT_PENDING = "BOOT_IS_PENDING";
    public static final int BOOT_STABILITY_DELAY = 120;
    public static final int BOOT_FLAG_APPLY_DELAY = 20;

    private static Runnable stabilityFlagSetter;

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
        if (Synapse.currentEnvironmentState != Synapse.environmentState.VALID_ENVIRONMENT)
            return START_NOT_STICKY;

        if (!PreferenceManager.getDefaultSharedPreferences(Synapse.getAppContext())
                .getBoolean(Settings.PREF_BOOT, true))
            return START_NOT_STICKY;

        if (!getBootFlag()) {
            Toast.makeText(Synapse.getAppContext(), R.string.boot_service_cancel, Toast.LENGTH_LONG).show();
            Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_PENDING, String.valueOf(false));
            return START_NOT_STICKY;
        }

        final boolean useProbationPeriod = PreferenceManager.getDefaultSharedPreferences(Synapse.getAppContext())
                .getBoolean(Settings.PREF_PROBATION, true);

        if(useProbationPeriod) {
            setBootFlag(false);
            Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_PENDING, String.valueOf(true));
        }

        for (Object section : Utils.configSections) {
            JSONArray sectionElements = (JSONArray)((JSONObject)section).get("elements");
            for (Object sectionElement : sectionElements) {
                JSONObject elm = (JSONObject) sectionElement;
                String type = Utils.getEnclosure(elm);

                if (type.equals(SButton.class.getSimpleName()) ||
                    type.equals(SLiveLabel.class.getSimpleName()))
                    continue;

                if (type.equals(STreeDescriptor.class.getSimpleName())) {
                    STreeDescriptor sdp = new STreeDescriptor((JSONObject) elm.get(type), null, null);

                    for (String command : sdp.getFlatActionTreeList()) {
                        String value = Utils.db.getValue(command);
                        try {
                            Utils.runCommand(command + " \"" + value + "\"");
                        } catch (Exception e) {
                            L.e(e.getMessage());
                        }
                    }
                    continue;
                }

                JSONObject parameters = (JSONObject) elm.get(type);
                if (parameters.containsKey("action")) {
                    String command = (String) parameters.get("action");
                    String value = Utils.db.getValue(command);
                    try {
                        Utils.runCommand(command + " \"" + value + "\"");
                    } catch (Exception e) {
                        L.e(e.getMessage());
                    }
                }
            }
        }

        Toast.makeText(Synapse.getAppContext(), R.string.boot_service_complete, Toast.LENGTH_LONG).show();

        if(useProbationPeriod) {
            setBootFlag(BOOT_STABILITY_DELAY, this);
        } else {
            L.i("Synapse settings automatically treated as stable due to user preference.");
            setBootFlag(true);
            Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_PENDING, String.valueOf(false));
            stopSelf2();
        }

        return START_NOT_STICKY;
    }

    public static void setBootFlag(int delaySeconds, final BootService callingService) {
        Synapse.handler.removeCallbacks(stabilityFlagSetter);

        stabilityFlagSetter = new Runnable() {
            public void run() {
                setBootFlag(true);
                L.i("Synapse settings deemed stable.");

                if (callingService != null) {
                    Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_PENDING, String.valueOf(false));
                    callingService.stopSelf2();
                }
            }
        };

        Synapse.handler.postDelayed(stabilityFlagSetter, delaySeconds * 1000);
    }

    public static void setBootFlag(boolean flag) {
        Utils.db.setValue(Utils.CONFIG_CONTEXT, BOOT_STABLE, String.valueOf(flag));
    }

    private static boolean getFlag(String flag) {
        flag = Utils.db.getValue(Utils.CONFIG_CONTEXT, flag);

        return (flag != null && Boolean.valueOf(flag));
    }

    public static boolean getBootFlag() {
        return getFlag(BOOT_STABLE);
    }

    public static boolean getBootFlagPending() {
        return getFlag(BOOT_PENDING);
    }
}
