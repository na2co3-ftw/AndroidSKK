package jp.gr.java_conf.na2co3.skk;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SKKPrefsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle icicle, String s) {
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof SetKeyPreference) {
            DialogFragment f = SetKeyPreferenceDialogFragmentCompat.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
