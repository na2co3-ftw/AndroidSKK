package jp.gr.java_conf.na2co3.skk;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SKKPrefsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle icicle, String s) {
		addPreferencesFromResource(R.xml.prefs);
	}
}
