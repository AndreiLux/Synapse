/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.elements;

import android.support.v4.util.ArrayMap;
import android.view.View;
import android.widget.LinearLayout;

import com.af.synapse.MainActivity;
import com.af.synapse.Synapse;
import com.af.synapse.lib.ActionNotification;
import com.af.synapse.lib.ActionValueClient;
import com.af.synapse.lib.ActionValueEvent;
import com.af.synapse.lib.ActionValueNotifierClient;
import com.af.synapse.lib.ActionValueNotifierHandler;
import com.af.synapse.lib.ActionValueUpdater;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Created by Andrei on 15/03/14.
 */

public class STreeDescriptor extends BaseElement implements ActionValueNotifierClient {
    private String directoryPath;

    static class ObjectDescriptor {
        String          type;
        JSONObject   element;
        BaseElement instance;

        public ObjectDescriptor(String type, JSONObject element) {
            this.type = type;
            this.element = element;
        }

        public ObjectDescriptor(ObjectDescriptor copy) {
            this.type = copy.type;
            this.element = (JSONObject) copy.element.clone();
        }
    }

    private final ObjectDescriptor directoryDescriptor;
    private final ObjectDescriptor elementDescriptor;

    private ArrayMap<String, ObjectDescriptor> directories;
    private ArrayMap<String, ObjectDescriptor> elements;

    private ArrayList<ObjectDescriptor> descriptorList = new ArrayList<ObjectDescriptor>();
    private ArrayList<String> excludes = new ArrayList<String>();

    private ArrayList<BaseElement> baseElements = new ArrayList<BaseElement>();
    private ArrayList<String> actionList = new ArrayList<String>();
    private ArrayList<STreeDescriptor> children = new ArrayList<STreeDescriptor>();

    private MainActivity.TabSectionFragment container = null;

    public STreeDescriptor(JSONObject element, LinearLayout layout, MainActivity.TabSectionFragment fragment) {
        super(element, layout, fragment);

        if (element.containsKey("path"))
            this.directoryPath = (String) element.get("path");

        if (element.containsKey("exclude")) {
            JSONArray list = (JSONArray) element.get("exclude");
            for (Object term : list)
                excludes.add((String) term);
        }

        if (element.containsKey("generic")) {
            JSONObject generic = (JSONObject) element.get("generic");

            if (generic.containsKey("directory")) {
                JSONObject directory = (JSONObject) generic.get("directory");
                String type = Utils.getEnclosure(directory);
                directory = (JSONObject) directory.get(type);
                this.directoryDescriptor = new ObjectDescriptor(type, directory);
            } else
                this.directoryDescriptor = null;

            if (generic.containsKey("element")) {
                JSONObject elementObject = (JSONObject) generic.get("element");
                String type = Utils.getEnclosure(elementObject);
                elementObject = (JSONObject) elementObject.get(type);
                this.elementDescriptor = new ObjectDescriptor(type, elementObject);
            } else
                this.elementDescriptor = null;
        } else {
            this.directoryDescriptor = null;
            this.elementDescriptor = null;
        }

        if (element.containsKey("matched")) {
            JSONObject matched = (JSONObject) element.get("matched");

            if (matched.containsKey("directories")) {
                JSONArray directoriesJSON = (JSONArray) matched.get("directories");
                this.directories = new ArrayMap<String, ObjectDescriptor>();

                for (Object directoryEntry : directoriesJSON) {
                    JSONObject valuePair = (JSONObject) directoryEntry;
                    String path = Utils.getEnclosure(valuePair);

                    JSONObject directoryObject = (JSONObject) valuePair.get(path);
                    String type = Utils.getEnclosure(directoryObject);

                    directoryObject = (JSONObject) directoryObject.get(type);
                    this.directories.put(path, new ObjectDescriptor(type, directoryObject));
                }
            }

            if (matched.containsKey("elements")) {
                JSONArray elementsJSON = (JSONArray) matched.get("elements");
                this.elements = new ArrayMap<String, ObjectDescriptor>();

                for (Object elementEntry : elementsJSON) {
                    JSONObject valuePair = (JSONObject) elementEntry;
                    String path = Utils.getEnclosure(valuePair);

                    JSONObject elementObject = (JSONObject) valuePair.get(path);
                    String type = Utils.getEnclosure(elementObject);

                    elementObject = (JSONObject) elementObject.get(type);
                    this.elements.put(path, new ObjectDescriptor(type, elementObject));
                }
            }
        }

        buildHierarchy();
        ActionValueNotifierHandler.register(this);
    }

    private String replaceTokens(String input, File file) {
        return
            input
                .replace("@NAME", file.getName().replace('_', ' '))
                .replace("@BASENAME", file.getName())
                .replace("@PATH", file.getAbsolutePath());
    }

    private void buildHierarchy() {
        File[] files = new File(this.directoryPath).listFiles();
        if (files != null) {
            for (File file : files) {
                if (excludes.contains(file.getName()))
                    continue;

                if (file.isDirectory()) {
                    if (this.directories != null && this.directories.containsKey(file.getName())) {
                        descriptorList.add(this.directories.get(file.getName()));
                    } else if (directoryDescriptor != null) {
                        ObjectDescriptor desc = new ObjectDescriptor(directoryDescriptor);
                        desc.element.put("title", file.getName());

                        if (desc.type.equals(this.getClass().getSimpleName()))
                            desc.element.put("path", file.getAbsolutePath());

                        descriptorList.add(desc);
                    } else
                        L.e("Directory descriptor is null for " + directoryPath);
                } else {
                    ObjectDescriptor desc = null;
                    if (this.elements != null && this.elements.containsKey(file.getName())) {
                        desc = this.elements.get(file.getName());
                    } else if (elementDescriptor != null) {
                        desc = new ObjectDescriptor(elementDescriptor);

                        if (desc.element.containsKey("title"))
                            desc.element.put("title", replaceTokens((String)desc.element.get("title"), file));
                    } else
                        L.e("File descriptor is null for " + directoryPath);

                    if (desc != null) {
                        if (!desc.element.containsKey("action"))
                            desc.element.put("action", "generic" + ' ' + file.getAbsolutePath());
                        else
                            desc.element.put("action",
                                replaceTokens(((String) desc.element.get("action")), file));

                        descriptorList.add(desc);
                    }
                }
            }
        }

        descriptorLoop:
        for (ObjectDescriptor d : descriptorList) {
            if (d.type.equals(this.getClass().getSimpleName())) {
                for (STreeDescriptor parser : this.children)
                    if (parser.directoryPath.equals(d.element.get("path"))) {
                        d.instance = parser;
                        continue descriptorLoop;
                    }

                try {
                    d.instance = BaseElement.createObject(d.type, d.element, this.layout, this.fragment);
                    this.children.add((STreeDescriptor) d.instance);
                    continue;
                } catch (ElementFailureException e) {
                    e.printStackTrace();
                }
            }

            if (d.element.containsKey("action"))
                actionList.add((String) d.element.get("action"));
        }
    }

    @Override
    public View getView() {
        for (ObjectDescriptor d : descriptorList) {
            try {
                BaseElement b;
                if (d.type.equals(this.getClass().getSimpleName()))
                    b = d.instance;
                else
                    b = BaseElement.createObject(d.type, d.element, this.layout, this.fragment);

                baseElements.add(b);
                fragment.addElement(b);

                if (b instanceof ActionValueClient)
                    ActionValueUpdater.registerPerpetual((ActionValueClient)b, this.fragment.getSectionNumber());

                View v = b.getView();
                if (v != null)
                    this.layout.addView(v);
            } catch (ElementFailureException e) {
                this.layout.addView(Utils.createElementErrorView(e));
            }
        }

        return null;
    }

    public void collapse() throws ElementFailureException {
        View v;
        LinearLayout p;

        for (BaseElement e : baseElements) {
            if (e instanceof ActionValueClient) {
                if (ActionValueUpdater.isRegistered((ActionValueClient) e))
                    ActionValueUpdater.removeElement((ActionValueClient) e);

                if (ActionValueUpdater.isPerpetual((ActionValueClient) e))
                    ActionValueUpdater.removePerpetual((ActionValueClient) e);
            }

            if (e instanceof  ActionValueNotifierClient) {
                ActionValueNotifierHandler.remove((ActionValueNotifierClient) e);
            }

            /*
             *  Trying to get a View of an child STreeDescriptor is not what we want to do here,
             *  and will cause undefined behaviour. A collapse will logically mean an invalidation
             *  of the whole tree, and getting a View of a child will cause a parsing of all
             *  of its elements, which will not be able to operate normally as the parent node
             *  will be invalid.
             */
            if (e instanceof STreeDescriptor)
                continue;

            v = e.getView();
            if (v != null) {
                p = (LinearLayout) v.getParent();
                if (p != null)
                    p.removeView(v);
            }

            fragment.removeElement(e);
        }

        actionList.clear();
        baseElements.clear();
        descriptorList.clear();

        for (STreeDescriptor b : this.children)
            b.collapse();

        this.children.clear();
    }

    public void rebuild() {
        buildHierarchy();
        getView();
    }

    public ArrayList<String> getFlatActionTreeList() {
        ArrayList<String> f = new ArrayList<String>();

        f.addAll(this.actionList);

        for (STreeDescriptor b : this.children)
            f.addAll(b.getActions());

        return f;
    }

    public ArrayList<String> getActions() {
        return this.actionList;
    }

    /**
     *  ActionValueNotifierClient methods
     */

    @Override
    public String getId() {
        return directoryPath;
    }

    private ArrayDeque<ActionNotification> queue = new ArrayDeque<ActionNotification>();
    private boolean jobRunning = false;

    public void handleNotifications() {
        jobRunning = true;
        while (queue.size() > 0) {
            ActionNotification current = queue.removeFirst();
            switch (current.notification) {
                case REFRESH:
                    try {
                        collapse();
                        rebuild();
                    } catch (ElementFailureException e) {
                        e.printStackTrace();
                    }
                    break;

                case CANCEL:
                    try { cancelValue(); } catch (ElementFailureException e) { e.printStackTrace(); }
                    break;

                case RESET:
                    setDefaults();
                    break;

                case APPLY:
                    try { applyValue(); } catch (ElementFailureException e) {  e.printStackTrace(); }
                    break;
            }
        }
        jobRunning = false;
    }

    private Runnable dequeJob = new Runnable() {
        @Override
        public void run() {
            handleNotifications();
        }
    };

    @Override
    public void onNotify(ActionValueNotifierClient source, ActionValueEvent notification) {
        queue.add(new ActionNotification(source, notification));

        if (queue.size() == 1 && !jobRunning)
            Synapse.handler.post(dequeJob);
    }

    @Override
    public String getLiveValue() throws ElementFailureException { return null;}

    @Override
    public String getSetValue() { return null; }

    @Override
    public String getStoredValue() { return null; }

    @Override
    public void refreshValue() throws ElementFailureException {
        for (BaseElement b : baseElements)
            if (b instanceof ActionValueClient)
                ((ActionValueClient)b).refreshValue();
    }

    @Override
    public void setDefaults() {
        for (BaseElement b : baseElements)
            if (b instanceof ActionValueClient)
                ((ActionValueClient)b).setDefaults();
    }

    @Override
    public void applyValue() throws ElementFailureException {
        for (BaseElement b : baseElements)
            if (b instanceof ActionValueClient)
                ((ActionValueClient)b).applyValue();
    }

    @Override
    public void cancelValue() throws ElementFailureException {
        for (BaseElement b : baseElements)
            if (b instanceof ActionValueClient)
                ((ActionValueClient)b).cancelValue();
    }
}

