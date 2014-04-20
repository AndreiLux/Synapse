/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

import com.af.synapse.utils.ElementFailureException;

/**
 * Created by Andrei on 02/09/13.
 */
public interface ActionValueClient {
    public String getLiveValue() throws ElementFailureException;
    public String getSetValue();
    public String getStoredValue();

    public void refreshValue() throws ElementFailureException;
    public void setDefaults();
    public void applyValue() throws ElementFailureException;
    public void cancelValue() throws ElementFailureException;
}
