package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.util.AttributeSet;

public class QwertyKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {
	private static final int KEYCODE_QWERTY_TOJP	= -1008;
	private static final int KEYCODE_QWERTY_TOSYM	= -1009;
	private static final int KEYCODE_QWERTY_TOLATIN	= -1010;
	private static final int KEYCODE_QWERTY_ENTER	= -1011;
	private static final int KEYCODE_QWERTY_SPACE = 32;

	private SKKEngine mService;

	private SKKKeyboard mLatinKeyboard;
	private SKKKeyboard mSymbolsKeyboard;
	private SKKKeyboard mSymbolsShiftedKeyboard;

	private int mFlickSensitivitySquared = 100;
	private float mFlickStartX = -1;
	private float mFlickStartY = -1;
	private boolean mFlicked = false;

	public QwertyKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup(context);
	}

	public QwertyKeyboardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setup(context);
	}

	private void setup(Context context) {
		mLatinKeyboard = new SKKKeyboard(context, R.xml.qwerty);
		mSymbolsKeyboard = new SKKKeyboard(context, R.xml.symbols);
		mSymbolsShiftedKeyboard = new SKKKeyboard(context, R.xml.symbols_shift);
		setKeyboard(mLatinKeyboard);
		setOnKeyboardActionListener(this);
	}

	public void setService(SKKEngine listener) {
		mService = listener;
	}

	void changeKeyHeight(int px) {
		mLatinKeyboard.changeKeyHeight(px);
		mSymbolsKeyboard.changeKeyHeight(px);
		mSymbolsShiftedKeyboard.changeKeyHeight(px);
	}

	void setFlickSensitivity(int sensitivity) {
		mFlickSensitivitySquared = sensitivity*sensitivity;
	}

	@Override
	protected boolean onLongPress (Keyboard.Key key) {
		int code = key.codes[0];
		if (code == KEYCODE_QWERTY_ENTER) {
			mService.keyDownUp(KeyEvent.KEYCODE_SEARCH);
			return true;
		} else if (code == KEYCODE_QWERTY_SPACE) {
			mService.showInputMethodPicker();
			return true;
		} else if (code == KEYCODE_QWERTY_TOJP) {
			mService.showMenuDialog();
			return true;
		}

		return super.onLongPress(key);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mFlickStartX = event.getRawX();
			mFlickStartY = event.getRawY();
			mFlicked = false;
			break;
		case MotionEvent.ACTION_MOVE:
			float dx = event.getRawX() - mFlickStartX;
			float dy = event.getRawY() - mFlickStartY;
			float dx2 = dx*dx;
			float dy2 = dy*dy;
			if (dx2+dy2 > mFlickSensitivitySquared) {
				if (dy < 0 && dx2 < dy2) {
					mFlicked = true;
					return true;
				}
			}
			break;
		}

		return super.onTouchEvent(event);
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
		if (primaryCode == Keyboard.KEYCODE_DELETE) {
			mService.keyDownUp(KeyEvent.KEYCODE_DEL);
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
			setShifted(!isShifted());
			Keyboard cur_keyboard = getKeyboard();
			if (cur_keyboard == mSymbolsKeyboard) {
				mSymbolsKeyboard.setShifted(true);
				setKeyboard(mSymbolsShiftedKeyboard);
				mSymbolsShiftedKeyboard.setShifted(true);
			} else if (cur_keyboard == mSymbolsShiftedKeyboard) {
				mSymbolsShiftedKeyboard.setShifted(false);
				setKeyboard(mSymbolsKeyboard);
				mSymbolsKeyboard.setShifted(false);
			}
		} else if (primaryCode == KEYCODE_QWERTY_ENTER) {
			mService.pressEnter();
		} else if (primaryCode == KEYCODE_QWERTY_TOJP) {
			setShifted(false);
			mLatinKeyboard.setShifted(false);
			mSymbolsKeyboard.setShifted(false);
			mSymbolsShiftedKeyboard.setShifted(false);
			setKeyboard(mLatinKeyboard);
			mService.toggleSKK();
		} else if (primaryCode == KEYCODE_QWERTY_TOSYM) {
			setShifted(false);
			mLatinKeyboard.setShifted(false);
			setKeyboard(mSymbolsKeyboard);
		} else if (primaryCode == KEYCODE_QWERTY_TOLATIN) {
			setShifted(false);
			mSymbolsKeyboard.setShifted(false);
			mSymbolsShiftedKeyboard.setShifted(false);
			setKeyboard(mLatinKeyboard);
		} else {
			handleCharacter(primaryCode, keyCodes);
		}
	}

	private void handleCharacter(int primaryCode, int[] keyCodes) {
		Keyboard cur_keyboard = getKeyboard();
		if (cur_keyboard == mLatinKeyboard) {
			if (isShifted() ^ mFlicked) {
				primaryCode = Character.toUpperCase(primaryCode);
			}
		}

		mService.sendText(String.valueOf((char) primaryCode));
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