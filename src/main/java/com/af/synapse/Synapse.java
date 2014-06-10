/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.af.synapse.utils.RootFailureException;
import com.af.synapse.utils.RunCommandFailedException;
import com.af.synapse.utils.Utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Andrei on 04/10/13.
 */
public class Synapse extends Application {
    private static Context context;
    public static Handler handler;
    public static ThreadPoolExecutor executor;
    public static BlockingQueue<Runnable> threadWorkQueue;

    public static int NR_CORES;

    public enum environmentState {
        VALID_ENVIRONMENT,
        ROOT_FAILURE,
        UCI_FAILURE,
        JSON_FAILURE,
        UNINITIALIZED
    }

    public static environmentState currentEnvironmentState = environmentState.UNINITIALIZED;

    public void onCreate(){
        super.onCreate();

        Synapse.context = getApplicationContext();
        Synapse.handler = new Handler(Looper.getMainLooper());
        Utils.initiateDatabase();

        assert context.getResources().getConfiguration().locale != null;
        Utils.locale = context.getResources().getConfiguration().locale.toString();

        try {
            Utils.isUciSupport();
            currentEnvironmentState = environmentState.VALID_ENVIRONMENT;
        } catch (RootFailureException e) {
            currentEnvironmentState = environmentState.ROOT_FAILURE;
        } catch (RunCommandFailedException e) {
            currentEnvironmentState = environmentState.UCI_FAILURE;
        }

        if (currentEnvironmentState == environmentState.VALID_ENVIRONMENT)
            Utils.loadSections();
    }

    public static void openExecutor() {
        if (executor != null)
            return;

        NR_CORES = Runtime.getRuntime().availableProcessors();
        threadWorkQueue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(NR_CORES, NR_CORES, 0L, TimeUnit.MILLISECONDS, threadWorkQueue);
    }

    public static void closeExecutor() {
        executor.shutdown();
    }

    public static Context getAppContext() {
        return Synapse.context;
    }

    @Override
    public void onTerminate(){
        super.onTerminate();

        Utils.destroy();
    }
}
