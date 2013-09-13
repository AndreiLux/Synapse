/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.LinearLayout;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;

import com.af.synapse.elements.*;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.L;
import com.af.synapse.utils.Utils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends FragmentActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private static JSONArray configSections;
    long startTime;

    private void setupUtilities() {
        if (!Utils.isUciSupport()) {
            //TODO no support
        }

        Utils.setPackageName(getPackageName());
        Utils.initiateDatabase(this);
        Utils.mainActivity = this;

        JSONObject resultsJSONObject = Utils.getJSON(getAssets());
        configSections = (JSONArray)resultsJSONObject.get("sections");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        L.d("Creating main activity");
        super.onCreate(savedInstanceState);

        startTime = System.nanoTime();
        Utils.appStart = true;

        if (savedInstanceState == null)
            setupUtilities();

        setContentView(R.layout.activity_main);
        fragments =  new Fragment[configSections.size()];
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        /**
         *  The UI building continues in buildFragment after fragment generation
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        ActionValueUpdater.setMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_apply:
                ActionValueUpdater.applyElements();
            case R.id.action_cancel:
                ActionValueUpdater.cancelElements();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static Fragment[] fragments;
    private static AtomicInteger fragmentsDone = new AtomicInteger(0);
    private static boolean generated = false;

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment
     * corresponding to one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private void buildFragment(int position) {
            if (fragments[position] != null)
                return;

            L.d("Getting tabSectionFragment "+ position);
            tabSectionFragment fragment = new tabSectionFragment();
            Bundle args = new Bundle();
            args.putInt(tabSectionFragment.ARG_SECTION_NUMBER, position);
            fragment.setArguments(args);
            fragment.prepareView(Utils.mainActivity);
            fragments[position] = fragment;
            fragmentsDone.incrementAndGet();
            L.d("Finished fragment " + position);

            if (fragmentsDone.get() < configSections.size())
                return;

            /**
             *  After all fragments are created, continue building the UI.
             */
            Utils.mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.appStart = false;
                    mViewPager = (ViewPager) findViewById(R.id.pager);
                    mViewPager.setOffscreenPageLimit(configSections.size());
                    mViewPager.setAdapter(mSectionsPagerAdapter);
                    L.d("Finish in " + (System.nanoTime() - startTime));
                }
            });
        }

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            if (fragmentsDone.get() > 0)
                return;

            for (int i = 0; i < configSections.size(); i++) {
                /**
                 *  Spawn a builder thread for each section/fragment
                 */
                final int position = i;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        buildFragment(position);
                    }
                }).start();
            }
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return configSections.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            JSONObject section = (JSONObject)configSections.get(position);
            return section.get("name").toString();
        }
    }

    public static class tabSectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this fragment.
         */

        public static final String ARG_SECTION_NUMBER = "section_number";

        private View fragmentView;
        private ArrayList<BaseElement> fragmentElements = new ArrayList<BaseElement>();

        public tabSectionFragment() {
            this.setRetainInstance(true);
        }

        public void prepareView(Activity context) {
            if (fragmentView != null)
                return;

            int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            ScrollView tabSectionView = (ScrollView)LayoutInflater.from(context)
                                                        .inflate(R.layout.section_container, null);
            assert tabSectionView != null;
            LinearLayout tabContentLayout = (LinearLayout) tabSectionView.getChildAt(0);
            assert tabContentLayout != null;

            JSONObject section = (JSONObject)configSections.get(sectionNumber);
            JSONArray sectionElements = (JSONArray)section.get("elements");

            for (int i = 0; i < sectionElements.size(); i++) {
                JSONObject elm = (JSONObject)sectionElements.get(i);
                String type = elm.keySet().toString().replace("[","").replace("]","");
                JSONObject parameters = (JSONObject)elm.get(type);

                L.d("Building element " + i + " (" + type + ") out of section " + sectionNumber);
                BaseElement elementObj = BaseElement.createObject(type, parameters,
                                                                  context, tabContentLayout);

                if (elementObj == null)
                    continue;
                /**
                 *  Simple standalone elements may not add themselves to the layout, if so, add
                 *  them here after their creation.
                 */

                View elementView = elementObj.getView();
                if (elementView != null)
                    tabContentLayout.addView(elementView);

                fragmentElements.add(elementObj);

                L.d("Added element " + i + " (" + type + ") out of section " + sectionNumber);
            }

            fragmentView = tabSectionView;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            L.d("");
            return fragmentView;
        }

        @Override
        public void onStart(){
            super.onStart();
            for (BaseElement elm : fragmentElements)
                try { ((ActivityListener) elm).onStart(); } catch (ClassCastException ignored) {}
        }

        @Override
        public void onResume(){
            super.onResume();
            for (BaseElement elm : fragmentElements)
                try { ((ActivityListener) elm).onResume(); } catch (ClassCastException ignored) {}
        }

        @Override
        public void onPause(){
            super.onPause();
            for (BaseElement elm : fragmentElements)
                try { ((ActivityListener) elm).onPause(); } catch (ClassCastException ignored) {}
        }

        @Override
        public void onStop(){
            super.onStop();
            for (BaseElement elm : fragmentElements)
                try { ((ActivityListener) elm).onStop(); } catch (ClassCastException ignored) {}
        }

        @Override
        public void onDetach(){
            /**
             *  On main activity destruction we are keeping the fragments instead of killing them.
             *  However on the next activity re-creation they need to get added to that new View
             *  instance. So we remove the child view from the old instance so that it can be
             *  added to the new one.
             */

            ((ViewGroup)fragmentView.getParent()).removeView(fragmentView);
            super.onDetach();
        }
    }
}
