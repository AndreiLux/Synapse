/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

import java.util.ArrayList;

/**
 * Created by Andrei on 07/04/14.
 */

public interface ActionValueNotifierClient extends ActionValueClient {
    public String getId();
    public void onNotify(ActionValueNotifierClient source, ActionValueEvent notification);
}
