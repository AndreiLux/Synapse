/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

/**
 * Created by Andrei on 03/09/13.
 */
public class ContextSwitcher {
    private static final String globalContext = "global";
    private static String context = globalContext;

    public static String getContext() {
        return context;
    }
}
