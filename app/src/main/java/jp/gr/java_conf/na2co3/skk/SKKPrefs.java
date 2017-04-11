package jp.gr.java_conf.na2co3.skk;

import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.support.v7.widget.Toolbar;

public class SKKPrefs extends AppCompatActivity implements OnPreferenceStartFragmentCallback {
    public static final String FRAGMENT = "fragment";
    public static final String TITLE = "title";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.skkprefs);
        setSupportActionBar((Toolbar)findViewById(R.id.pref_toolbar));

        Fragment fragment = null;
        Intent intent = getIntent();
        if (intent != null) {
            String fragmentName = intent.getStringExtra(FRAGMENT);
            if (fragmentName != null) {
                fragment = Fragment.instantiate(this, fragmentName, null);
            }

            String title = intent.getStringExtra(TITLE);
            if (title != null) {
                setTitle(title);
            }
        }
        if (fragment == null) {
            fragment = new SKKPrefsFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.pref_content, fragment).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.sendAppPrivateCommand(null, SKKService.ACTION_READ_PREFS, null);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        String fragmentName = pref.getFragment();
        if (!SKKPrefsFragment.class.getName().equals(fragmentName) &&
            !HardKeyPrefsFragment.class.getName().equals(fragmentName) &&
            !SoftKeyPrefsFragment.class.getName().equals(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment: " + fragmentName);
        }
        Intent intent = new Intent(SKKPrefs.this, SKKPrefs.class);
        intent.putExtra(FRAGMENT, pref.getFragment());
        intent.putExtra(TITLE, pref.getTitle());
        startActivity(intent);
        return true;
    }

    static String getKutoutenType(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefkey_kutouten_type), "en");
    }

    //~ static String getDefaultMode(Context context) {
        //~ return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_DEFAULT_MODE, "");
    //~ }

    static boolean getUseCandidatesView(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_use_candidates_view), true);
    }

    static int getCandidatesSize(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_candidates_size), 18);
    }

    static int getKanaKey(Context context) {
        int key = PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_kana_key), 93);
        if (key == KeyEvent.KEYCODE_UNKNOWN) {key = 93;}
        return key;
    }

    static int getCancelKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_cancel_key), KeyEvent.KEYCODE_UNKNOWN);
    }

    private static int getModKey(Context context, String key) {
        String val = PreferenceManager.getDefaultSharedPreferences(context).getString(key, "none");
        if (val.equals("alt")) {
            return KeyEvent.META_ALT_ON;
        } else if (val.equals("ctrl")) {
            return 4096; // KeyEvent.META_CTRL_ON
        } else if (val.equals("sym")) {
            return KeyEvent.META_SYM_ON;
        } else if (val.equals("meta")) {
            return 65536; // KeyEvent.META_META_ON
        } else if (val.equals("fn")) {
            return 8; // KeyEvent.META_FUNCTION_ON
        }

        return 0;
    }

    static int getModKanaKey(Context context) {
        return getModKey(context, context.getString(R.string.prefkey_mod_kana_key));
    }

    static int getModCancelKey(Context context) {
        return getModKey(context, context.getString(R.string.prefkey_mod_cancel_key));
    }

    public static boolean getToggleKanaKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_toggle_kana_key), true);
    }

    static int getFlickSensitivity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_flick_sensitivity), 30);
    }

    static String getCurveSensitivity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefkey_curve_sensitivity), "high");
    }

    static String getUseSoftKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefkey_use_softkey), "auto");
    }

    static boolean getUsePopup(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_use_popup), true);
    }

    static boolean getFixedPopup(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_fixed_popup), true);
    }

    static boolean getUseSoftCancelKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_use_soft_cancel_key), false);
    }

    static int getKeyHeightPort(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_key_height_port), 30);
    }

    static int getKeyHeightLand(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_key_height_land), 30);
    }

    static int getKeyWidthPort(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_key_width_port), 100);
    }

    static int getKeyWidthLand(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.prefkey_key_width_land), 100);
    }

    static String getKeyPosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.prefkey_key_position), "center");
    }

    static boolean getStickyMeta(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_sticky_meta), false);
    }

    static boolean getSandS(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefkey_sands), false);
    }
}
