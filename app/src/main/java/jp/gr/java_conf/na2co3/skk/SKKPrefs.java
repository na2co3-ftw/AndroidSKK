package jp.gr.java_conf.na2co3.skk;

import android.os.Bundle;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;

public class SKKPrefs extends PreferenceActivity {
	private static final String PREFKEY_KUTOUTEN_TYPE = "PrefKeyKutoutenType";
	private static final String PREFKEY_KANA_KEY = "PrefKeyKanaKey";
	private static final String PREFKEY_MOD_KANA_KEY = "PrefKeyModKanaKey";
	private static final String PREFKEY_TOGGLE_KANAKEY = "PrefKeyToggleKanaKey";
	private static final String PREFKEY_CANCEL_KEY = "PrefKeyCancelKey";
	private static final String PREFKEY_MOD_CANCEL_KEY = "PrefKeyModCancelKey";
	private static final String PREFKEY_CANDIDATES_OP_COUNT = "PrefKeyCandidatesOpCount";
	private static final String PREFKEY_CANDIDATES_SIZE = "PrefKeyCandidatesSize";
	private static final String PREFKEY_FLICK_SENSITIVITY = "PrefKeyFlickSensitivity";
	private static final String PREFKEY_CURVE_SENSITIVITY = "PrefKeyCurveSensitivity";
	private static final String PREFKEY_USE_SOFTKEY = "PrefKeyUseSoftKey";
	private static final String PREFKEY_USE_POPUP = "PrefKeyUsePopup";
	private static final String PREFKEY_FIXED_POPUP = "PrefKeyFixedPopup";
	private static final String PREFKEY_USE_SOFT_CANCEL_KEY = "PrefKeyUseSoftCancelKey";
	private static final String PREFKEY_KEY_HEIGHT = "PrefKeyKeyHeight";
	private static final String PREFKEY_KEY_HEIGHT_PORT = "PrefKeyKeyHeightPort";
	private static final String PREFKEY_KEY_HEIGHT_LAND = "PrefKeyKeyHeightLand";
	private static final String PREFKEY_KEY_WIDTH_PORT = "PrefKeyKeyWidthPort";
	private static final String PREFKEY_KEY_WIDTH_LAND = "PrefKeyKeyWidthLand";
	private static final String PREFKEY_KEY_POSITION = "PrefKeyKeyPosition";
	private static final String PREFKEY_STICKY_META = "PrefKeyStickyMeta";
	private static final String PREFKEY_SANDS = "PrefKeySandS";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.prefs);

		final CheckBoxPreference stickyPr = (CheckBoxPreference)findPreference(PREFKEY_STICKY_META);
		final CheckBoxPreference sandsPr = (CheckBoxPreference)findPreference(PREFKEY_SANDS);
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
	protected void onPause() {
		super.onPause();

		InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.sendAppPrivateCommand(null, SKKEngine.ACTION_READ_PREFS, null);
	}

	static String getKutoutenType(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_KUTOUTEN_TYPE, "en");
	}

	//~ static String getDefaultMode(Context context) {
		//~ return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_DEFAULT_MODE, "");
	//~ }

	static int getCandidatesOpCount(Context context) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_CANDIDATES_OP_COUNT, "2"));
	}

	static int getCandidatesSize(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_CANDIDATES_SIZE, 18);
	}

	static int getKanaKey(Context context) {
		int key = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KANA_KEY, 93);
		if (key == KeyEvent.KEYCODE_UNKNOWN) {key = 93;}
		return key;
	}

	static int getCancelKey(Context context) {
		int key = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_CANCEL_KEY, KeyEvent.KEYCODE_UNKNOWN);
		return key;
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
		return getModKey(context, PREFKEY_MOD_KANA_KEY);
	}

	static int getModCancelKey(Context context) {
		return getModKey(context, PREFKEY_MOD_CANCEL_KEY);
	}

	static boolean getToggleKanaKey(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_TOGGLE_KANAKEY, true);
	}

	static int getFlickSensitivity(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_FLICK_SENSITIVITY, 30);
	}

	static String getCurveSensitivity(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_CURVE_SENSITIVITY, "high");
	}

	static String getUseSoftKey(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_USE_SOFTKEY, "auto");
	}

	static boolean getUsePopup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_USE_POPUP, true);
	}

	static boolean getFixedPopup(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_FIXED_POPUP, true);
	}

	static boolean getUseSoftCancelKey(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_USE_SOFT_CANCEL_KEY, false);
	}

	static int getKeyHeight(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_HEIGHT, 80);
	}

	static int getKeyHeightPort(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_HEIGHT_PORT, 30);
	}

	static int getKeyHeightLand(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_HEIGHT_LAND, 30);
	}

	static int getKeyWidthPort(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_WIDTH_PORT, 100);
	}

	static int getKeyWidthLand(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREFKEY_KEY_WIDTH_LAND, 100);
	}

	static String getKeyPosition(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PREFKEY_KEY_POSITION, "center");
	}

	static boolean getStickyMeta(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_STICKY_META, false);
	}

	static boolean getSandS(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFKEY_SANDS, false);
	}
}
