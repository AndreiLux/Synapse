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
 * Created by Andrei on 19/04/14.
 */

public class ActionNotification {
    public ActionValueNotifierClient source;
    public ActionValueEvent notification;

    public ActionNotification(ActionValueNotifierClient s, ActionValueEvent e){
        this.source = s;
        this.notification = e;
    }
}
