package jp.gr.java_conf.na2co3.skk;

import android.os.Bundle;

public class SoftKeyPrefsFragment extends SKKPrefsFragment {
    @Override
    public void onCreatePreferences(Bundle icicle, String s) {
        addPreferencesFromResource(R.xml.prefs_softkey);
    }
}
