package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.util.AttributeSet;

public class AbbrevKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {
    private static final int KEYCODE_ABBREV_CANCEL	= -1009;
    private static final int KEYCODE_ABBREV_ZENKAKU	= -1010;
    private static final int KEYCODE_ABBREV_ENTER	= -1011;

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
        setOnKeyboardActionListener(this);
    }

    public void setService(SKKService listener) {
        mService = listener;
    }

    void changeKeyHeight(int px) {
        mKeyboard.changeKeyHeight(px);
    }

    @Override
    protected boolean onLongPress (Keyboard.Key key) {
        if (key.codes[0] == KeyEvent.KEYCODE_SPACE) {
            mService.showInputMethodPicker();
            return true;
        } else if (key.codes[0] == KEYCODE_ABBREV_ZENKAKU) {
            mService.showMenuDialog();
            return true;
        }

        return super.onLongPress(key);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            if (!mService.handleBackspace()) {
                mService.keyDownUp(KeyEvent.KEYCODE_DEL);
            }
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            setShifted(!isShifted());
        } else if (primaryCode == KEYCODE_ABBREV_ENTER) {
            if (!mService.handleEnter()) {
                mService.pressEnter();
            }
        } else if (primaryCode == KEYCODE_ABBREV_CANCEL) {
            mService.handleCancel();
        } else if (primaryCode == KEYCODE_ABBREV_ZENKAKU) {
            mService.processKey(primaryCode);
        } else {
            if (isShifted()) {primaryCode = Character.toUpperCase(primaryCode);}
            mService.processKey((char) primaryCode);
        }
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }

    public void onText(CharSequence text) {
    }

    public void swipeRight() {
    }

    public void swipeLeft() {
    }

    public void swipeDown() {
    }

    public void swipeUp() {
    }

}