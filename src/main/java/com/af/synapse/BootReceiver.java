/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;
/**
 * Created by Andrei on 23/09/13.
 */
public class BootReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, BootService.class);
        context.startService(service);

        Toast.makeText(Synapse.getAppContext(), "Synapse boot completed", Toast.LENGTH_LONG).show();
    }
}
