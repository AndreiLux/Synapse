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

import com.af.synapse.elements.BaseElement;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;
import com.af.synapse.utils.RootFailureException;
import com.af.synapse.utils.RunCommandFailedException;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Andrei on 10/04/14.
 */
public class ActionValueNotifierHandler {
    public static ArrayMap<String, ActionValueNotifierClient> clients = new ArrayMap<String, ActionValueNotifierClient>();

    public static ArrayMap<ActionValueNotifierClient, ArrayList<NotificationRelation>> notifiers =
            new ArrayMap<ActionValueNotifierClient, ArrayList<NotificationRelation>>();

    static class NotificationRelation {
        private ActionValueNotifierClient target;
        private ActionValueEvent event;
        private Object[] response;

        protected NotificationRelation(ActionValueNotifierClient target,
                                       ActionValueEvent event, Object ... response) {
            this.target = target;
            this.event = event;
            this.response = response;
        }
    }

    public static synchronized void register(ActionValueNotifierClient client) {
        clients.put(client.getId(), client);
    }

    public static synchronized void remove(ActionValueNotifierClient client) {
        clients.remove(client);
    }

    private static void subExtract(ActionValueNotifierClient client,
                                   JSONObject notification,
                                   boolean notify) {
        if (notification.containsKey("on") &&
            notification.containsKey("do") &&
            notification.containsKey("to")) {

            Object on = notification.get("on");
            Object doo = notification.get("do");
            Object to = notification.get("to");

            Object event = null;
            Object response = null;
            Object target = null;

            if (on instanceof String)
                // Single event listener
                event = ActionValueEvent.valueOf((String) on);
            else if (on instanceof JSONArray) {
                // Multiple event listeners
                event = ((JSONArray) on).toArray();

                for (int i=0; i < ((Object[]) event).length; i++)
                    ((Object[]) event)[i] = ActionValueEvent.valueOf((String) ((Object[]) event)[i]);
            }

            if (to instanceof String)
                // Single target
                target = to.toString();
            else if (to instanceof JSONArray)
                // Multiple targets
                target = ((JSONArray) to).toArray();

            if (doo instanceof String)
                try {
                    // Single response of standard Event
                    response = ActionValueEvent.valueOf((String) doo);
                } catch (IllegalArgumentException ignored) {
                    // Single response of custom action
                    response = doo.toString();
                }
            else if (doo instanceof JSONArray) {
                // Multiple responses, may be standard event or custom actions
                response = new ArrayList<Object>();
                for (Object o : (JSONArray) doo)
                    try {
                        // Add the next Event
                        ((ArrayList<Object>)response).add(ActionValueEvent.valueOf((String) o));
                    } catch (IllegalArgumentException ignored) {
                        // Add the next action
                        ((ArrayList<Object>)response).add(o.toString());
                    }
            }

            if (event instanceof Object[])
                for (Object e : (Object[]) event)
                    // For each event, if there are multiple targets then create a notifier object
                    // for each target with the response chain for current event.
                    if (target instanceof Object[]) {
                        for (Object t : (Object[]) target)
                            if (notify)
                                notifyTo(client, (String) t, (ActionValueEvent) e, response);
                            else
                                listenTo(client, (String) t, (ActionValueEvent) e, response);
                    } else
                        // Otherwise create a single notifier object only on the current event
                        if (notify)
                            notifyTo(client, (String) target, (ActionValueEvent) e, response);
                        else
                            listenTo(client, (String) target, (ActionValueEvent) e, response);
            else
                if (target instanceof Object[]) {
                    for (Object t : (Object[]) target)
                        if (notify)
                            notifyTo(client, (String) t, (ActionValueEvent) event, response);
                        else
                            listenTo(client, (String) t, (ActionValueEvent) event, response);
                } else
                    // Single event, single target
                    if (notify)
                        notifyTo(client, (String) target, (ActionValueEvent) event, response);
                    else
                        listenTo(client, (String) target, (ActionValueEvent) event, response);
        }
    }

    public static void addNotifiers(ActionValueNotifierClient client) {
        JSONObject element = ((BaseElement) client).element;

        if (element.containsKey("notify")) {
            Object notification = element.get("notify");

            if (notification instanceof JSONArray)
                for (Object sub : (JSONArray) notification)
                    subExtract(client, (JSONObject) sub, true);
            else
                subExtract(client, (JSONObject) notification, true);
        }

        if (element.containsKey("listen")) {
            Object notification = element.get("listen");

            if (notification instanceof JSONArray)
                for (Object sub : (JSONArray) notification)
                    subExtract(client, (JSONObject) sub, false);
            else
                subExtract(client, (JSONObject) notification, false);
        }
    }

    public static void listenTo(ActionValueNotifierClient listener, String target,
                                ActionValueEvent event, Object ... response) {
        ActionValueNotifierClient t = clients.get(target);
        if (t == null)
            throw new IllegalArgumentException("Target "+target+" is not registered");

        if (!notifiers.containsKey(t))
            notifiers.put(t, new ArrayList<NotificationRelation>());

        notifiers.get(t).add(new NotificationRelation(listener, event, response));
    }

    public static void notifyTo(ActionValueNotifierClient notifier, String target,
                                ActionValueEvent event, Object ... response) {
        ActionValueNotifierClient t = clients.get(target);
        if (t == null)
            throw new IllegalArgumentException("Target "+target+" is not registered");

        if (!notifiers.containsKey(notifier))
            notifiers.put(notifier, new ArrayList<NotificationRelation>());

        notifiers.get(notifier).add(new NotificationRelation(t, event, response));
    }

    private static String replaceTokens(String input, ActionValueNotifierClient source) throws ElementFailureException {
        return
            input
                .replace("@SAVED", source.getStoredValue())
                .replace("@SET", source.getSetValue())
                .replace("@LIVE", source.getLiveValue())
                .replace("@ACTION", source.getId());
    }

    public static void propagate(ActionValueNotifierClient source, ActionValueEvent event) {
        if (!notifiers.containsKey(source))
            return;

        for (NotificationRelation n : notifiers.get(source)) {
            if (n.event == event) {
                for (Object r : n.response)
                    if (r instanceof ActionValueEvent)
                        n.target.onNotify(source, (ActionValueEvent) r);
                    else if (r instanceof ArrayList) {
                        for (Object rr : (ArrayList) r)
                            if (rr instanceof ActionValueEvent)
                                n.target.onNotify(source, (ActionValueEvent) rr);
                            else if (rr instanceof String)
                                try {
                                    Utils.runCommand(replaceTokens((String) rr, source));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                    } else if (r instanceof String) {
                        try {
                            Utils.runCommand(replaceTokens((String) r, source));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            }
        }
    }

    public static void clear(){
        notifiers.clear();
        clients.clear();
    }
}
