package com.af.synapse.lib;

import android.view.View;

/**
 * Created by Andrei on 24/05/2014.
 */
public interface Selectable extends View.OnLongClickListener {
    public void setSelectable(boolean flag);
    public void select();
    public void deselect();
    public boolean isSelected();
}
