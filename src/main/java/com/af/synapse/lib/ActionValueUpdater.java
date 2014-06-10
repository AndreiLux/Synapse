/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.lib;

import android.view.Menu;
import android.view.MenuItem;

import com.af.synapse.R;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Andrei on 04/09/13.
 */
public class ActionValueUpdater {
    public static ArrayList<ArrayList<ActionValueClient>> perpetuals = new ArrayList<ArrayList<ActionValueClient>>();
    private static ArrayList<ActionValueClient> registrees = new ArrayList<ActionValueClient>();
    private static MenuItem applyButton;
    private static MenuItem cancelButton;
    private static boolean blocked = false;
    private static boolean showingButtons = false;

    public static void registerPerpetual(ActionValueClient element, int sectionNumber) {
        for (Object o : Utils.configSections)
            perpetuals.add(new ArrayList<ActionValueClient>());

        ArrayList<ActionValueClient> sectionList = perpetuals.get(sectionNumber);
        sectionList.add(element);
    }

    public static void removePerpetual(ActionValueClient element) {
        ArrayList<ActionValueClient> targetSection = null;
        for (ArrayList<ActionValueClient> section : perpetuals)
            for (ActionValueClient client : section)
                if (client == element)
                    targetSection = section;

        if (targetSection != null)
            targetSection.remove(element);
    }

    public static boolean isPerpetual(ActionValueClient element) {
        for (ArrayList<ActionValueClient> section : perpetuals)
            for (ActionValueClient client : section)
                if (client == element)
                    return true;

        return false;
    }

    public static void registerElement(ActionValueClient element) {
        if (blocked)
            return;

        registrees.add(element);
        refreshButtons();
    }

    public static void removeElement(ActionValueClient element) {
        if (blocked)
            return;

        registrees.remove(element);
        refreshButtons();
    }

    public static boolean isRegistered(ActionValueClient element) {
        return registrees.contains(element);
    }

    private static void changeElements(boolean commit) {
        blocked = true;

        for (ActionValueClient element : registrees) {
            try {
                if (commit)
                    element.applyValue();
                else
                    element.cancelValue();
            } catch (ElementFailureException e) {
                removePerpetual((ActionValueClient) e.getSource());
                Utils.createElementErrorView(e);
            }
        }

        registrees.clear();
        BootService.setBootFlag(true);
        blocked = false;

        refreshButtons();
    }

    public static void refreshButtons() {
        refreshButtons(false);
    }

    public static void refreshButtons(boolean force) {
        if (applyButton == null || cancelButton == null || force)
            return;

        if (registrees.isEmpty() ^ showingButtons)
            return;

        Utils.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (registrees.isEmpty()) {
                    applyButton.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                    cancelButton.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
                    applyButton.setVisible(false);
                    cancelButton.setVisible(false);
                    showingButtons = false;
                } else {
                    applyButton.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_ALWAYS);
                    cancelButton.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_ALWAYS);
                    applyButton.setVisible(true);
                    cancelButton.setVisible(true);
                    showingButtons = true;
                }
            }
        });
    }

    public static void applyElements() {
        changeElements(true);
    }

    public static void cancelElements() {
        changeElements(false);
    }

    public static void resetSectionDefault(int sectionPosition) {
        for (ActionValueClient client : perpetuals.get(sectionPosition))
            client.setDefaults();
    }

    public static void setMenu(Menu menu) {
        applyButton = menu.findItem(R.id.action_apply);
        cancelButton = menu.findItem(R.id.action_cancel);
        showingButtons = false;
        refreshButtons();
    }

    public static void clear() {
        perpetuals.clear();
        registrees.clear();
    }
}
