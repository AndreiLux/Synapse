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
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;

import net.minidev.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Andrei on 30/08/13.
 */
public class BaseElement extends ElementSkeleton {
    public BaseElement(JSONObject element, LinearLayout layout) {
        this.element = element;
        this.layout = layout;
    }

    public static BaseElement createObject(String type, JSONObject element, LinearLayout layout)
                              throws ElementFailureException {
        BaseElement newObject = null;
        Class<?> c;

        if (type == null)
            throw new ElementFailureException(BaseElement.class.getName(),
                    new IllegalArgumentException("Can't create element without a type."));

        /**
         *  Get the target class object that we want to create. Use the dummy instance
         *  that was statically created to get the package path.
         */

        try {
            c = Class.forName(Synapse.getAppContext().getPackageName() + ".elements." + type);
        } catch (Exception e) {
            throw new ElementFailureException(type, e);
        }

        /**
         *  Create the desired constructor parameter types and create a constructor for
         *  the target class object. Initialize a new instance of the new object.
         */

        Class<?>[] types = new Class[] { JSONObject.class, LinearLayout.class };
        try {
            Constructor<?> constructor = c.getConstructor(types);
            newObject = (BaseElement)constructor.newInstance(element, layout);
        } catch (InvocationTargetException e) {
            throw new ElementFailureException(type, (Exception) e.getCause());
        } catch (Exception e) {
            throw new ElementFailureException(type, e);
        }

        return newObject;
    }

    public View getView() throws ElementFailureException {
        return null;
    }
}