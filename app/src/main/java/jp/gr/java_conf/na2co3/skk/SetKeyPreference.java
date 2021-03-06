package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class SetKeyPreference extends DialogPreference {
    private int mKeyCode;
    private static final int DEFAULT_VALUE = KeyEvent.KEYCODE_UNKNOWN;

    public SetKeyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SetKeyPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void setValue(int keyCode) {
        mKeyCode = keyCode;
        persistInt(keyCode);
    }

    int getValue() {
        return mKeyCode;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mKeyCode = this.getPersistedInt(DEFAULT_VALUE);
        } else {
            mKeyCode = (Integer)defaultValue;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, DEFAULT_VALUE);
    }

    int getDefaultValue() {
        return DEFAULT_VALUE;
    }

    static String getKeyName(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_A: return "A";
        case KeyEvent.KEYCODE_B: return "B";
        case KeyEvent.KEYCODE_C: return "C";
        case KeyEvent.KEYCODE_D: return "D";
        case KeyEvent.KEYCODE_E: return "E";
        case KeyEvent.KEYCODE_F: return "F";
        case KeyEvent.KEYCODE_G: return "G";
        case KeyEvent.KEYCODE_H: return "H";
        case KeyEvent.KEYCODE_I: return "I";
        case KeyEvent.KEYCODE_J: return "J";
        case KeyEvent.KEYCODE_K: return "K";
        case KeyEvent.KEYCODE_L: return "L";
        case KeyEvent.KEYCODE_M: return "M";
        case KeyEvent.KEYCODE_N: return "N";
        case KeyEvent.KEYCODE_O: return "O";
        case KeyEvent.KEYCODE_P: return "P";
        case KeyEvent.KEYCODE_Q: return "Q";
        case KeyEvent.KEYCODE_R: return "R";
        case KeyEvent.KEYCODE_S: return "S";
        case KeyEvent.KEYCODE_T: return "T";
        case KeyEvent.KEYCODE_U: return "U";
        case KeyEvent.KEYCODE_V: return "V";
        case KeyEvent.KEYCODE_W: return "W";
        case KeyEvent.KEYCODE_X: return "X";
        case KeyEvent.KEYCODE_Y: return "Y";
        case KeyEvent.KEYCODE_Z: return "Z";

        case KeyEvent.KEYCODE_0: return "0";
        case KeyEvent.KEYCODE_1: return "1";
        case KeyEvent.KEYCODE_2: return "2";
        case KeyEvent.KEYCODE_3: return "3";
        case KeyEvent.KEYCODE_4: return "4";
        case KeyEvent.KEYCODE_5: return "5";
        case KeyEvent.KEYCODE_6: return "6";
        case KeyEvent.KEYCODE_7: return "7";
        case KeyEvent.KEYCODE_8: return "8";
        case KeyEvent.KEYCODE_9: return "9";

        case KeyEvent.KEYCODE_ALT_LEFT:		return "ALT (left)";
        case KeyEvent.KEYCODE_ALT_RIGHT:	return "ALT (right)";
        case KeyEvent.KEYCODE_SHIFT_LEFT:	return "SHIFT (left)";
        case KeyEvent.KEYCODE_SHIFT_RIGHT:	return "SHIFT (right)";
        case KeyEvent.KEYCODE_NUM:			return "NUM";
        case KeyEvent.KEYCODE_SYM:			return "SYM";
        case KeyEvent.KEYCODE_SPACE:		return "SPACE";
        case KeyEvent.KEYCODE_DEL:			return "DEL";
        case KeyEvent.KEYCODE_ENTER:		return "ENTER";
        case KeyEvent.KEYCODE_TAB:			return "TAB";
        case KeyEvent.KEYCODE_AT:			return "@";
        case KeyEvent.KEYCODE_PERIOD:		return ".";
        case KeyEvent.KEYCODE_COMMA:		return ",";
        case KeyEvent.KEYCODE_APOSTROPHE:	return "'";
        case KeyEvent.KEYCODE_EQUALS:		return "=";
        case KeyEvent.KEYCODE_GRAVE:		return "`";
        case KeyEvent.KEYCODE_MINUS:		return "-";
        case KeyEvent.KEYCODE_PLUS:			return "+";
        case KeyEvent.KEYCODE_SEMICOLON:	return ";";
        case KeyEvent.KEYCODE_SLASH:		return "/";
        case KeyEvent.KEYCODE_STAR:			return "*";

        case KeyEvent.KEYCODE_DPAD_CENTER:	return "DPAD CENTER";
        case KeyEvent.KEYCODE_DPAD_DOWN:	return "DPAD DOWN";
        case KeyEvent.KEYCODE_DPAD_LEFT:	return "DPAD LEFT";
        case KeyEvent.KEYCODE_DPAD_RIGHT:	return "DPAD RIGHT";
        case KeyEvent.KEYCODE_DPAD_UP:		return "DPAD UP";
        case KeyEvent.KEYCODE_MENU:			return "MENU";
        case KeyEvent.KEYCODE_BACK:			return "BACK";
        case KeyEvent.KEYCODE_CALL:			return "CALL";
        case KeyEvent.KEYCODE_ENDCALL:		return "ENDCALL";
        case KeyEvent.KEYCODE_CAMERA:		return "CAMERA";
        case KeyEvent.KEYCODE_FOCUS:		return "FOCUS";
        case KeyEvent.KEYCODE_SEARCH:		return "SEARCH";
        case KeyEvent.KEYCODE_VOLUME_UP:	return "Volume UP";
        case KeyEvent.KEYCODE_VOLUME_DOWN:	return "Volume DOWN";

        case KeyEvent.KEYCODE_UNKNOWN: return "Not set";
        default: return "Unknown";
        }
    }
}
