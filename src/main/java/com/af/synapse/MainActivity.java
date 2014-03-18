/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;

import com.af.synapse.elements.*;
import com.af.synapse.utils.ActionValueClient;
import com.af.synapse.utils.ActionValueUpdater;
import com.af.synapse.utils.ActivityListener;
import com.af.synapse.utils.ElementFailureException;
import com.af.synapse.utils.L;
import com.af.synapse.utils.NamedRunnable;
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
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    private static Fragment[] fragments = null;
    private static AtomicInteger fragmentsDone = new AtomicInteger(0);
    long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.nanoTime();
        setContentView(R.layout.activity_loading);

        super.onCreate(fragments == null ? null : savedInstanceState);

        Utils.mainActivity = this;
        Utils.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (fragments == null) {
            if (Synapse.currentEnvironmentState != Synapse.environmentState.VALID_ENVIRONMENT) {
                findViewById(R.id.initialProgressBar).setVisibility(View.INVISIBLE);
                switch (Synapse.currentEnvironmentState) {
                    case ROOT_FAILURE:
                        ((TextView) findViewById(R.id.initialText)).setText(R.string.initial_no_root);
                        break;
                    case UCI_FAILURE:
                        ((TextView) findViewById(R.id.initialText)).setText(R.string.initial_no_uci);
                        break;
                    case JSON_FAILURE:
                        ((TextView) findViewById(R.id.initialText)).setText(R.string.initial_json_parse);
                }

                return;
            }
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        /**
         *  The UI building continues in buildFragment after fragment generation, or if
         *  the fragments are already live, continue here.
         */

        if (fragmentsDone.get() == Utils.configSections.size())
            continueCreate();
    }

    @SuppressWarnings("ConstantConditions")
    private void continueCreate() {
        setContentView(R.layout.activity_main);
        mViewPager = (ViewPager) findViewById(R.id.mainPager);
        mViewPager.setOffscreenPageLimit(Utils.configSections.size());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPagerPageChangeListener());
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        String[] section_titles = new String[Utils.configSections.size()];
        for (int i = 0; i < Utils.configSections.size(); i++)
            section_titles[i] = Utils.localise(((JSONObject)Utils.configSections.get(i)).get("name"));

        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, section_titles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setItemChecked(0, true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                                                  R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.syncState();

        ActionValueUpdater.refreshButtons(true);

        L.i("Interface creation finished in " + (System.nanoTime() - startTime) + "ns");

        if (!BootService.getBootFlag() && !BootService.getBootFlagPending()) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.popup_failed_boot_title)
                .setMessage(R.string.popup_failed_boot_message)
                .setCancelable(true)
                .setPositiveButton(R.string.popup_failed_boot_ack, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy(){
        if (!isChangingConfigurations()) {
            fragments = null;
            fragmentsDone = new AtomicInteger(0);
        }
        super.onDestroy();
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
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
            return true;

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_apply:
                ActionValueUpdater.applyElements();
                break;
            case R.id.action_cancel:
                ActionValueUpdater.cancelElements();
                break;
            case R.id.action_section_default:
                ActionValueUpdater.resetSectionDefault(mViewPager.getCurrentItem());
                break;
            case R.id.action_global_default:
                for (int i=0; i < Utils.configSections.size(); i++)
                    ActionValueUpdater.resetSectionDefault(i);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            mViewPager.setCurrentItem(position, true);
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    private class ViewPagerPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int i) {
            mDrawerList.setItemChecked(i, true);
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment
     * corresponding to one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private void buildFragment(int position) {
            if (fragments[position] != null)
                return;

            tabSectionFragment fragment = new tabSectionFragment();
            Bundle args = new Bundle();
            args.putInt(tabSectionFragment.ARG_SECTION_NUMBER, position);
            fragment.setArguments(args);
            fragments[position] = fragment;
            fragmentsDone.incrementAndGet();

            if (fragmentsDone.get() < Utils.configSections.size())
                return;

            /**
             *  After all fragments are created, continue building the UI.
             */
            Utils.mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    continueCreate();
                }
            });
        }

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            if (fragmentsDone.get() > 0)
                return;

            if (fragments == null)
                fragments = new Fragment[Utils.configSections.size()];

            for (int i = 0; i < Utils.configSections.size(); i++) {
                /**
                 *  Spawn a builder thread for each section/fragment
                 */
                final int position = i;

                Synapse.executor.execute(new NamedRunnable(
                    new Runnable() {
                        @Override
                        public void run() { buildFragment(position); }
                    })
                {
                    @Override
                    public String getName() {
                        return Utils.localise(((JSONObject)Utils.configSections
                                                                .get(position))
                                                                .get("name"));
                    }
                });
            }

            tabSectionFragment.startedFragments = 0;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return Utils.configSections.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            JSONObject section = (JSONObject)Utils.configSections.get(position);
            return Utils.localise(section.get("name"));
        }
    }

    public static class tabSectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this fragment.
         */

        public static final String ARG_SECTION_NUMBER = "section_number";
        public static int startedFragments = 0;

        public View fragmentView = null;
        private ArrayList<BaseElement> fragmentElements = new ArrayList<BaseElement>();

        public tabSectionFragment() {
            this.setRetainInstance(true);
        }

        public void prepareView() {
            if (fragmentView != null)
                return;

            int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            ScrollView tabSectionView = (ScrollView)LayoutInflater.from(Utils.mainActivity)
                                                        .inflate(R.layout.section_container, null);
            assert tabSectionView != null;
            LinearLayout tabContentLayout = (LinearLayout) tabSectionView.getChildAt(0);
            assert tabContentLayout != null;

            JSONObject section = (JSONObject)Utils.configSections.get(sectionNumber);
            JSONArray sectionElements = (JSONArray)section.get("elements");

            for (Object sectionElement : sectionElements) {
                JSONObject elm = (JSONObject) sectionElement;
                String type = elm.keySet().toString().replace("[", "").replace("]", "");
                JSONObject parameters = (JSONObject) elm.get(type);

                BaseElement elementObj = null;

                try {
                    elementObj = BaseElement.createObject(type, parameters, tabContentLayout);
                } catch (ElementFailureException e) {
                    tabContentLayout.addView(Utils.createElementErrorView(e));
                    continue;
                }

                if (elementObj instanceof ActionValueClient)
                    ActionValueUpdater.registerPerpetual((ActionValueClient) elementObj, sectionNumber);

                /**
                 *  Simple standalone elements may not add themselves to the layout, if so, add
                 *  them here after their creation.
                 */

                View elementView = null;

                try {
                    elementView = elementObj.getView();
                } catch (ElementFailureException e) {
                    View errorView = Utils.createElementErrorView(e);
                    if (errorView != null)
                        tabContentLayout.addView(errorView);
                    continue;
                }

                if (elementView != null)
                    tabContentLayout.addView(elementView);

                fragmentElements.add(elementObj);
            }

            fragmentView = tabSectionView;
        }

        @Override
        public void setArguments(Bundle args) {
            super.setArguments(args);
            prepareView();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prepareView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            return fragmentView;
        }

        @Override
        public void onStart(){
            super.onStart();
            for (BaseElement elm : fragmentElements)
                if (elm instanceof ActivityListener)
                    try { ((ActivityListener) elm).onStart(); }
                    catch (ElementFailureException e) { Utils.createElementErrorView(e); }

            /**
             *  Utils.appStarted serves as a flag to mark the completion of the *first*
             *  post-onStart of *all* fragments.
             */
            if (startedFragments < Utils.configSections.size())
                startedFragments++;
            else
                Utils.appStarted = true;
        }

        @Override
        public void onResume(){
            super.onResume();
            for (BaseElement elm : fragmentElements)
                if (elm instanceof ActivityListener)
                    try { ((ActivityListener) elm).onResume(); }
                    catch (ElementFailureException e) { Utils.createElementErrorView(e); }
        }

        @Override
        public void onPause(){
            super.onPause();
            for (BaseElement elm : fragmentElements)
                if (elm instanceof ActivityListener)
                    try { ((ActivityListener) elm).onPause(); }
                    catch (ElementFailureException e) { Utils.createElementErrorView(e); }
        }

        @Override
        public void onStop(){
            super.onStop();
            for (BaseElement elm : fragmentElements)
                if (elm instanceof ActivityListener)
                    try { ((ActivityListener) elm).onStop(); }
                    catch (ElementFailureException e) { Utils.createElementErrorView(e); }
        }

        @Override
        public void onDetach(){
            /**
             *  On main activity destruction we are keeping the fragments instead of killing them.
             *  However on the next activity re-creation they need to get added to that new View
             *  instance. So we remove the child view from the old instance so that it can be
             *  added to the new one.
             */

            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup)fragmentView.getParent();
                if (parent != null)
                    parent.removeView(fragmentView);
            }
            super.onDetach();
        }
    }
}
