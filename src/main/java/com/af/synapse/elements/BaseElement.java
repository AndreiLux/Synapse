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

import com.af.synapse.Synapse;
import com.af.synapse.utils.L;

import net.minidev.json.JSONObject;

import java.lang.reflect.Constructor;

/**
 * Created by Andrei on 30/08/13.
 */
public class BaseElement extends ElementSkeleton {
    public BaseElement(JSONObject element, LinearLayout layout) {
        this.element = element;
        this.layout = layout;
    }

    public static BaseElement createObject(String type, JSONObject element, LinearLayout layout) {
        BaseElement newObject = null;
        Class<?> c;

        /**
         *  Get the target class object that we want to create. Use the dummy instance
         *  that was statically created to get the package path.
         */

        try {
            c = Class.forName(Synapse.getAppContext().getPackageName() + ".elements." + type);
        } catch (Exception e) {
            L.e("Failure to look up dynamic class element " + type + " due to " + e);
            e.printStackTrace();
            return null;
        }

        /**
         *  Create the desired constructor parameter types and create a constructor for
         *  the target class object. Initialize a new instance of the new object.
         */

        Class<?>[] types = new Class[] { JSONObject.class, LinearLayout.class };
        try {
            Constructor<?> constructor = c.getConstructor(types);
            newObject = (BaseElement)constructor.newInstance(element, layout);
        } catch (Exception e) {
            L.e("Failure to create dynamic class element " + type + " due to " + e);
            e.printStackTrace();

            return null;
        }

        return newObject;
    }

    public View getView() {
        return null;
    }
}