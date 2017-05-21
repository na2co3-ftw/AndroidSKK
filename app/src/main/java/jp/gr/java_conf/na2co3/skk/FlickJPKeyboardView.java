package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import jp.gr.java_conf.na2co3.skk.engine.RomajiConverter;
import jp.gr.java_conf.na2co3.skk.engine.SKKEngine;

public class FlickJPKeyboardView extends KeyboardView implements KeyboardView.OnKeyboardActionListener {
    private static final int KEYCODE_FLICK_JP_CHAR_A	= -201;
    private static final int KEYCODE_FLICK_JP_CHAR_KA	= -202;
    private static final int KEYCODE_FLICK_JP_CHAR_SA	= -203;
    private static final int KEYCODE_FLICK_JP_CHAR_TA	= -204;
    private static final int KEYCODE_FLICK_JP_CHAR_NA	= -205;
    private static final int KEYCODE_FLICK_JP_CHAR_HA	= -206;
    private static final int KEYCODE_FLICK_JP_CHAR_MA	= -207;
    private static final int KEYCODE_FLICK_JP_CHAR_YA	= -208;
    private static final int KEYCODE_FLICK_JP_CHAR_RA	= -209;
    private static final int KEYCODE_FLICK_JP_CHAR_WA	= -210;
    private static final int KEYCODE_FLICK_JP_CHAR_TEN	= -211;
    private static final int KEYCODE_FLICK_JP_NONE		= -1000;
    private static final int KEYCODE_FLICK_JP_LEFT		= -1001;
    private static final int KEYCODE_FLICK_JP_RIGHT		= -1002;
    private static final int KEYCODE_FLICK_JP_MOJI		= -1003;
    private static final int KEYCODE_FLICK_JP_SPACE		= -1004;
    private static final int KEYCODE_FLICK_JP_TOQWERTY	= -1005;
    private static final int KEYCODE_FLICK_JP_ROTATE	= -1006;
    private static final int KEYCODE_FLICK_JP_ENTER		= -1007;
    private static final int KEYCODE_FLICK_JP_SEARCH	= -1008;
    private static final int KEYCODE_FLICK_JP_TOKANA	= -1010;
    private static final int FLICK_STATE_NONE			= 0;
    private static final int FLICK_STATE_LEFT			= 1;
    private static final int FLICK_STATE_UP				= 2;
    private static final int FLICK_STATE_RIGHT			= 3;
    private static final int FLICK_STATE_DOWN			= 4;
    private static final int FLICK_STATE_NONE_LEFT		= 5;
    private static final int FLICK_STATE_LEFT_LEFT		= 6;
    private static final int FLICK_STATE_UP_LEFT		= 7;
    private static final int FLICK_STATE_RIGHT_LEFT		= 8;
    private static final int FLICK_STATE_DOWN_LEFT		= 9;
    private static final int FLICK_STATE_NONE_RIGHT		= 10;
    private static final int FLICK_STATE_LEFT_RIGHT		= 11;
    private static final int FLICK_STATE_UP_RIGHT		= 12;
    private static final int FLICK_STATE_RIGHT_RIGHT	= 13;
    private static final int FLICK_STATE_DOWN_RIGHT		= 14;
    private static final String[] POPUP_LABELS_NULL = new String[]{"", "", "", "", "", "", ""};

    private SKKService mService;

    private boolean isHiragana = true;

    private int mFlickSensitivitySquared = 100;
    private boolean mUseCurve = false;
    private float mCurveSensitivityMultiplier = 2.0f;
    private int mLastPressedKey = KEYCODE_FLICK_JP_NONE;
    private int mFlickState = FLICK_STATE_NONE;
    private float mFlickStartX = -1;
    private float mFlickStartY = -1;
    private String[] mCurrentPopupLabels = POPUP_LABELS_NULL;

    private boolean isEnterLongPressed = false;
    private boolean isSpaceLongPressed = false;
    private boolean isToQwertyLongPressed = false;

    private boolean mUsePopup = true;
    private boolean mFixedPopup = false;
    private PopupWindow mPopup = null;
    private TextView[] mPopupTextView = new TextView[15];
    private int mPopupSize = 120;
    private int[] mPopupOffset = {0, 0};
    private int[] mFixedPopupPos = {0, 0};

    private SKKKeyboard mJPKeyboard;
    private SKKKeyboard mNumKeyboard;

     private SparseArray<String[]> mFlickCharList = new SparseArray<>();
    {
        SparseArray<String[]> a = mFlickCharList;
        a.append(KEYCODE_FLICK_JP_CHAR_A,	new String[]{"あ", "い", "う", "え", "お", "小", ""});
        a.append(KEYCODE_FLICK_JP_CHAR_KA,	new String[]{"か", "き", "く", "け", "こ", "",   "゛"});
        a.append(KEYCODE_FLICK_JP_CHAR_SA,	new String[]{"さ", "し", "す", "せ", "そ", "",   "゛"});
        a.append(KEYCODE_FLICK_JP_CHAR_TA,	new String[]{"た", "ち", "つ", "て", "と", "",   "゛"});
        a.append(KEYCODE_FLICK_JP_CHAR_NA,	new String[]{"な", "に", "ぬ", "ね", "の", "",   ""});
        a.append(KEYCODE_FLICK_JP_CHAR_HA,	new String[]{"は", "ひ", "ふ", "へ", "ほ", "゜", "゛"});
        a.append(KEYCODE_FLICK_JP_CHAR_MA,	new String[]{"ま", "み", "む", "め", "も", "",   ""});
        a.append(KEYCODE_FLICK_JP_CHAR_YA,	new String[]{"や", "「", "ゆ", "」", "よ", "小", ""});
        a.append(KEYCODE_FLICK_JP_CHAR_RA,	new String[]{"ら", "り", "る", "れ", "ろ", "",   ""});
        a.append(KEYCODE_FLICK_JP_CHAR_WA,	new String[]{"わ", "を", "ん", "ー", "",   "",   ""});
        a.append(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"、", "。", "？", "！", "",   "",   ""});
        a.append(KEYCODE_FLICK_JP_MOJI,		new String[]{"仮", "",   "数", "",   "",   "",   ""});
        a.append(Keyboard.KEYCODE_DELETE,	new String[]{"消", "戻", "",   "",   "",   "",   ""});
    }
    private static final String MOD_SMALL = "小";
    private static final String MOD_DAKUTEN = "゛";
    private static final String MOD_HANDAKUTEN = "゜";

    public FlickJPKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnKeyboardActionListener(this);
        setPreviewEnabled(false);
        setBackgroundColor(0x00000000);
    }

    public FlickJPKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnKeyboardActionListener(this);
        setPreviewEnabled(false);
        setBackgroundColor(0x00000000);
    }

    public void setService(SKKService listener) {
        mService = listener;
    }

    void setHiraganaMode() {
        isHiragana = true;
        for (Keyboard.Key key : getKeyboard().getKeys()) {
            switch (key.codes[0]) {
            case KEYCODE_FLICK_JP_CHAR_A:
                key.label = "あ";
                break;
            case KEYCODE_FLICK_JP_CHAR_KA:
                key.label = "か";
                break;
            case KEYCODE_FLICK_JP_CHAR_SA:
                key.label = "さ";
                break;
            case KEYCODE_FLICK_JP_CHAR_TA:
                key.label = "た";
                break;
            case KEYCODE_FLICK_JP_CHAR_NA:
                key.label = "な";
                break;
            case KEYCODE_FLICK_JP_CHAR_HA:
                key.label = "は";
                break;
            case KEYCODE_FLICK_JP_CHAR_MA:
                key.label = "ま";
                break;
            case KEYCODE_FLICK_JP_CHAR_YA:
                key.label = "や";
                break;
            case KEYCODE_FLICK_JP_CHAR_RA:
                key.label = "ら";
                break;
            case KEYCODE_FLICK_JP_CHAR_WA:
                key.label = "わ";
                break;
            case KEYCODE_FLICK_JP_MOJI:
                key.label = "カナ";
                break;
            }
        }
        invalidateAllKeys();
    }

    void setKatakanaMode() {
        isHiragana = false;
        for (Keyboard.Key key : getKeyboard().getKeys()) {
            switch (key.codes[0]) {
            case KEYCODE_FLICK_JP_CHAR_A:
                key.label = "ア";
                break;
            case KEYCODE_FLICK_JP_CHAR_KA:
                key.label = "カ";
                break;
            case KEYCODE_FLICK_JP_CHAR_SA:
                key.label = "サ";
                break;
            case KEYCODE_FLICK_JP_CHAR_TA:
                key.label = "タ";
                break;
            case KEYCODE_FLICK_JP_CHAR_NA:
                key.label = "ナ";
                break;
            case KEYCODE_FLICK_JP_CHAR_HA:
                key.label = "ハ";
                break;
            case KEYCODE_FLICK_JP_CHAR_MA:
                key.label = "マ";
                break;
            case KEYCODE_FLICK_JP_CHAR_YA:
                key.label = "ヤ";
                break;
            case KEYCODE_FLICK_JP_CHAR_RA:
                key.label = "ラ";
                break;
            case KEYCODE_FLICK_JP_CHAR_WA:
                key.label = "ワ";
                break;
            case KEYCODE_FLICK_JP_MOJI:
                key.label = "かな";
                break;
            }
        }
        invalidateAllKeys();
    }

    private void setAbbrevLabel(boolean isAbbrev) {
        for (Keyboard.Key key : getKeyboard().getKeys()) {
            if (key.codes[0] == KEYCODE_FLICK_JP_TOQWERTY) {
                if (isAbbrev) {
                    key.label = "abbr";
                } else {
                    key.label = "ABC";
                }
                break;
            }
        }
    }

    private void setKutoutenLabel(Keyboard keyboard, String kutouten) {
        List<Keyboard.Key> keys = keyboard.getKeys();
        for (Keyboard.Key key : keys) {
            if (key.codes[0] == KEYCODE_FLICK_JP_CHAR_TEN) {
                if (kutouten.equals("en")) {
                    key.label = "，．？！";
                } else if (kutouten.equals("jp_en")) {
                    key.label = "，。？！";
                } else {
                    key.label = "、。？！";
                }
                break;
            }
        }
    }

    private void adjustKeysHorizontally(SKKKeyboard keyboard, int maxKeyWidth, double ratio, String position) {
        int y = 0;
        int colNo = 0;
        int newWidth = (int)(maxKeyWidth*ratio);
        int gap;
        switch (position) {
            case "right":
                gap = (maxKeyWidth - newWidth)*5;
                break;
            case "center":
                gap = (int)((maxKeyWidth - newWidth)*2.5);
                break;
            default:
                gap = 0;
        }
        for (Keyboard.Key key : keyboard.getKeys()) {
            key.width = newWidth;
            if (key.y != y) {
                y = key.y;
                colNo = 0;
            }
            key.x = gap+key.width*colNo;
            colNo++;
        }
    }

    // widthはパーセントでheightはpxなので注意
    void prepareNewKeyboard(Context context, int width, int height, String position) {
        mJPKeyboard = new SKKKeyboard(context, R.xml.keys_flick_jp, 4);
        int keyWidth = mJPKeyboard.getKeys().get(0).width;
        adjustKeysHorizontally(mJPKeyboard, keyWidth, (double)width/100, position);
        mJPKeyboard.changeKeyHeight(height);
        setKeyboard(mJPKeyboard);

        mNumKeyboard = new SKKKeyboard(context, R.xml.keys_flick_number, 4);
        adjustKeysHorizontally(mNumKeyboard, keyWidth, (double)width/100, position);
        mNumKeyboard.changeKeyHeight(height);

        readPrefs(context);
    }

    private void readPrefs(Context context) {
        int sensitivity = SKKPrefs.getFlickSensitivity(context);
        mFlickSensitivitySquared = sensitivity*sensitivity;
        mUseCurve = SKKPrefs.getUseCurve(context);
        String curve = SKKPrefs.getCurveSensitivity(context);
        if (curve.equals("low")) {
            mCurveSensitivityMultiplier = 0.5f;
        } else if (curve.equals("mid")) {
            mCurveSensitivityMultiplier = 1.0f;
        } else {
            mCurveSensitivityMultiplier = 2.0f;
        }
        mUsePopup = SKKPrefs.getUsePopup(context);
        String kutouten = SKKPrefs.getKutoutenType(context);
        setKutoutenLabel(mJPKeyboard, kutouten);
        setKutoutenLabel(mNumKeyboard, kutouten);
        if (mUsePopup) {
            mFixedPopup = SKKPrefs.getFixedPopup(context);
            if (mPopup == null) {
                mPopup = createPopupGuide(context);
                mPopupTextView[0]  = (TextView)mPopup.getContentView().findViewById(R.id.labelA);
                mPopupTextView[1]  = (TextView)mPopup.getContentView().findViewById(R.id.labelI);
                mPopupTextView[2]  = (TextView)mPopup.getContentView().findViewById(R.id.labelU);
                mPopupTextView[3]  = (TextView)mPopup.getContentView().findViewById(R.id.labelE);
                mPopupTextView[4]  = (TextView)mPopup.getContentView().findViewById(R.id.labelO);
                mPopupTextView[5]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftA);
                mPopupTextView[6]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftI);
                mPopupTextView[7]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftU);
                mPopupTextView[8]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftE);
                mPopupTextView[9]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftO);
                mPopupTextView[10] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightA);
                mPopupTextView[11] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightI);
                mPopupTextView[12] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightU);
                mPopupTextView[13] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightE);
                mPopupTextView[14] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightO);
            }

            if (kutouten.equals("en")) {
                mFlickCharList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "．", "？", "！", "", "",   ""});
            } else if (kutouten.equals("jp_en")) {
                mFlickCharList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "。", "？", "！", "", "",   ""});
            } else {
                mFlickCharList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"、", "。", "？", "！", "", "",   ""});
            }
        }
    }

    private PopupWindow createPopupGuide(Context context) {
        View view = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_flickguide, null);

        float scale = getContext().getResources().getDisplayMetrics().density;
        int size = (int)(mPopupSize * scale + 0.5f);

        PopupWindow popup = new PopupWindow(view, size, size);
        //~ popup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        popup.setAnimationStyle(0);

        return popup;
    }

    private void setupPopupTextView() {
        if (!mUsePopup) {return;}

        for (int i=0; i<15; i++) {
            mPopupTextView[i].setText("");
            mPopupTextView[i].setBackgroundResource(R.drawable.popup_label);
        }

        int baseKey = 0;
        switch (mFlickState) {
            case FLICK_STATE_LEFT:
            case FLICK_STATE_LEFT_LEFT:
            case FLICK_STATE_LEFT_RIGHT:
                baseKey = 1;
                break;
            case FLICK_STATE_UP:
            case FLICK_STATE_UP_LEFT:
            case FLICK_STATE_UP_RIGHT:
                baseKey = 2;
                break;
            case FLICK_STATE_RIGHT:
            case FLICK_STATE_RIGHT_LEFT:
            case FLICK_STATE_RIGHT_RIGHT:
                baseKey = 3;
                break;
            case FLICK_STATE_DOWN:
            case FLICK_STATE_DOWN_LEFT:
            case FLICK_STATE_DOWN_RIGHT:
                baseKey = 4;
                break;
        }

        if (!mUseCurve || mFlickState == FLICK_STATE_NONE) {
            mPopupTextView[0].setText(mCurrentPopupLabels[0]);
            mPopupTextView[1].setText(mCurrentPopupLabels[1]);
            mPopupTextView[2].setText(mCurrentPopupLabels[2]);
            mPopupTextView[3].setText(mCurrentPopupLabels[3]);
            mPopupTextView[4].setText(mCurrentPopupLabels[4]);
        } else {
            mPopupTextView[baseKey].setText(mCurrentPopupLabels[baseKey]);
        }

        if (mUseCurve) {
            if (isLeftCurve(mFlickState)) {
                mPopupTextView[baseKey + 5].setText(mCurrentPopupLabels[5]);
            } else if (isRightCurve(mFlickState)) {
                mPopupTextView[baseKey + 10].setText(mCurrentPopupLabels[6]);
            } else {
                if (hasLeftCurve(mFlickState)) {
                    mPopupTextView[baseKey + 5].setText(mCurrentPopupLabels[5]);
                }
                if (hasRightCurve(mFlickState)) {
                    mPopupTextView[baseKey + 10].setText(mCurrentPopupLabels[6]);
                }
            }
        }

        for (int i=5; i<15; i++) {
            if (mPopupTextView[i].getText().equals("小")) {
                mPopupTextView[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
            } else {
                mPopupTextView[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 18);
            }
        }

        mPopupTextView[mFlickState].setBackgroundResource(R.drawable.popup_label_highlighted);
    }

    private boolean isLeftCurve(int flick) {
        return (flick == FLICK_STATE_NONE_LEFT	||
                flick == FLICK_STATE_LEFT_LEFT	||
                flick == FLICK_STATE_UP_LEFT	||
                flick == FLICK_STATE_RIGHT_LEFT	||
                flick == FLICK_STATE_DOWN_LEFT);
    }

    private boolean isRightCurve(int flick) {
        return (flick == FLICK_STATE_NONE_RIGHT	||
                flick == FLICK_STATE_LEFT_RIGHT	||
                flick == FLICK_STATE_UP_RIGHT	||
                flick == FLICK_STATE_RIGHT_RIGHT||
                flick == FLICK_STATE_DOWN_RIGHT);
    }

    private boolean hasLeftCurve(int flick) {
        switch (flick) {
            case FLICK_STATE_NONE:
                return (getModifiedKana(mCurrentPopupLabels[0], mCurrentPopupLabels[5]) != null);
            case FLICK_STATE_LEFT:
                return (getModifiedKana(mCurrentPopupLabels[1], mCurrentPopupLabels[5]) != null);
            case FLICK_STATE_UP:
                return (getModifiedKana(mCurrentPopupLabels[2], mCurrentPopupLabels[5]) != null);
            case FLICK_STATE_RIGHT:
                return (getModifiedKana(mCurrentPopupLabels[3], mCurrentPopupLabels[5]) != null);
            case FLICK_STATE_DOWN:
                return (getModifiedKana(mCurrentPopupLabels[4], mCurrentPopupLabels[5]) != null);
            default:
                return false;
        }
    }

    private boolean hasRightCurve(int flick) {
        switch (flick) {
            case FLICK_STATE_NONE:
                return (getModifiedKana(mCurrentPopupLabels[0], mCurrentPopupLabels[6]) != null);
            case FLICK_STATE_LEFT:
                return (getModifiedKana(mCurrentPopupLabels[1], mCurrentPopupLabels[6]) != null);
            case FLICK_STATE_UP:
                return (getModifiedKana(mCurrentPopupLabels[2], mCurrentPopupLabels[6]) != null);
            case FLICK_STATE_RIGHT:
                return (getModifiedKana(mCurrentPopupLabels[3], mCurrentPopupLabels[6]) != null);
            case FLICK_STATE_DOWN:
                return (getModifiedKana(mCurrentPopupLabels[4], mCurrentPopupLabels[6]) != null);
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mFlickStartX = event.getRawX();
            mFlickStartY = event.getRawY();
            break;
        case MotionEvent.ACTION_MOVE:
            if (isLeftCurve(mFlickState) || isRightCurve(mFlickState)) {return true;}

            float dx = event.getRawX() - mFlickStartX;
            float dy = event.getRawY() - mFlickStartY;
            if (mUseCurve) {
                if (dx*dx + dy*dy < mFlickSensitivitySquared) {return true;}

                if (mFlickState == FLICK_STATE_NONE) {
                    // 一回目の終了座標を記憶
                    mFlickStartX = event.getRawX();
                    mFlickStartY = event.getRawY();

                    processFirstFlick(dx, dy);
                } else {
                    processCurveFlick(dx, dy);
                }
            } else {
                processSimpleFlick(dx, dy);
            }

            if (mUsePopup) {setupPopupTextView();}
            return true;
        case MotionEvent.ACTION_UP:
            release();
        }

        return super.onTouchEvent(event);
    }

    private float diamondAngle(float x, float y) {
        if (y >= 0) {
            return (x >= 0 ? y/(x+y) : 1-x/(-x+y));
        } else {
            return (x < 0 ? 2-y/(-x-y) : 3+x/(x-y));
        }
    }

    private void processFirstFlick(float dx, float dy) {
        float d_angle = diamondAngle(dx, dy);

        if (d_angle >= 0.5f && d_angle < 1.5f) {
            mFlickState = FLICK_STATE_DOWN;
        } else if (d_angle >=  1.5f && d_angle < 2.29f) {
            mFlickState = FLICK_STATE_LEFT;
        } else if (d_angle >= 2.29f && d_angle < 2.71f) {
            if (hasLeftCurve(FLICK_STATE_NONE)) {
                mFlickState = FLICK_STATE_NONE_LEFT;
            } else if (d_angle < 2.5f) {
                mFlickState = FLICK_STATE_LEFT;
            } else {
                mFlickState = FLICK_STATE_UP;
            }
        } else if (d_angle >= 2.71f && d_angle < 3.29f) {
            mFlickState = FLICK_STATE_UP;
        } else if (d_angle >= 3.29f && d_angle < 3.71f) {
            if (hasRightCurve(FLICK_STATE_NONE)) {
                mFlickState = FLICK_STATE_NONE_RIGHT;
            } else if (d_angle < 3.5f) {
                mFlickState = FLICK_STATE_UP;
            } else {
                mFlickState = FLICK_STATE_RIGHT;
            }
        } else {
            mFlickState = FLICK_STATE_RIGHT;
        }
    }

    private void processCurveFlick(float dx, float dy) {
        if (!hasLeftCurve(mFlickState) && !hasRightCurve(mFlickState)) {return;}

        int newstate = -1;
        switch (mFlickState) {
        case FLICK_STATE_LEFT:
            if (Math.abs(dx) < mCurveSensitivityMultiplier*Math.abs(dy)) {
                newstate = dy < 0 ? FLICK_STATE_LEFT_RIGHT : FLICK_STATE_LEFT_LEFT;
            }
            break;
        case FLICK_STATE_UP:
            if (mCurveSensitivityMultiplier*Math.abs(dx) > Math.abs(dy)) {
                newstate = dx < 0 ? FLICK_STATE_UP_LEFT : FLICK_STATE_UP_RIGHT;
            }
            break;
        case FLICK_STATE_RIGHT:
            if (Math.abs(dx) < mCurveSensitivityMultiplier*Math.abs(dy)) {
                newstate = dy < 0 ? FLICK_STATE_RIGHT_LEFT : FLICK_STATE_RIGHT_RIGHT;
            }
            break;
        case FLICK_STATE_DOWN:
            if (mCurveSensitivityMultiplier*Math.abs(dx) > Math.abs(dy)) {
                newstate = dx < 0 ? FLICK_STATE_DOWN_RIGHT : FLICK_STATE_DOWN_LEFT;
            }
            break;
        }
        if (newstate == -1) {return;}

        if ((hasLeftCurve(mFlickState)  && isLeftCurve(newstate)) ||
            (hasRightCurve(mFlickState) && isRightCurve(newstate))) {
            mFlickState = newstate;
        }
    }

    private void processSimpleFlick(float x, float y) {
        if (x * x + y * y < mFlickSensitivitySquared * 9) {
            mFlickState = FLICK_STATE_NONE;
            return;
        }
        float d_angle = diamondAngle(x, y);

        if (d_angle >= 0.5f && d_angle < 1.5f) {
            mFlickState = FLICK_STATE_DOWN;
        } else if (d_angle >=  1.5f && d_angle < 2.5f) {
            mFlickState = FLICK_STATE_LEFT;
        } else if (d_angle >= 2.5f && d_angle < 3.5f) {
            mFlickState = FLICK_STATE_UP;
        } else {
            mFlickState = FLICK_STATE_RIGHT;
        }
    }

    private void processFlickForLetter(int keyCode, int flick, boolean isShifted) {
        String[] chars = mFlickCharList.get(keyCode);
        if (chars == null) return;

        String hchr = "";
        switch (flick) {
            case FLICK_STATE_NONE:
            case FLICK_STATE_NONE_LEFT:
            case FLICK_STATE_NONE_RIGHT:
                hchr = chars[0];
                break;
            case FLICK_STATE_LEFT:
            case FLICK_STATE_LEFT_LEFT:
            case FLICK_STATE_LEFT_RIGHT:
                hchr = chars[1];
                break;
            case FLICK_STATE_UP:
            case FLICK_STATE_UP_LEFT:
            case FLICK_STATE_UP_RIGHT:
                hchr = chars[2];
                break;
            case FLICK_STATE_RIGHT:
            case FLICK_STATE_RIGHT_LEFT:
            case FLICK_STATE_RIGHT_RIGHT:
                hchr = chars[3];
                break;
            case FLICK_STATE_DOWN:
            case FLICK_STATE_DOWN_LEFT:
            case FLICK_STATE_DOWN_RIGHT:
                hchr = chars[4];
                break;
        }
        if (hchr.length() == 0) {
            return;
        }

        String mod = null;
        if (isLeftCurve(flick)) {
            mod = getModifiedKana(hchr, chars[5]);
        } else if (isRightCurve(flick)) {
            mod = getModifiedKana(hchr, chars[6]);
        }
        if (mod != null) {
            hchr = mod;
        }

        mService.processText(hchr, isShifted);
    }

    private String getModifiedKana(String kana, String mod) {
        switch (mod) {
            case MOD_DAKUTEN:
                return RomajiConverter.INSTANCE.convertLastChar(kana, SKKEngine.LAST_CONVERTION_DAKUTEN);
            case MOD_HANDAKUTEN:
                return RomajiConverter.INSTANCE.convertLastChar(kana, SKKEngine.LAST_CONVERTION_HANDAKUTEN);
            case MOD_SMALL:
                return RomajiConverter.INSTANCE.convertLastChar(kana, SKKEngine.LAST_CONVERTION_SMALL);
        }
        return null;
    }

    @Override
    protected boolean onLongPress(Keyboard.Key key) {
        int code = key.codes[0];
        if (code == KEYCODE_FLICK_JP_ENTER) {
            mService.keyDownUp(KeyEvent.KEYCODE_SEARCH);
            isEnterLongPressed = true;
            return true;
        } else if (code == KEYCODE_FLICK_JP_SPACE) {
            mService.showInputMethodPicker();
            isSpaceLongPressed = true;
            return true;
        } else if (code == KEYCODE_FLICK_JP_TOQWERTY) {
            mService.showMenuDialog();
            isToQwertyLongPressed = true;
            return true;
        }

        return super.onLongPress(key);
    }

    @Override
    public void onPress(int primaryCode) {
        if (mFlickState == FLICK_STATE_NONE) {
            mLastPressedKey = primaryCode;
        }

        if (mUsePopup) {
            String[] chars = mFlickCharList.get(primaryCode);
            if (chars == null) {
                mCurrentPopupLabels = POPUP_LABELS_NULL;
                return;
            }

            for (int i=0; i<7; i++) {
                if (isHiragana) {
                    mCurrentPopupLabels[i] = chars[i];
                } else {
                    mCurrentPopupLabels[i] = SKKUtils.hirakana2katakana(chars[i]);
                }
            }
            setupPopupTextView();

            if (mFixedPopupPos[0] == 0) {calculatePopupPos();}

            if (mFixedPopup) {
                mPopup.showAtLocation(this, android.view.Gravity.NO_GRAVITY, mFixedPopupPos[0], mFixedPopupPos[1]);
            } else {
                mPopup.showAtLocation(this, android.view.Gravity.NO_GRAVITY, (int)mFlickStartX + mPopupOffset[0], (int)mFlickStartY + mPopupOffset[1]);
            }
        }
    }

    private void calculatePopupPos() {
        float scale = getContext().getResources().getDisplayMetrics().density;
        int size = (int)(mPopupSize * scale + 0.5f);

        int[] offsetInWindow = new int[2];
        getLocationInWindow(offsetInWindow);
        int[] windowLocation = new int[2];
        getLocationOnScreen(windowLocation);
        mPopupOffset[0] = -size/2;
        mPopupOffset[1] = -windowLocation[1] + offsetInWindow[1] - size/2;
        mFixedPopupPos[0] = windowLocation[0] + this.getWidth()/2 + mPopupOffset[0];
        mFixedPopupPos[1] = windowLocation[1] - size/2 + mPopupOffset[1];
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        switch (primaryCode) {
        case Keyboard.KEYCODE_SHIFT:
            setShifted(!isShifted());
            setAbbrevLabel(isShifted());
            break;
        case KEYCODE_FLICK_JP_LEFT:
            if (!mService.handleDpad(KeyEvent.KEYCODE_DPAD_LEFT)) {
                mService.keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT);
            }
            break;
        case KEYCODE_FLICK_JP_RIGHT:
            if (!mService.handleDpad(KeyEvent.KEYCODE_DPAD_RIGHT)) {
                mService.keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT);
            }
            break;
        case 48: case 49: case 50: case 51: case 52: case 53: case 54: case 55: case 56: case 57:
        // 0〜9
            mService.processKey(primaryCode);
            break;
        }
    }

    private void release() {
        switch (mLastPressedKey) {
        case KEYCODE_FLICK_JP_SPACE:
            if (isSpaceLongPressed) {
                isSpaceLongPressed = false;
            } else {
                mService.processKey(' ');
            }
            break;
        case KEYCODE_FLICK_JP_ENTER:
            if (isEnterLongPressed) {
                isEnterLongPressed = false;
            } else {
                if (!mService.handleEnter()) {
                    mService.pressEnter();
                }
            }
            break;
        case Keyboard.KEYCODE_DELETE:
            if (mFlickState == FLICK_STATE_NONE) {
                if (!mService.handleBackspace()) {
                    mService.keyDownUp(KeyEvent.KEYCODE_DEL);
                }
            } else if (mFlickState == FLICK_STATE_LEFT) {
                mService.handleCancel();
            }
            break;
        case KEYCODE_FLICK_JP_ROTATE:
            mService.changeLastChar(SKKEngine.LAST_CONVERTION_ROTATE);
            break;
        case KEYCODE_FLICK_JP_MOJI:
            if (mFlickState == FLICK_STATE_NONE) {
                mService.processKey('q');
            } else if (mFlickState == FLICK_STATE_UP && getKeyboard() == mJPKeyboard) {
                setKeyboard(mNumKeyboard);
            }
            break;
        case KEYCODE_FLICK_JP_TOKANA:
            if (getKeyboard() == mNumKeyboard) {
                setKeyboard(mJPKeyboard);
            }
            break;
        case KEYCODE_FLICK_JP_TOQWERTY:
            if (isToQwertyLongPressed) {
                isToQwertyLongPressed = false;
            } else {
                if (isShifted()) {
                    mService.processKey('/');
                } else {
                    mService.processKey('l');
                }
            }
            break;
        case KEYCODE_FLICK_JP_CHAR_A:
        case KEYCODE_FLICK_JP_CHAR_KA:
        case KEYCODE_FLICK_JP_CHAR_SA:
        case KEYCODE_FLICK_JP_CHAR_TA:
        case KEYCODE_FLICK_JP_CHAR_NA:
        case KEYCODE_FLICK_JP_CHAR_HA:
        case KEYCODE_FLICK_JP_CHAR_MA:
        case KEYCODE_FLICK_JP_CHAR_YA:
        case KEYCODE_FLICK_JP_CHAR_RA:
        case KEYCODE_FLICK_JP_CHAR_WA:
        case KEYCODE_FLICK_JP_CHAR_TEN:
            processFlickForLetter(mLastPressedKey, mFlickState, isShifted());
            break;
        }

        if (mLastPressedKey != Keyboard.KEYCODE_SHIFT) {
            setShifted(false);
            setAbbrevLabel(false);
        }

        mLastPressedKey = KEYCODE_FLICK_JP_NONE;
        mFlickState = FLICK_STATE_NONE;
        mFlickStartX = -1;
        mFlickStartY = -1;
        if (mUsePopup && mPopup.isShowing()) {mPopup.dismiss();}
    }

    @Override
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
