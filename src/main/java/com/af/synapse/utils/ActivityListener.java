/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

/**
 * Created by Andrei on 05/09/13.
 */
public interface ActivityListener {
    public void onStart();
    public void onResume();
    public void onPause();
    public void onStop();
}
