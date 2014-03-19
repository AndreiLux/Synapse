/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

import android.content.ActivityNotFoundException;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.af.synapse.R;

/**
 * Created by Andrei on 19/03/14.
 */
public class LinkMovementMethod extends android.text.method.LinkMovementMethod {
    private static LinkMovementMethod sInstance;

    public static android.text.method.MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new LinkMovementMethod();

        return sInstance;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        try {
            return super.onTouchEvent(widget, buffer, event) ;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(Utils.mainActivity,
                            R.string.activity_link_movement_failure,
                            Toast.LENGTH_LONG).show();
            return true;
        }
    }
}
