package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;

class SKKKeyboardView extends KeyboardView {
    private static final int KEYCODE_NONE = -1000;

    private static final int MSG_REPEAT = 1;

    private static final int REPEAT_DELAY = 400;
    private static final int REPEAT_INTERVAL = 50;

    private OnKeyboardActionListenerImpl actionListener = new OnKeyboardActionListenerImpl();
    private Handler mHandler;

    private int mRepeatingKeyCode = KEYCODE_NONE;
    private boolean mKeyPending = false;

    public SKKKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnKeyboardActionListener(actionListener);
    }

    public SKKKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnKeyboardActionListener(actionListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_REPEAT:
                            repeatKey();
                            break;
                    }
                }
            };
        }
    }

    @Override
    public void closing() {
        mHandler.removeMessages(MSG_REPEAT);
        super.closing();
    }

    private void repeatKey() {
        if (mRepeatingKeyCode == KEYCODE_NONE) {
            return;
        }
        onKey(mRepeatingKeyCode);
        mKeyPending = false;

        Message message = Message.obtain(mHandler, MSG_REPEAT);
        mHandler.sendMessageDelayed(message, REPEAT_INTERVAL);
    }

    protected void onPress(int code) {
    }

    protected void onKey(int code) {
    }

    public void startKeyRepeat(int code) {
        mRepeatingKeyCode = code;
        mKeyPending = true;
        Message message = mHandler.obtainMessage(MSG_REPEAT);
        mHandler.sendMessageDelayed(message, REPEAT_DELAY);
    }

    public void endKeyRepeat() {
        mHandler.removeMessages(MSG_REPEAT);
        mRepeatingKeyCode = KEYCODE_NONE;
        mKeyPending = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                endKeyRepeat();
        }
        return result;
    }

    private class OnKeyboardActionListenerImpl implements KeyboardView.OnKeyboardActionListener {
        @Override
        public void onPress(int primaryCode) {
            Keyboard keyboard = getKeyboard();
            if (keyboard instanceof SKKKeyboard) {
                for (SKKKeyboard.Key key : ((SKKKeyboard) keyboard).getSKKKeys()) {
                    if (key.codes[0] == primaryCode) {
                        if (key.skkRepeatable) {
                            startKeyRepeat(primaryCode);
                        }
                    }
                }
            }
            SKKKeyboardView.this.onPress(primaryCode);
        }

        public void onRelease(int primaryCode) {
        }

        public void onKey(int primaryCode, int[] keyCodes) {
            if (mKeyPending || primaryCode != mRepeatingKeyCode) {
                SKKKeyboardView.this.onKey(primaryCode);
            }
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
}
