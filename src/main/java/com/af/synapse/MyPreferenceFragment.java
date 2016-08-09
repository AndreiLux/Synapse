package com.af.synapse;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.af.synapse.utils.Utils;


public class MyPreferenceFragment extends PreferenceFragment {

    public MyPreferenceFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(Utils.settingsActivity, R.xml.preference_main, true);
        addPreferencesFromResource(R.xml.preference_main);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState){
        View v = super.onCreateView(inflater, root, savedInstanceState);
        if (v != null)
            v.setPadding(0, MainActivity.topPadding, 0, 0);
        return v;
    }
}