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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.af.synapse.R;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionValueDatabase;

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
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by Andrei on 27/08/13.
 */
public class Utils {
    public static boolean appStarted = false;
    public static ActionValueDatabase db = null;
    public static DecimalFormat df = new DecimalFormat("#.######");
    public static Activity mainActivity;
    public static Activity settingsActivity;
    public static final String CONFIG_CONTEXT = "CONFIGURATION";
    public static JSONArray configSections = null;
    protected static ArrayList<SuperShell> shells = new ArrayList<SuperShell>();
    public static String locale = "en";
    public static InputMethodManager imm;
    public static float density;

    public static boolean useInflater = false;

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

    public static String runCommand(String command) throws RootFailureException, RunCommandFailedException {
        return runCommandWithException(command, false);
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

    public static String getEnclosure(JSONObject object) {
        return object.keySet().toString().replace("[", "").replace("]", "");
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

    public static boolean hasSoftKeys(WindowManager windowManager){
        Display d = windowManager.getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        return (realDisplayMetrics.widthPixels - displayMetrics.widthPixels) > 0 ||
               (realDisplayMetrics.heightPixels - displayMetrics.heightPixels) > 0;
    }

    public static Bitmap drawableToBitmap (WindowManager wm, Drawable drawable) {
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Bitmap bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static View createElementErrorView(ElementFailureException e) {
        final LinearLayout v = (LinearLayout) LayoutInflater.from(Utils.mainActivity)
                                .inflate(R.layout.element_failure, null, false);
        assert v != null;

        ((TextView)v.findViewById(R.id.element_failure_title))
                .setText(e.getSourceClass() + " has failed:");

        ((TextView)v.findViewById(R.id.element_failure_description))
                .setText(e.getMessage());

        e.printStackTrace();

        try {
            final View child = e.getSource().getView();
            final LinearLayout section = (LinearLayout) child.getParent();

            if (section != null) {
                final int position = section.indexOfChild(child);
                Synapse.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        section.removeView(child);
                        section.addView(v, position);
                    }
                });
                return null;
            }
        } catch (Exception ignored) {}

        return v;
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
    private static final int callbackLength = callback.length();

    private static String actionPath = null;
    Pattern pattern = Pattern.compile("[\n\r]+");

    public final AtomicInteger lock = new AtomicInteger(0);
    public final CountDownLatch rootLatch = new CountDownLatch(1);

    public SuperShell() throws RootFailureException, RunCommandFailedException {
        try {
            this.rp = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            throw new RootFailureException(e.getMessage());
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
                    this.rp.exitValue();
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
        int i;

        try {
            /* Throws exception if not terminated */
            rp.exitValue();

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

        StringBuilder sb = new StringBuilder("");

        try {
            co.write(command + "\necho " + callback + "\n");
            co.flush();

            char[] buffer = new char[bigOutput ? 8192 : 32];
            while (true) {
                i = is.read(buffer);
                sb.append(buffer, 0, i);
                if ((i = sb.indexOf(callback)) > -1) {
                    sb.delete(i, i + callbackLength);
                    break;
                }
            }

            flushError();
        } catch (IOException ex) {
            throw new RunCommandFailedException(ex.getMessage());
        }

        return pattern.matcher(sb.toString()).replaceAll("");
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