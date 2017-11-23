package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.view.KeyEvent;
import android.util.AttributeSet;

public class AbbrevKeyboardView extends SKKKeyboardView {
    private static final int KEYCODE_ABBREV_CANCEL	= -1009;
    private static final int KEYCODE_ABBREV_ZENKAKU	= -1010;
    private static final int KEYCODE_ABBREV_ENTER	= -1011;
    private static final int KEYCODE_ABBREV_SPACE   = 32;

    private SKKService mService;

    private SKKKeyboard mKeyboard;

    public AbbrevKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public AbbrevKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setShifted(false);
    }

    private void setup(Context context) {
        mKeyboard = new SKKKeyboard(context, R.xml.abbrev, 5);
        setKeyboard(mKeyboard);
    }

    public void setService(SKKService listener) {
        mService = listener;
    }

    void changeKeyHeight(int px) {
        mKeyboard.changeKeyHeight(px);
    }

    @Override
    protected boolean onLongPress (Keyboard.Key key) {
        if (key.codes[0] == KEYCODE_ABBREV_SPACE) {
            mService.showInputMethodPicker();
            return true;
        } else if (key.codes[0] == KEYCODE_ABBREV_ZENKAKU) {
            mService.showMenuDialog();
            return true;
        }

        return super.onLongPress(key);
    }

    @Override
    public void onKey(int code) {
        if (code == Keyboard.KEYCODE_DELETE) {
            if (!mService.handleBackspace()) {
                mService.keyDownUp(KeyEvent.KEYCODE_DEL);
            }
        } else if (code == Keyboard.KEYCODE_SHIFT) {
            setShifted(!isShifted());
        } else if (code == KEYCODE_ABBREV_ENTER) {
            if (!mService.handleEnter()) {
                mService.pressEnter();
            }
        } else if (code == KEYCODE_ABBREV_CANCEL) {
            mService.handleCancel();
        } else if (code == KEYCODE_ABBREV_ZENKAKU) {
            mService.processKey(code);
        } else {
            if (isShifted()) {code = Character.toUpperCase(code);}
            mService.processKey((char) code);
        }
    }

}
