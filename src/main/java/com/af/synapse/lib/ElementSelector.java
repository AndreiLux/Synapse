/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

import android.support.v4.util.ArrayMap;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.af.synapse.MainActivity;
import com.af.synapse.R;
import com.af.synapse.elements.BaseElement;
import com.af.synapse.elements.STreeDescriptor;
import com.af.synapse.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Andrei on 15/05/14.
 */
public class ElementSelector {
    private static ArrayList<Selectable> selection = new ArrayList<Selectable>();

    public static ActionMode.Callback callback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            if (inflater != null)
                inflater.inflate(R.menu.element_selection, menu);

            openSelection();

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_reset:
                    for (Selectable s : selection)
                        if (s instanceof ActionValueClient)
                            ((ActionValueClient) s).setDefaults();

                    mode.finish();
                    return true;

                case R.id.action_section_select_all:
                    for (BaseElement b : MainActivity.fragments[MainActivity.mViewPager.getCurrentItem()].fragmentElements)
                        if (b instanceof Selectable)
                            ((Selectable) b).select();
                    return true;

                case R.id.action_global_select_all:
                    for (int i=0; i < MainActivity.fragments.length; i++)
                        for (BaseElement b : MainActivity.fragments[i].fragmentElements)
                            if (b instanceof Selectable)
                                ((Selectable) b).select();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            closeSelection();
        }
    };

    public static void addElement(Selectable s) {
        selection.add(s);
    }

    public static void removeElement(Selectable s) {
        selection.remove(s);
    }

    public static void openSelection() {
        selection.clear();

        for (MainActivity.TabSectionFragment fragment : MainActivity.fragments) {
            for (BaseElement b : fragment.fragmentElements) {
                if (b instanceof Selectable) {
                    ((Selectable) b).setSelectable(true);
                }
            }
        }
    }

    public static void closeSelection() {
        for (MainActivity.TabSectionFragment fragment : MainActivity.fragments) {
            for (BaseElement b : fragment.fragmentElements) {
                if (b instanceof Selectable) {
                    if (((Selectable) b).isSelected())
                        ((Selectable) b).deselect();

                    ((Selectable) b).setSelectable(false);
                }
            }
        }

        selection.clear();
    }
}
