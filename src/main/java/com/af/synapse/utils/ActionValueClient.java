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
 * Created by Andrei on 02/09/13.
 */
public interface ActionValueClient {
    public String getLiveValue();
    public String getSetValue();
    public String getStoredValue();

    public void refreshValue();
    public boolean commitValue();
    public void cancelValue();
}
