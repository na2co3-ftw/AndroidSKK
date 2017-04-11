package jp.gr.java_conf.na2co3.skk;

import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class HardKeyPrefsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle icicle, String s) {
		addPreferencesFromResource(R.xml.prefs_hardkey);

		final CheckBoxPreference stickyPr = (CheckBoxPreference) findPreference(getString(R.string.prefkey_sticky_meta));
		final CheckBoxPreference sandsPr = (CheckBoxPreference) findPreference(getString(R.string.prefkey_sands));
		stickyPr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				sandsPr.setEnabled(!stickyPr.isChecked());
				return true;
			}
		});
		sandsPr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				stickyPr.setEnabled(!sandsPr.isChecked());
				return true;
			}
		});

		if (stickyPr.isChecked()) {
			sandsPr.setEnabled(false);
		} else if (sandsPr.isChecked()) {
			stickyPr.setEnabled(false);
		}
	}
}
