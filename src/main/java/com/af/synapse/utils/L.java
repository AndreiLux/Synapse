/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

import android.util.Log;

import com.af.synapse.BuildConfig;

/**
 * Created by Andrei on 27/08/13.
 */
public class L {
    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.d(stackTraceElements[4].getMethodName() + "->" +
                    stackTraceElements[3].getMethodName() + " ",
                    message);
        }
    }

    public static void v(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.v(stackTraceElements[4].getMethodName() + "->" +
                    stackTraceElements[3].getMethodName() + " ",
                    message);
        }
    }

    public static void i(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.i(stackTraceElements[4].getMethodName() + "->" +
                    stackTraceElements[3].getMethodName() + " ",
                    message);
        }
    }

    public static void w(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.w(stackTraceElements[4].getMethodName() + "->" +
                    stackTraceElements[3].getMethodName() + " ",
                    message);
        }
    }

    public static void e(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            Log.e(stackTraceElements[4].getMethodName() + "->" +
                    stackTraceElements[3].getMethodName() + " ",
                    message);
        }
    }
}
