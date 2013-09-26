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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andrei on 27/08/13.
 */
public class Utils {
    private static boolean initialized = false;
    public static boolean appStart = true;

    public static ActionValueDatabase db = null;
    public static String packageName = null;
    public static DecimalFormat df = new DecimalFormat("#.######");
    public static Activity mainActivity;

    public static JSONArray configSections = null;

    protected static ArrayList<SuperShell> shells = new ArrayList<SuperShell>();

    public static boolean isUciSupport() {
        return true; //TODO rewrite properly
    }

    public static String runCommand(String command, boolean bigOutput) {
        SuperShell shell = null;
        for (SuperShell s : shells) {
            if (s.lock.get() == 0) {
                shell = s;
                break;
            }
        }

        if (shell == null)
            shell = new SuperShell();

        shell.lock.set(1);
        String ret = shell.runCommand(command, bigOutput);
        shell.lock.set(0);

        return ret;
    }

    public static String runCommand(String command) {
        return runCommand(command, false);
    }

    public static JSONObject getJSON() {
        JSONObject result;

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

        return result;
    }

    public static void initiateDatabase(Context context) {
        db = new ActionValueDatabase(context);
        db.createDataBase();
    }

    public static void setPackageName(String name) {
        packageName = name;
    }

    public static void loadSections() {
        if (Utils.configSections == null) {
            JSONObject resultsJSONObject = Utils.getJSON();
            Utils.configSections = (JSONArray)resultsJSONObject.get("sections");
        }
    }

    public static void destroy() {
        for (SuperShell s : shells)
            s.destroy();

        shells.clear();
        db.close();
    }
}

class SuperShell {
    private Process rp = null;

    private BufferedWriter co = null;
    private BufferedReader ci = null;
    private BufferedReader ce = null;

    private OutputStreamWriter os = null;
    private InputStreamReader is = null;
    private InputStreamReader es = null;

    private static final int MAX_ROOT_TIMEOUT_MS = 40000;
    private static final String callback = "/shellCallback/";

    private static String actionPath = null;

    public final AtomicInteger lock = new AtomicInteger(0);

    public SuperShell() {
        L.d("New SuperShell");
        try {
            this.rp = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        os = new OutputStreamWriter(rp.getOutputStream());
        this.co = new BufferedWriter(os);

        is = new InputStreamReader(rp.getInputStream());
        this.ci = new BufferedReader(is);

        es = new InputStreamReader(rp.getErrorStream());
        this.ce = new BufferedReader(es);

        Utils.shells.add(this);

        if (actionPath == null)
            actionPath = Utils.runCommand("uci actionpath");

        this.runCommand("export PATH=" + actionPath + ":$PATH", false);
        this.isRoot();
    }

    public boolean isRoot() {
        String line = "";

        try {
            long timeStart = 0;

            co.write("echo " + callback + "\n");
            co.flush();

            while (true) {
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

    private boolean flushError()
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

    public synchronized String runCommand(String command, boolean bigOutput) {
        String line;
        StringBuilder sb = null;
        String out = "";

        if (command == null || command.isEmpty())
            return null;

        if (bigOutput)
            sb = new StringBuilder(out);

        try {
            boolean finished = false;

            co.write(command + "\necho " + callback + "\n");
            co.flush();

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
        } catch (IOException ex) {
            L.e("Running command failed: " + command);
        }

        if (bigOutput)
            out = sb.toString();

        return out;
    }

    public void destroy() {
        try {
            if (rp != null) {
                co.write("exit\n");
                co.flush();
                try {
                    rp.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                L.d("Exited shell with " + rp.exitValue());
            }
        } catch (IOException ignored) {}
    }
}