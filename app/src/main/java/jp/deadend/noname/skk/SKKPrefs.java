package jp.deadend.noname.skk;

import android.os.Bundle;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.support.v7.widget.Toolbar;

public class SKKPrefs extends PreferenceActivity {
    private AppCompatDelegate mDelegate;

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getDelegate().onCreate(icicle);
        getDelegate().setContentView(R.layout.skkprefs);
        getDelegate().setSupportActionBar((Toolbar)findViewById(R.id.pref_toolbar));
        addPreferencesFromResource(R.xml.prefs);

        final CheckBoxPreference stickyPr = (CheckBoxPreference)findPreference(getString(R.string.prefkey_sticky_meta));
        final CheckBoxPreference sandsPr = (CheckBoxPreference)findPreference(getString(R.string.prefkey_sands));
        stickyPr.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                sandsPr.setEnabled(!stickyPr.isChecked());
                return true;
            }
        });
        sandsPr.setOnPreferenceClickListener(new OnPreferenceClickListener() {
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

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        View view = super.onCreateView(parent, name, context, attrs);
        if(view != null) {
            return view;
        }
        return getDelegate().createView(parent, name, context, attrs);
    }

    @Nullable
    @Override public View onCreateView(String name, Context context, AttributeSet attrs) {
        View view = super.onCreateView(name, context, attrs);
        if(view != null) {
            return view;
        }
        return getDelegate().createView(null, name, context, attrs);
    }

    @Override
    protected void onPause() {
        super.onPause();

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.sendAppPrivateCommand(null, SKKService.ACTION_READ_PREFS, null);
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
