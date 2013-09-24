/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;

/**
 * Created by Andrei on 27/08/13.
 */
public class Utils {
    private static Process rp = null;

    private static BufferedWriter co = null;
    private static BufferedReader ci = null;
    private static BufferedReader ce = null;

    private static boolean initialized = false;
    public static boolean appStart = true;

    public static final int MAX_ROOT_TIMEOUT_MS = 40000;
    private static final String callback = "/shellCallback/";

    public static ActionValueDatabase db = null;
    public static String packageName = null;
    public static DecimalFormat df = new DecimalFormat("#.######");
    public static Activity mainActivity;

    // TODO FIXME
    private static long total_time_shell = 0;

    private static void initialize() {
        if (!initialized) {
            try {
                Utils.rp = Runtime.getRuntime().exec("su");

                Utils.co = new BufferedWriter(new OutputStreamWriter(rp.getOutputStream()));
                Utils.ci = new BufferedReader(new InputStreamReader(rp.getInputStream()));
                Utils.ce = new BufferedReader(new InputStreamReader(rp.getErrorStream()));

                initialized = isRoot();
                initializeActionPath();
            } catch (IOException e) {
                Utils.rp = null;
                Utils.ci = null;
                Utils.co = null;
                Utils.ce = null;

                e.printStackTrace();
            }
        }
    }

    public static boolean isUciSupport() {
        return true; //TODO rewrite properly
    }

    private static boolean flushError()
    {
        String lineErr;
        boolean ret = false;

        try {
            while (ce.ready()) {
                if (((lineErr = ce.readLine()) != null) && !lineErr.isEmpty()) {
                    L.e(lineErr);
                    ret |= true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public synchronized static boolean isRoot() {
        String line = "";

        try {
            boolean finished = false;
            long timeStart = 0;

            co.write("echo " + callback + "\n");
            co.flush();

            while (!finished) {
                if (ci.ready()) {
                    if ((line = ci.readLine()) != null && line.equalsIgnoreCase(callback))
                        return true;
                } else if (flushError())
                    return false;
                else {
                    if (timeStart == 0) {
                        timeStart = System.currentTimeMillis();
                        try { Thread.sleep(10); } catch (InterruptedException e) {}
                    } else if (System.currentTimeMillis() > timeStart + MAX_ROOT_TIMEOUT_MS) {
                        L.e("Root test timeout");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            L.e("Root test fail" + e.getMessage());
        }

        return false;
    }

    public synchronized static String runCommand(String command, boolean bigOutput) {
        String line;
        StringBuilder sb = null;
        String out = "";
        long startTime = System.nanoTime();

        if (command == null || command.isEmpty())
            return null;

        if (bigOutput)
            sb = new StringBuilder(out);

        Utils.initialize();

        try {
            boolean finished = false;

            long reponseTime = System.nanoTime();
            co.write(command + "\necho " + callback + "\n");
            co.flush();
            L.d("Command send time:" + (System.nanoTime() - reponseTime));

            reponseTime = System.nanoTime();
            while (!finished) {
                if (ci.ready()) {
                    if ((line = ci.readLine()) == null)
                        finished = true;
                    else {
                        if (line.equals(callback)) {
                            finished = true;
                        } else
                            if (bigOutput)
                                sb.append(line);
                            else
                                out += line;
                    }
                } else if(flushError())
                    break;
            }

            flushError();
            L.d("Command response time:" + (System.nanoTime() - reponseTime));
        } catch (IOException ex) {
            L.e("Running command failed: " + command);
        }

        if (bigOutput)
            out = sb.toString();

        long command_time = System.nanoTime() - startTime;
        total_time_shell += command_time;
        L.d("Shell time:\t" + total_time_shell + " / " + command_time);

        return out;
    }


    public synchronized static String runCommand(String command) {
        return runCommand(command, false);
    }

    public static JSONObject getJSON(AssetManager assets) {
        JSONObject result;
        InputStream is = null;

        if (false) {
            try {
                is = assets.open("customconfig2.json");
            } catch (Exception e) {
                L.e("Can't access asset customconfig: " + e.getMessage());
            }
            L.i("Parsing JSON");
            result = (JSONObject) JSONValue.parse(is);
            L.i("Parsed JSON");
        } else {
            String res = null;
            try {
                L.i("Requesting JSON");
                res = Utils.runCommand("uci config", true);
                L.i("Retrieved JSON");
            } catch (Exception e) {
                L.e("Can't access live customconfig: " + e.getMessage());
            }
            L.i("Parsing JSON");
            result = (JSONObject) JSONValue.parse(res);
            L.i("Parsed JSON");
        }

        return result;
    }

    public static void initializeActionPath() {
        String actionPath = Utils.runCommand("uci actionpath");
        Utils.runCommand("export PATH=" + actionPath + ":$PATH");
    }

    public static void initiateDatabase(Context context) {
        L.i("Creating database instance");
        db = new ActionValueDatabase(context);
        L.i("Creating database");
        db.createDataBase();
    }

    public static void setPackageName(String name) {
        packageName = name;
    }
}
