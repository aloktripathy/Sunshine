package com.aloktripathy.sunshine;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen()
                .getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen()
                .getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        setSummary();
    }

    private void setSummary() {
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        // Set location summary.
        findPreference(getString(R.string.preference_location_key)).
                setSummary(sharedPreferences.getString(
                        getString(R.string.preference_location_key), ""));
        // Set temperature unit summary.
        findPreference(getString(R.string.preference_temperature_key)).
                setSummary(sharedPreferences.getString(
                        getString(R.string.preference_temperature_key), ""));
    }
}
