package com.af.synapse;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.af.synapse.utils.Utils;

public class Settings extends PreferenceActivity {
    public final static String PREF_THEME   = "preference_list_theme_option";
    public final static String PREF_BOOT    = "preference_bool_boot_master";
    public final static String PREF_PROBATION    = "preference_bool_boot_probation";
    public final static String PREF_HIDE_DESC    = "preference_bool_hide_descriptions";

    public enum Theme {
        HOLO_BLACK,
        HOLO_DARK,
        TRANSLUCENT_DARK
    }

    public static Theme theme = null;
    public static BitmapDrawable wallpaper = null;

    public static Theme getAppTheme() {
        if (theme == null) {
            theme = Theme.valueOf(PreferenceManager
                    .getDefaultSharedPreferences(Synapse.getAppContext())
                    .getString(PREF_THEME, "HOLO_BLACK"));
        }

        return theme;
    }

    public static void setWallpaper(Activity context) {
        switch (getAppTheme()) {
            case HOLO_BLACK:
                context.getWindow()
                    .setBackgroundDrawableResource(android.R.color.black);
                return;

            case HOLO_DARK:
                context.getWindow()
                    .setBackgroundDrawableResource(R.drawable.holo_gradient_grey_270);
                return;

            case TRANSLUCENT_DARK:
                if (wallpaper == null) {
                    wallpaper = new BitmapDrawable(
                        Utils.drawableToBitmap(
                            context.getWindowManager(),
                            WallpaperManager.getInstance(Synapse.getAppContext()).getFastDrawable())
                    );

                    WallpaperManager.getInstance(Synapse.getAppContext()).forgetLoadedWallpaper();
                    System.gc();

                    wallpaper.setColorFilter(
                        context.getResources().getColor(R.color.app_background_inverse),
                        PorterDuff.Mode.MULTIPLY);
                }
        }

        context.getWindow().setBackgroundDrawable(wallpaper);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.settingsActivity = this;

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getFragmentManager()
            .beginTransaction()
            .replace(android.R.id.content, new MyPreferenceFragment())
            .commit();

        setWallpaper(this);

        PreferenceManager
            .getDefaultSharedPreferences(Utils.mainActivity)
            .registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                        if (key == PREF_THEME) {
                            theme = null;
                            setWallpaper(Utils.mainActivity);
                            setWallpaper(Utils.settingsActivity);
                        }
                    }
                }
            );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wallpaper = null;
        Utils.settingsActivity = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(Settings.this, R.xml.preference_main, true);
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
}
