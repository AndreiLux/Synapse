/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONObject;

import java.lang.reflect.Constructor;

/**
 * Created by Andrei on 30/08/13.
 */
public class BaseElement extends ElementSkeleton {
    public BaseElement(JSONObject element, Activity activity, LinearLayout layout) {
        this.element = element;
        this.activity = activity;
        this.layout = layout;
    }

    public static BaseElement createObject(String type, JSONObject element,
                                           Activity activity, LinearLayout layout) {
        BaseElement newObject = null;
        Class<?> c;

        /**
         *  Get the target class object that we want to create. Use the dummy instance
         *  that was statically created to get the package path.
         */

        try {
            c = Class.forName(Utils.packageName + ".elements." + type);
        } catch (Exception e) {
            L.e("Failure to look up dynamic class element " + e.getMessage());
            return null;
        }

        /**
         *  Create the desired constructor parameter types and create a constructor for
         *  the target class object. Initialize a new instance of the new object.
         */

        Class<?>[] types = new Class[] { JSONObject.class, Activity.class , LinearLayout.class };
        try {
            Constructor<?> constructor = c.getConstructor(types);
            newObject = (BaseElement)constructor.newInstance(element, activity, layout);
        } catch (Exception e) {
            L.e("Failure to create dynamic class element " + e.getMessage());
            return null;
        }

        return newObject;
    }

    public View getView() {
        return null;
    }
}