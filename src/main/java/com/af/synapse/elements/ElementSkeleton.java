/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.view.View;
import android.widget.LinearLayout;

import com.af.synapse.MainActivity;
import com.af.synapse.utils.ElementFailureException;

import net.minidev.json.JSONObject;

/**
 * Created by Andrei on 30/08/13.
 */
abstract class ElementSkeleton {
    public JSONObject element;
    public LinearLayout layout;
    public MainActivity.TabSectionFragment fragment;

    abstract public View getView() throws ElementFailureException;
}
