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

import com.af.synapse.Synapse;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Andrei on 27/08/13.
 */
public class Utils {
    public static boolean appStarted = false;
    public static ActionValueDatabase db = null;
    public static DecimalFormat df = new DecimalFormat("#.######");
    public static Activity mainActivity;
    public static final String CONFIG_CONTEXT = "CONFIGURATION";
    public static JSONArray configSections = null;
    protected static ArrayList<SuperShell> shells = new ArrayList<SuperShell>();
    public static String locale = "en";

    public static boolean isUciSupport() throws RunCommandFailedException, RootFailureException {
        runCommandWithException("uci", false);
        return true;
    }

    public static String runCommand(String command, boolean bigOutput) {
        try {
            return runCommandWithException(command, bigOutput);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String runCommandWithException(String command, boolean bigOutput)
        throws RunCommandFailedException, RootFailureException {
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
            res = Utils.runCommandWithException("uci config", true);
        } catch (Exception e) {
            L.e("Can't access live customconfig: " + e.getMessage());
            return null;
        }

        try {
        	result = (JSONObject) JSONValue.parse(res);
        } catch (ClassCastException e) {
            L.e(e.getMessage());
            return null;
        }

        return result;
    }

    public static void initiateDatabase() {
        if (db != null) return;

        db = new ActionValueDatabase();
        db.createDataBase();
    }

    public static void loadSections() {
        if (Utils.configSections == null) {
            JSONObject resultsJSONObject = Utils.getJSON();
            if (resultsJSONObject == null) {
                Synapse.currentEnvironmentState = Synapse.environmentState.JSON_FAILURE;
                return;
            }
            Utils.configSections = (JSONArray)resultsJSONObject.get("sections");
        }
    }

    public static void destroy() {
        for (SuperShell s : shells)
            s.destroy();

        shells.clear();
        if (db != null) {
            db.close();
            db = null;
        }

        appStarted = false;
    }

    public static String localise(Object textObject) {
        if (textObject instanceof String) {
            return (String) textObject;
        } else if (textObject instanceof JSONObject) {
            JSONObject object = (JSONObject) textObject;
            String localeKey;

            if (object.containsKey(Utils.locale))
                localeKey = Utils.locale;
            else if (object.containsKey(Utils.locale.substring(0, 2)))
                localeKey = Utils.locale.substring(0, 2);
            else
                localeKey = "en";

            return (String) object.get(localeKey);
        }

        return null;
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

    private static final int MAX_ROOT_TIMEOUT_MS = 120000;
    private static final String callback = "/shellCallback/";

    private static String actionPath = null;

    public final AtomicInteger lock = new AtomicInteger(0);
    public final CountDownLatch rootLatch = new CountDownLatch(1);

    public SuperShell() throws RootFailureException, RunCommandFailedException {
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

        this.isRoot();

        if (actionPath == null)
            actionPath = this.runCommand("uci actionpath", false);

        this.runCommand("export PATH=" + actionPath + ":$PATH", false);

        Utils.shells.add(this);
    }

    public boolean isRoot() throws RootFailureException, RunCommandFailedException {
        String line = "";

        try {
            long timeStart = 0;

            co.write("echo " + callback + "\n");
            co.flush();

            while (true) {
                try {
                    /* Throws exception if not terminated */
                    int exitValue = this.rp.exitValue();
                    throw new RootFailureException("Root permission revoked.");
                } catch (IllegalThreadStateException ignored) {}

                if (ci.ready()) {
                    if ((line = ci.readLine()) != null && line.equalsIgnoreCase(callback)) {
                        rootLatch.countDown();
                        return true;
                    }
                } else {
                    flushError();
                    if (timeStart == 0) {
                        timeStart = System.currentTimeMillis();
                        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    } else if (System.currentTimeMillis() > timeStart + MAX_ROOT_TIMEOUT_MS) {
                        throw new RootFailureException("Root test timeout.");
                    }
                }
            }
        } catch (IOException e) {
            throw new RootFailureException(e.getMessage());
        }
    }

    private void flushError() throws RunCommandFailedException
    {
        String lineErr = "";
        boolean ret = false;

        try {
            while (ce.ready()) {
                if (((lineErr += ce.readLine()) != null) && !lineErr.isEmpty())
                    ret |= true;
            }
        } catch (IOException e) {
            throw new RunCommandFailedException(e.getMessage());
        }

        if (ret)
            throw new RunCommandFailedException(lineErr);
    }

    public synchronized String runCommand(String command, boolean bigOutput)
            throws RunCommandFailedException, RootFailureException {
        String line;
        StringBuilder sb = null;
        String out = "";

        try {
            /* Throws exception if not terminated */
            int exitValue = rp.exitValue();

            Utils.shells.remove(this);
            SuperShell newShell = new SuperShell();
            newShell.lock.set(1);
            String result = newShell.runCommand(command, bigOutput);
            newShell.lock.set(0);
            return result;
        } catch (IllegalThreadStateException ignored) {}

        if (command == null || command.isEmpty())
            return null;

        try {
            rootLatch.await();
        } catch (InterruptedException e) {
            throw new RootFailureException(e.getMessage());
        }

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
                } else flushError();
            }

            flushError();
        } catch (IOException ex) {
            throw new RunCommandFailedException(ex.getMessage());
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