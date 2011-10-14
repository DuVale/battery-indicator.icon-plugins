/*
    Copyright (c) 2009, 2010 Josiah Barber (aka Darshan)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

package com.darshancomputing.BatteryIndicatorPro.IconPluginV1.Classic;

// TODO: Remove as many of these imports as you can
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_TEN_PERCENT_MODE = "ten_percent_mode";
    public static final String KEY_CAT_COLOR = "category_color";
    public static final String KEY_RED = "use_red";
    public static final String KEY_RED_THRESH = "red_threshold";
    public static final String KEY_AMBER = "use_amber";
    public static final String KEY_AMBER_THRESH = "amber_threshold";
    public static final String KEY_GREEN = "use_green";
    public static final String KEY_GREEN_THRESH = "green_threshold";
    public static final String KEY_COLOR_PREVIEW = "color_preview";

    private static final String[] PARENTS =    {KEY_RED,        KEY_AMBER,        KEY_GREEN};
    private static final String[] DEPENDENTS = {KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH};

    private static final String[] LIST_PREFS = {KEY_RED_THRESH, KEY_AMBER_THRESH, KEY_GREEN_THRESH};

    private static final String[] RESET_SERVICE = {KEY_RED, KEY_RED_THRESH, KEY_AMBER, KEY_AMBER_THRESH,
                                                   KEY_GREEN, KEY_GREEN_THRESH, KEY_TEN_PERCENT_MODE};

    public static final String EXTRA_SCREEN = "com.darshancomputing.BatteryIndicatorPro.PrefScreen";

    public static final int   RED = 0;
    public static final int AMBER = 1;
    public static final int GREEN = 2;

    /* Red must go down to 0 and green must go up to 100,
       which is why they aren't listed here. */
    public static final int   RED_ICON_MAX = 30;
    public static final int AMBER_ICON_MIN =  0;
    public static final int AMBER_ICON_MAX = 50;
    public static final int GREEN_ICON_MIN = 20;

    public static final int   RED_SETTING_MIN =  5;
    public static final int   RED_SETTING_MAX = 30;
    public static final int AMBER_SETTING_MIN = 10;
    public static final int AMBER_SETTING_MAX = 50;
    public static final int GREEN_SETTING_MIN = 20;
    /* public static final int GREEN_SETTING_MAX = 100; /* TODO: use this, and possibly set it to 95. */

    private static final int DIALOG_CONFIRM_TEN_PERCENT_ENABLE  = 0;
    private static final int DIALOG_CONFIRM_TEN_PERCENT_DISABLE = 1;

    private Intent biServiceIntent;
    //private BIServiceConnection biServiceConnection;

    private Resources res;
    private PreferenceScreen mPreferenceScreen;
    private SharedPreferences mSharedPreferences;

    private String pref_screen;

    private ColorPreviewPreference cpbPref;

    private ListPreference   redThresh;
    private ListPreference amberThresh;
    private ListPreference greenThresh;

    private Boolean   redEnabled;
    private Boolean amberEnabled;
    private Boolean greenEnabled;

    private int   iRedThresh;
    private int iAmberThresh;
    private int iGreenThresh;

    private Boolean ten_percent_mode;

    private int menu_res = R.menu.settings;

    private static final String[] fivePercents = {
        "5", "10", "15", "20", "25", "30", "35", "40", "45", "50",
        "55", "60", "65", "70", "75", "80", "85", "90", "95", "100"};

    /* Also includes 5 and 15, as the orginal Droid (and presumably similarly crippled devices)
       goes by 5% once you get below 20%. */
    private static final String[] tenPercentEntries = {
	"5", "10", "15", "20", "30", "40", "50",
	"60", "70", "80", "90", "100"};

    /* Setting Red and Amber values like this allows the Service to follow the same algorithm no matter what. */
    private static final String[] tenPercentValues = {
	"6", "11", "16", "21", "31", "41", "51",
	"61", "71", "81", "91", "101"};

    /* Returns a two-item array of the start and end indices into the above arrays. */
    private int[] indices(int x, int y) {
        int[] a = new int[2];
        int i; /* How many values to remove from the front */
        int j; /* How many values to remove from the end   */

        if (ten_percent_mode) {
            for (i = 0; i < tenPercentEntries.length - 1; i++)
                if (Integer.valueOf(tenPercentEntries[i]) >= Integer.valueOf(x)) break;
            j = (100 - y) / 10;
        } else {
            i = (x / 5) - 1;
            j = (100 - y) / 5;
        }

        a[0] = i;
        a[1] = j;
        return a;
    }

    private static final Object[] EMPTY_OBJECT_ARRAY = {};
    private static final  Class[]  EMPTY_CLASS_ARRAY = {};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        pref_screen = intent.getStringExtra(EXTRA_SCREEN);
        res = getResources();

        mSharedPreferences = getPreferenceManager().getSharedPreferences();

        setPrefScreen(R.xml.color_pref_screen);
        setTitle(res.getString(R.string.settings_activity_name));

        menu_res = R.menu.settings;

        ten_percent_mode = mSharedPreferences.getBoolean(KEY_TEN_PERCENT_MODE, false);

        cpbPref     = (ColorPreviewPreference) mPreferenceScreen.findPreference(KEY_COLOR_PREVIEW);
        if (ten_percent_mode) cpbPref.setLayoutResource(R.layout.hidden);

        redThresh   = (ListPreference) mPreferenceScreen.findPreference(KEY_RED_THRESH);
        amberThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_AMBER_THRESH);
        greenThresh = (ListPreference) mPreferenceScreen.findPreference(KEY_GREEN_THRESH);

        redEnabled   = mSharedPreferences.getBoolean(  KEY_RED, false);
        amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
        greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);

        iRedThresh   = Integer.valueOf(  redThresh.getValue()); /* Entries don't exist yet */
        iAmberThresh = Integer.valueOf(amberThresh.getValue());
        iGreenThresh = Integer.valueOf(greenThresh.getValue());

        mPreferenceScreen.findPreference(KEY_TEN_PERCENT_MODE)
            .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
                {
                    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                        showDialog((Boolean) newValue ? DIALOG_CONFIRM_TEN_PERCENT_ENABLE : DIALOG_CONFIRM_TEN_PERCENT_DISABLE);
                        return false;
                    }
                });

        if (ten_percent_mode) {
            /* These should always correspond to the logical (entry) value, not the actual stored value. */
            iRedThresh--;
            iAmberThresh--;
        }

        validateColorPrefs(null);

        for (int i=0; i < PARENTS.length; i++)
            setEnablednessOfDeps(i);

        for (int i=0; i < LIST_PREFS.length; i++)
            updateListPrefSummary(LIST_PREFS[i]);

        //biServiceIntent = new Intent(this, BatteryIndicatorService.class);
        //biServiceConnection = new BIServiceConnection();
        //bindService(biServiceIntent, biServiceConnection, 0);
    }

    private void setPrefScreen(int resource) {
        addPreferencesFromResource(resource);

        mPreferenceScreen  = getPreferenceScreen();
    }

    private void restartThisScreen() {
        ComponentName comp = new ComponentName(getPackageName(), SettingsActivity.class.getName());
        Intent intent = new Intent().setComponent(comp);
        intent.putExtra(EXTRA_SCREEN, pref_screen);
        startActivity(intent);
        finish();
    }

    // TODO: Can the plugin (easily) restart teh service?
    private void resetService() {
        try {
            //biServiceConnection.biService.reloadSettings();
        } catch (Exception e) {
            //startService(new Intent(this, BatteryIndicatorService.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //unbindService(biServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menu_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_help:
            ComponentName comp = new ComponentName(getPackageName(), SettingsHelpActivity.class.getName());
            Intent intent = new Intent().setComponent(comp);

            if (pref_screen != null) intent.putExtra(EXTRA_SCREEN, pref_screen);

            startActivity(intent);

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        /* Android saves and reuses these dialogs; we want different titles for each, hence two IDs */
        case DIALOG_CONFIRM_TEN_PERCENT_ENABLE:
        case DIALOG_CONFIRM_TEN_PERCENT_DISABLE:
            builder.setTitle(ten_percent_mode ? res.getString(R.string.confirm_ten_percent_disable) : res.getString(R.string.confirm_ten_percent_enable))
                .setMessage(res.getString(R.string.confirm_ten_percent_hint))
                .setCancelable(false)
                .setPositiveButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        ten_percent_mode = ! ten_percent_mode;
                        ((CheckBoxPreference) mPreferenceScreen.findPreference(KEY_TEN_PERCENT_MODE)).setChecked(ten_percent_mode);
                        di.cancel();

                        restartThisScreen();
                    }
                })
                .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.cancel();
                    }
                });

            dialog = builder.create();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        if (key.equals(KEY_RED)) {
            redEnabled = mSharedPreferences.getBoolean(KEY_RED, false);
        } else if (key.equals(KEY_AMBER)) {
            amberEnabled = mSharedPreferences.getBoolean(KEY_AMBER, false);
        } else if (key.equals(KEY_GREEN)) {
            greenEnabled = mSharedPreferences.getBoolean(KEY_GREEN, false);
        } else if (key.equals(KEY_RED_THRESH)) {
            iRedThresh = Integer.valueOf((String) redThresh.getEntry());
        } else if (key.equals(KEY_AMBER_THRESH)) {
            iAmberThresh = Integer.valueOf((String) amberThresh.getEntry());
        } else if (key.equals(KEY_GREEN_THRESH)) {
            iGreenThresh = Integer.valueOf((String) greenThresh.getEntry());
        }

        if (key.equals(KEY_RED) || key.equals(KEY_RED_THRESH) ||
            key.equals(KEY_AMBER) || key.equals(KEY_AMBER_THRESH) ||
            key.equals(KEY_GREEN) || key.equals(KEY_GREEN_THRESH)) {
            validateColorPrefs(key);
        }

        if (key.equals(KEY_TEN_PERCENT_MODE))
            resetColorsToDefaults();

        for (int i=0; i < PARENTS.length; i++) {
            if (key.equals(PARENTS[i])) {
                setEnablednessOfDeps(i);
                break;
            }
        }

        for (int i=0; i < LIST_PREFS.length; i++) {
            if (key.equals(LIST_PREFS[i])) {
                updateListPrefSummary(LIST_PREFS[i]);
                break;
            }
        }

        for (int i=0; i < RESET_SERVICE.length; i++) {
            if (key.equals(RESET_SERVICE[i])) {
                resetService();
                break;
            }
        }

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setEnablednessOfDeps(int index) {
        Preference dependent = mPreferenceScreen.findPreference(DEPENDENTS[index]);
        if (dependent == null) return;

        if (mSharedPreferences.getBoolean(PARENTS[index], false))
            dependent.setEnabled(true);
        else
            dependent.setEnabled(false);

        updateListPrefSummary(DEPENDENTS[index]);
    }

    private void setEnablednessOfMutuallyExclusive(String key1, String key2) {
        Preference pref1 = mPreferenceScreen.findPreference(key1);
        Preference pref2 = mPreferenceScreen.findPreference(key2);

        if (pref1 == null) return;

        if (mSharedPreferences.getBoolean(key1, false))
            pref2.setEnabled(false);
        else if (mSharedPreferences.getBoolean(key2, false))
            pref1.setEnabled(false);
        else {
            pref1.setEnabled(true);
            pref2.setEnabled(true);
        }
    }

    private void updateListPrefSummary(String key) {
        ListPreference pref;
        try { /* Code is simplest elsewhere if we call this on all dependents, but some aren't ListPreferences. */
            pref = (ListPreference) mPreferenceScreen.findPreference(key);
        } catch (java.lang.ClassCastException e) {
            return;
        }

        if (pref == null) return;

        if (pref.isEnabled()) {
            pref.setSummary(res.getString(R.string.currently_set_to) + pref.getEntry());
        } else {
            pref.setSummary(res.getString(R.string.currently_disabled));
        }
    }

    private void validateColorPrefs(String changedKey) {
        if (redThresh == null) return;
        String lowest;

        if (changedKey == null) {
            setColorPrefEntriesAndValues(redThresh, RED_SETTING_MIN, RED_SETTING_MAX);

            /* Older version had a higher max; user's setting could be too high. */
            if (iRedThresh > RED_SETTING_MAX) {
                redThresh.setValue("" + RED_SETTING_MAX);
                iRedThresh = RED_SETTING_MAX;
                if (ten_percent_mode) iRedThresh--;
            }
        }

        if (changedKey == null || changedKey.equals(KEY_RED) || changedKey.equals(KEY_RED_THRESH) ||
            changedKey.equals(KEY_AMBER)) {
            if (amberEnabled) {
                lowest = setColorPrefEntriesAndValues(amberThresh, determineMin(AMBER), AMBER_SETTING_MAX);

                if (iAmberThresh < Integer.valueOf(lowest)) {
                    amberThresh.setValue(lowest);
                    iAmberThresh = Integer.valueOf(lowest);
                    if (ten_percent_mode) iAmberThresh--;
                    updateListPrefSummary(KEY_AMBER_THRESH);
                }
            }
        }

        if (changedKey == null || !changedKey.equals(KEY_GREEN_THRESH)) {
            if (greenEnabled) {
                lowest = setColorPrefEntriesAndValues(greenThresh, determineMin(GREEN), 100);

                if (iGreenThresh < Integer.valueOf(lowest)) {
                    greenThresh.setValue(lowest);
                    iGreenThresh = Integer.valueOf(lowest);
                    updateListPrefSummary(KEY_GREEN_THRESH);
                }
            }
        }

        updateColorPreviewBar();
    }

    /* Does the obvious and returns the lowest value. */
    private String setColorPrefEntriesAndValues(ListPreference lpref, int min, int max) {
        String[] entries, values;
        int i, j;
        int[] a;

        a = indices(min, max);
        i = a[0];
        j = a[1];

        if (ten_percent_mode) {
            entries = new String[tenPercentEntries.length - i - j];
            values  = new String[tenPercentEntries.length - i - j];
            System.arraycopy(tenPercentEntries, i, entries, 0, entries.length);
            System.arraycopy(tenPercentValues , i,  values, 0,  values.length);
            if (lpref.equals(greenThresh)) values = entries;
        } else {
            entries = values = new String[fivePercents.length - i - j];
            System.arraycopy(fivePercents, i, entries, 0, entries.length);
        }

        lpref.setEntries(entries);
        lpref.setEntryValues(values);

        return values[0];
    }

    private void resetColorsToDefaults() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putBoolean(KEY_RED,   res.getBoolean(R.bool.default_use_red));
        editor.putBoolean(KEY_AMBER, res.getBoolean(R.bool.default_use_amber));
        editor.putBoolean(KEY_GREEN, res.getBoolean(R.bool.default_use_green));

        if (mSharedPreferences.getBoolean(KEY_TEN_PERCENT_MODE, false)){
            editor.putString(  KEY_RED_THRESH, res.getString(R.string.default_red_thresh10  ));
            editor.putString(KEY_AMBER_THRESH, res.getString(R.string.default_amber_thresh10));
            editor.putString(KEY_GREEN_THRESH, res.getString(R.string.default_green_thresh10));
        } else {
            editor.putString(  KEY_RED_THRESH, res.getString(R.string.default_red_thresh  ));
            editor.putString(KEY_AMBER_THRESH, res.getString(R.string.default_amber_thresh));
            editor.putString(KEY_GREEN_THRESH, res.getString(R.string.default_green_thresh));
        }

        editor.commit();
    }

    /* Determine the minimum valid threshold setting for a particular color, based on other active settings,
         with red being independent, amber depending on red, and green depending on both others. */
    private int determineMin(int color) {
        switch (color) {
        case RED:
            return RED_SETTING_MIN;
        case AMBER:
            if (redEnabled)
                /* In 10% mode, we might want +10, but xToY10() will sort it out if +5 is too small. */
                return java.lang.Math.max(iRedThresh + 5, AMBER_SETTING_MIN);
            else
                return AMBER_SETTING_MIN;
        case GREEN:
            int i;

            if (amberEnabled)
                i = iAmberThresh;
            else if (redEnabled)
                i = iRedThresh;
            else
                return GREEN_SETTING_MIN;

            if (ten_percent_mode)
                /* We'll usually want +10, but it could be just +5. xToY10() will sort it out if +5 is too small. */
                return java.lang.Math.max(i + 5, GREEN_SETTING_MIN);
            else
                return java.lang.Math.max(i, GREEN_SETTING_MIN);
        default:
                return GREEN_SETTING_MIN;
        }
    }

    private void updateColorPreviewBar() {
        if (cpbPref == null) return;

        cpbPref.redThresh   =   redEnabled ?   iRedThresh :   0;
        cpbPref.amberThresh = amberEnabled ? iAmberThresh :   0;
        cpbPref.greenThresh = greenEnabled ? iGreenThresh : 100;
    }
}
