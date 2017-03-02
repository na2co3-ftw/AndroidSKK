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
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

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
	private static final int KEYCODE_FLICK_JP_TOQWERTY	= -1003;
	private static final int KEYCODE_FLICK_JP_SPACE		= -1004;
	private static final int KEYCODE_FLICK_JP_MOJI		= -1005;
	private static final int KEYCODE_FLICK_JP_KOMOJI	= -1006;
	private static final int KEYCODE_FLICK_JP_ENTER		= -1007;
	private static final int KEYCODE_FLICK_JP_SEARCH	= -1008;
	private static final int FLICK_STATE_NONE			= 0;
	private static final int FLICK_STATE_LEFT			= 1;
	private static final int FLICK_STATE_UP				= 2;
	private static final int FLICK_STATE_RIGHT			= 3;
	private static final int FLICK_STATE_DOWN			= 4;
	private static final int FLICK_STATE_NONE_LEFT		= 5;
	private static final int FLICK_STATE_NONE_RIGHT		= 6;
	private static final int FLICK_STATE_LEFT_LEFT		= 7;
	private static final int FLICK_STATE_LEFT_RIGHT		= 8;
	private static final int FLICK_STATE_UP_LEFT		= 9;
	private static final int FLICK_STATE_UP_RIGHT		= 10;
	private static final int FLICK_STATE_RIGHT_LEFT		= 11;
	private static final int FLICK_STATE_RIGHT_RIGHT	= 12;
	private static final int FLICK_STATE_DOWN_LEFT		= 13;
	private static final int FLICK_STATE_DOWN_RIGHT		= 14;

	private SKKEngine mService;

	private boolean isHiragana = true;

	private int mFlickSensitivitySquared = 100;
	private float mCurveSensitivityMultiplier = 2.0f;
	private int mLastPressedKey = KEYCODE_FLICK_JP_NONE;
	private int mFlickState = FLICK_STATE_NONE;
	private float mFlickStartX = -1;
	private float mFlickStartY = -1;
	private String[] mCurrentPopupLabels = new String[7];

	private boolean isEnterLongPressed = false;
	private boolean isSpaceLongPressed = false;

	private boolean mUsePopup = true;
	private boolean mFixedPopup = false;
	private PopupWindow mPopup = null;
	private TextView[] mPopupTextView = new TextView[15];
	private int mPopupSize = 120;
	private int[] mPopupOffset = {0, 0};
	private int[] mFixedPopupPos = {0, 0};

	//フリックガイドTextView用
	private SparseArray<String[]> mFlickGuideLabelList = new SparseArray<String[]>();
	{
		SparseArray<String[]> a = mFlickGuideLabelList;
		a.append(KEYCODE_FLICK_JP_CHAR_A,	new String[]{"あ", "い", "う", "え", "お", "小", ""});
		a.append(KEYCODE_FLICK_JP_CHAR_KA,	new String[]{"か", "き", "く", "け", "こ", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_SA,	new String[]{"さ", "し", "す", "せ", "そ", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_TA,	new String[]{"た", "ち", "つ", "て", "と", "",   "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_NA,	new String[]{"な", "に", "ぬ", "ね", "の", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_HA,	new String[]{"は", "ひ", "ふ", "へ", "ほ", "゜", "゛"});
		a.append(KEYCODE_FLICK_JP_CHAR_MA,	new String[]{"ま", "み", "む", "め", "も", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_YA,	new String[]{"や", "",   "ゆ", "",   "よ", "小", ""});
		a.append(KEYCODE_FLICK_JP_CHAR_RA,	new String[]{"ら", "り", "る", "れ", "ろ", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_WA,	new String[]{"わ", "を", "ん", "ー", "「", "",   ""});
		a.append(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"、", "。", "？", "！", "」", "",   ""});
	}

	// かな小文字変換用
	private Map<String, String> mSmallKanaMap = new HashMap<String, String>();
	{
		Map<String, String> m = mSmallKanaMap;
		m.put("あ", "ぁ");m.put("い", "ぃ");m.put("う", "ぅ");m.put("え", "ぇ");m.put("お", "ぉ");
		m.put("ぁ", "あ");m.put("ぃ", "い");m.put("ぅ", "う");m.put("ぇ", "え");m.put("ぉ", "お");
		m.put("や", "ゃ");m.put("ゆ", "ゅ");m.put("よ", "ょ");m.put("つ", "っ");
		m.put("ゃ", "や");m.put("ゅ", "ゆ");m.put("ょ", "よ");m.put("っ", "つ");
		m.put("ア", "ァ");m.put("イ", "ィ");m.put("ウ", "ゥ");m.put("エ", "ェ");m.put("オ", "ォ");
		m.put("ァ", "ア");m.put("ィ", "イ");m.put("ゥ", "ウ");m.put("ェ", "エ");m.put("ォ", "オ");
		m.put("ヤ", "ャ");m.put("ユ", "ュ");m.put("ヨ", "ョ");m.put("ツ", "ッ");
		m.put("ャ", "ヤ");m.put("ュ", "ユ");m.put("ョ", "ヨ");m.put("ッ", "ツ");
	}
	// 濁音変換用
	private Map<String, String> mDakutenMap = new HashMap<String, String>();
	{
		Map<String, String> m = mDakutenMap;
		m.put("か", "が");m.put("き", "ぎ");m.put("く", "ぐ");m.put("け", "げ");m.put("こ", "ご");
		m.put("が", "か");m.put("ぎ", "き");m.put("ぐ", "く");m.put("げ", "け");m.put("ご", "こ");
		m.put("さ", "ざ");m.put("し", "じ");m.put("す", "ず");m.put("せ", "ぜ");m.put("そ", "ぞ");
		m.put("ざ", "さ");m.put("じ", "し");m.put("ず", "す");m.put("ぜ", "せ");m.put("ぞ", "そ");
		m.put("た", "だ");m.put("ち", "ぢ");m.put("つ", "づ");m.put("て", "で");m.put("と", "ど");
		m.put("だ", "た");m.put("ぢ", "ち");m.put("づ", "つ");m.put("で", "て");m.put("ど", "と");
		m.put("は", "ば");m.put("ひ", "び");m.put("ふ", "ぶ");m.put("へ", "べ");m.put("ほ", "ぼ");
		m.put("ば", "は");m.put("び", "ひ");m.put("ぶ", "ふ");m.put("べ", "へ");m.put("ぼ", "ほ");
		m.put("カ", "ガ");m.put("キ", "ギ");m.put("ク", "グ");m.put("ケ", "ゲ");m.put("コ", "ゴ");
		m.put("ガ", "カ");m.put("ギ", "キ");m.put("グ", "ク");m.put("ゲ", "ケ");m.put("ゴ", "コ");
		m.put("サ", "ザ");m.put("シ", "ジ");m.put("ス", "ズ");m.put("セ", "セ");m.put("ソ", "ゾ");
		m.put("ザ", "サ");m.put("ジ", "シ");m.put("ズ", "ス");m.put("ゼ", "ゼ");m.put("ゾ", "ソ");
		m.put("タ", "ダ");m.put("チ", "ヂ");m.put("ツ", "ヅ");m.put("テ", "デ");m.put("ト", "ド");
		m.put("ダ", "タ");m.put("ヂ", "チ");m.put("ヅ", "ツ");m.put("デ", "テ");m.put("ド", "ト");
		m.put("ハ", "バ");m.put("ヒ", "ビ");m.put("フ", "ブ");m.put("ヘ", "ベ");m.put("ホ", "ボ");
		m.put("バ", "ハ");m.put("ビ", "ヒ");m.put("ブ", "フ");m.put("ベ", "ヘ");m.put("ボ", "ホ");
		m.put("ウ", "ヴ");m.put("ヴ", "ウ");
	}
	// 濁音変換用
	private Map<String, String> mHanDakutenMap = new HashMap<String, String>();
	{
		Map<String, String> m = mHanDakutenMap;
		m.put("は", "ぱ");m.put("ひ", "ぴ");m.put("ふ", "ぷ");m.put("へ", "ぺ");m.put("ほ", "ぽ");
		m.put("ぱ", "は");m.put("ぴ", "ひ");m.put("ぷ", "ふ");m.put("ぺ", "へ");m.put("ぽ", "ほ");
		m.put("ハ", "パ");m.put("ヒ", "ピ");m.put("フ", "プ");m.put("ヘ", "ペ");m.put("ホ", "ポ");
		m.put("パ", "ハ");m.put("ピ", "ヒ");m.put("プ", "フ");m.put("ペ", "ヘ");m.put("ポ", "ホ");
	}

	private Map<String, String> mRotateKanaMap = new HashMap<String, String>();
	{
		Map<String, String> m = mRotateKanaMap;
		m.put("あ", "ぁ");m.put("い", "ぃ");m.put("う", "ぅ");m.put("え", "ぇ");m.put("お", "ぉ");
		m.put("ぁ", "あ");m.put("ぃ", "い");m.put("ぅ", "う");m.put("ぇ", "え");m.put("ぉ", "お");
		m.put("か", "が");m.put("き", "ぎ");m.put("く", "ぐ");m.put("け", "げ");m.put("こ", "ご");
		m.put("が", "か");m.put("ぎ", "き");m.put("ぐ", "く");m.put("げ", "け");m.put("ご", "こ");
		m.put("さ", "ざ");m.put("し", "じ");m.put("す", "ず");m.put("せ", "ぜ");m.put("そ", "ぞ");
		m.put("ざ", "さ");m.put("じ", "し");m.put("ず", "す");m.put("ぜ", "せ");m.put("ぞ", "そ");
		m.put("た", "だ");m.put("ち", "ぢ");m.put("つ", "っ");m.put("て", "で");m.put("と", "ど");
		m.put("だ", "た");m.put("ぢ", "ち");m.put("っ", "づ");m.put("で", "て");m.put("ど", "と");
		                                   m.put("づ", "つ");
		m.put("は", "ば");m.put("ひ", "び");m.put("ふ", "ぶ");m.put("へ", "べ");m.put("ほ", "ぼ");
		m.put("ば", "ぱ");m.put("び", "ぴ");m.put("ぶ", "ぷ");m.put("べ", "ぺ");m.put("ぼ", "ぽ");
		m.put("ぱ", "は");m.put("ぴ", "ひ");m.put("ぷ", "ふ");m.put("ぺ", "へ");m.put("ぽ", "ほ");
		m.put("や", "ゃ");m.put("ゆ", "ゅ");m.put("よ", "ょ");
		m.put("ゃ", "や");m.put("ゅ", "ゆ");m.put("ょ", "よ");
		m.put("ア", "ァ");m.put("イ", "ィ");m.put("ウ", "ゥ");m.put("エ", "ェ");m.put("オ", "ォ");
		m.put("ァ", "ア");m.put("ィ", "イ");m.put("ゥ", "ヴ");m.put("ェ", "エ");m.put("ォ", "オ");
		                                   m.put("ヴ", "ウ");
		m.put("カ", "ガ");m.put("キ", "ギ");m.put("ク", "グ");m.put("ケ", "ゲ");m.put("コ", "ゴ");
		m.put("ガ", "カ");m.put("ギ", "キ");m.put("グ", "ク");m.put("ゲ", "ケ");m.put("ゴ", "コ");
		m.put("サ", "ザ");m.put("シ", "ジ");m.put("ス", "ズ");m.put("セ", "セ");m.put("ソ", "ゾ");
		m.put("ザ", "サ");m.put("ジ", "シ");m.put("ズ", "ス");m.put("ゼ", "ゼ");m.put("ゾ", "ソ");
		m.put("タ", "ダ");m.put("チ", "ヂ");m.put("ツ", "ッ");m.put("テ", "デ");m.put("ト", "ド");
		m.put("ダ", "タ");m.put("ヂ", "チ");m.put("ッ", "ヅ");m.put("デ", "テ");m.put("ド", "ト");
	                                       m.put("ヅ", "ツ");
		m.put("ハ", "バ");m.put("ヒ", "ビ");m.put("フ", "ブ");m.put("ヘ", "ベ");m.put("ホ", "ボ");
		m.put("バ", "パ");m.put("ビ", "ピ");m.put("ブ", "プ");m.put("ベ", "ペ");m.put("ボ", "ポ");
		m.put("パ", "ハ");m.put("ピ", "ヒ");m.put("プ", "フ");m.put("ペ", "ヘ");m.put("ポ", "ホ");
		m.put("ヤ", "ャ");m.put("ユ", "ュ");m.put("ヨ", "ョ");
		m.put("ャ", "ヤ");m.put("ュ", "ユ");m.put("ョ", "ヨ");
	}

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

	public void setService(SKKEngine listener) {
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

	void setKutoutenLabel(String kutouten) {
		for (Keyboard.Key key : getKeyboard().getKeys()) {
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

	void changeKeyHeight(int px) {
		((SKKKeyboard)getKeyboard()).changeKeyHeight(px);
	}

	// widthはパーセントでheightはpxなので注意
	void prepareNewKeyboard(Context context, int width, int height, String position) {
		String resName = String.format("keys_flick_jp_%d_%s", width, position);

		int ID = context.getResources().getIdentifier(resName, "xml", context.getPackageName());
		SKKKeyboard keyboard;
		if (ID != 0) {
			keyboard = new SKKKeyboard(context, ID);
		} else {
			keyboard = new SKKKeyboard(context, R.xml.keys_flick_jp);
		}

		setKeyboard(keyboard);
		changeKeyHeight(height);
	}

	void readPrefs(Context context) {
		int sensitivity = SKKPrefs.getFlickSensitivity(context);
		mFlickSensitivitySquared = sensitivity*sensitivity;
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
		setKutoutenLabel(kutouten);
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
				mPopupTextView[6]  = (TextView)mPopup.getContentView().findViewById(R.id.labelRightA);
				mPopupTextView[7]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftI);
				mPopupTextView[8]  = (TextView)mPopup.getContentView().findViewById(R.id.labelRightI);
				mPopupTextView[9]  = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftU);
				mPopupTextView[10] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightU);
				mPopupTextView[11] = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftE);
				mPopupTextView[12] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightE);
				mPopupTextView[13] = (TextView)mPopup.getContentView().findViewById(R.id.labelLeftO);
				mPopupTextView[14] = (TextView)mPopup.getContentView().findViewById(R.id.labelRightO);
			}

			if (kutouten.equals("en")) {
				mFlickGuideLabelList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "．", "？", "！", "」", "",   ""});
			} else if (kutouten.equals("jp_en")) {
				mFlickGuideLabelList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"，", "。", "？", "！", "」", "",   ""});
			} else {
				mFlickGuideLabelList.put(KEYCODE_FLICK_JP_CHAR_TEN,	new String[]{"、", "。", "？", "！", "」", "",   ""});
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

		SKKUtils.dlog("popup created.  width=" + popup.getWidth() + " height=" + popup.getHeight());
		return popup;
	}

	private void setupPopupTextView() {
		if (!mUsePopup) {return;}

		for (int i=0; i<15; i++) {
			mPopupTextView[i].setText("");
			mPopupTextView[i].setBackgroundResource(R.drawable.popup_label);
		}
		switch (mFlickState) {
		case FLICK_STATE_NONE:
			mPopupTextView[0].setText(mCurrentPopupLabels[0]);
			mPopupTextView[1].setText(mCurrentPopupLabels[1]);
			mPopupTextView[2].setText(mCurrentPopupLabels[2]);
			mPopupTextView[3].setText(mCurrentPopupLabels[3]);
			mPopupTextView[4].setText(mCurrentPopupLabels[4]);
			mPopupTextView[5].setText(mCurrentPopupLabels[5]);
			mPopupTextView[6].setText(mCurrentPopupLabels[6]);
			mPopupTextView[0].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_NONE_LEFT:
			mPopupTextView[0].setText(mCurrentPopupLabels[0]);
			mPopupTextView[5].setText(mCurrentPopupLabels[5]);
			mPopupTextView[5].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_NONE_RIGHT:
			mPopupTextView[0].setText(mCurrentPopupLabels[0]);
			mPopupTextView[6].setText(mCurrentPopupLabels[6]);
			mPopupTextView[6].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_LEFT:
			mPopupTextView[1].setText(mCurrentPopupLabels[1]);
			mPopupTextView[7].setText(mCurrentPopupLabels[5]);
			mPopupTextView[8].setText(mCurrentPopupLabels[6]);
			mPopupTextView[1].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_LEFT_LEFT:
			mPopupTextView[1].setText(mCurrentPopupLabels[1]);
			mPopupTextView[7].setText(mCurrentPopupLabels[5]);
			mPopupTextView[7].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_LEFT_RIGHT:
			mPopupTextView[1].setText(mCurrentPopupLabels[1]);
			mPopupTextView[8].setText(mCurrentPopupLabels[6]);
			mPopupTextView[8].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_UP:
			mPopupTextView[2].setText(mCurrentPopupLabels[2]);
			mPopupTextView[9].setText(mCurrentPopupLabels[5]);
			mPopupTextView[10].setText(mCurrentPopupLabels[6]);
			if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_TA) {
				// 例外：小さい「っ」
				mPopupTextView[9].setText("小");
			}
			if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_A && !isHiragana) {
				// 例外：「ヴ」
				mPopupTextView[10].setText("゛");
			}
			mPopupTextView[2].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_UP_LEFT:
			mPopupTextView[2].setText(mCurrentPopupLabels[2]);
			mPopupTextView[9].setText(mCurrentPopupLabels[5]);
			if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_TA) {
				// 例外：小さい「っ」
				mPopupTextView[9].setText("小");
			}
			mPopupTextView[9].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_UP_RIGHT:
			mPopupTextView[2].setText(mCurrentPopupLabels[2]);
			mPopupTextView[10].setText(mCurrentPopupLabels[6]);
			if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_A && !isHiragana) {
				// 例外：「ヴ」
				mPopupTextView[10].setText("゛");
			}
			mPopupTextView[10].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_RIGHT:
			mPopupTextView[3].setText(mCurrentPopupLabels[3]);
			mPopupTextView[11].setText(mCurrentPopupLabels[5]);
			mPopupTextView[12].setText(mCurrentPopupLabels[6]);
			mPopupTextView[3].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_RIGHT_LEFT:
			mPopupTextView[3].setText(mCurrentPopupLabels[3]);
			mPopupTextView[11].setText(mCurrentPopupLabels[5]);
			mPopupTextView[11].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_RIGHT_RIGHT:
			mPopupTextView[3].setText(mCurrentPopupLabels[3]);
			mPopupTextView[12].setText(mCurrentPopupLabels[6]);
			mPopupTextView[12].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_DOWN:
			mPopupTextView[4].setText(mCurrentPopupLabels[4]);
			mPopupTextView[13].setText(mCurrentPopupLabels[5]);
			mPopupTextView[14].setText(mCurrentPopupLabels[6]);
			mPopupTextView[4].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_DOWN_LEFT:
			mPopupTextView[4].setText(mCurrentPopupLabels[4]);
			mPopupTextView[13].setText(mCurrentPopupLabels[5]);
			mPopupTextView[13].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		case FLICK_STATE_DOWN_RIGHT:
			mPopupTextView[4].setText(mCurrentPopupLabels[4]);
			mPopupTextView[14].setText(mCurrentPopupLabels[6]);
			mPopupTextView[14].setBackgroundResource(R.drawable.popup_label_highlighted);
			break;
		}
		for (int i=5; i<15; i++) {
			if (mPopupTextView[i].getText().equals("小")) {
				mPopupTextView[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
			} else {
				mPopupTextView[i].setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 18);
			}
		}
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
			if (dx*dx + dy*dy < mFlickSensitivitySquared) {return true;}

			if (mFlickState == FLICK_STATE_NONE) {
				// 一回目の終了座標を記憶
				mFlickStartX = event.getRawX();
				mFlickStartY = event.getRawY();

				processFirstFlick(dx, dy);
			} else {
				processCurveFlick(dx, dy);
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
		boolean hasLeftCurve =  (mCurrentPopupLabels[5].length() != 0);
		boolean hasRightCurve = (mCurrentPopupLabels[6].length() != 0);

		if (d_angle >= 0.5f && d_angle < 1.5f) {
			mFlickState = FLICK_STATE_DOWN;
		} else if (d_angle >=  1.5f && d_angle < 2.29f) {
			mFlickState = FLICK_STATE_LEFT;
		} else if (d_angle >= 2.29f && d_angle < 2.71f) {
			if (hasLeftCurve) {
				mFlickState = FLICK_STATE_NONE_LEFT;
			} else if (d_angle < 2.5f) {
				mFlickState = FLICK_STATE_LEFT;
			} else {
				mFlickState = FLICK_STATE_UP;
			}
		} else if (d_angle >= 2.71f && d_angle < 3.29f) {
			mFlickState = FLICK_STATE_UP;
		} else if (d_angle >= 3.29f && d_angle < 3.71f) {
			if (hasRightCurve) {
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
		boolean hasLeftCurve =  (mCurrentPopupLabels[5].length() != 0);
		boolean hasRightCurve = (mCurrentPopupLabels[6].length() != 0);
		//小さい「っ」は特別処理
		if (mLastPressedKey == KEYCODE_FLICK_JP_CHAR_TA && mFlickState == FLICK_STATE_UP) {hasLeftCurve = true;}
		//「ヴ」は特別処理
		if (!isHiragana && mLastPressedKey == KEYCODE_FLICK_JP_CHAR_A && mFlickState == FLICK_STATE_UP) {hasRightCurve = true;}
		if (!hasLeftCurve && !hasRightCurve) {return;}

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

		if ((hasLeftCurve && isLeftCurve(newstate)) || (hasRightCurve && isRightCurve(newstate))) {
			mFlickState = newstate;
		}
	}

	private void processFlickForLetter(int keyCode, int flick, boolean isShifted) {
		int vowel ='a';
		switch (flick) {
		case FLICK_STATE_LEFT:
		case FLICK_STATE_LEFT_LEFT:
		case FLICK_STATE_LEFT_RIGHT:
			vowel = 'i';
			break;
		case FLICK_STATE_UP:
		case FLICK_STATE_UP_LEFT:
		case FLICK_STATE_UP_RIGHT:
			vowel = 'u';
			break;
		case FLICK_STATE_RIGHT:
		case FLICK_STATE_RIGHT_LEFT:
		case FLICK_STATE_RIGHT_RIGHT:
			vowel = 'e';
			break;
		case FLICK_STATE_DOWN:
		case FLICK_STATE_DOWN_LEFT:
		case FLICK_STATE_DOWN_RIGHT:
			vowel = 'o';
			break;
		}

		int consonant = -1;
		switch (keyCode) {
		case KEYCODE_FLICK_JP_CHAR_A:
			if (isLeftCurve(flick)) {
				mService.processKey('x');
				mService.processKey(vowel);
			} else if (!isHiragana && flick == FLICK_STATE_UP_RIGHT) {
				mService.processKey('v');
				mService.processKey('u');
			} else if (isShifted) {
				mService.processKey(Character.toUpperCase(vowel));
			} else {
				mService.processKey(vowel);
			}
			return;
		case KEYCODE_FLICK_JP_CHAR_KA:
			if (isRightCurve(flick)) {
				consonant = 'g';
			} else {
				consonant = 'k';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_SA:
			if (isRightCurve(flick)) {
				consonant = 'z';
			} else {
				consonant = 's';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_TA:
			if (isRightCurve(flick)) {
				consonant = 'd';
			} else {
				consonant = 't';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_NA:
			consonant = 'n';
			break;
		case KEYCODE_FLICK_JP_CHAR_HA:
			if (isRightCurve(flick)) {
				consonant = 'b';
			} else if (isLeftCurve(flick)) {
				consonant = 'p';
			} else {
				consonant = 'h';
			}
			break;
		case KEYCODE_FLICK_JP_CHAR_MA:
			consonant = 'm';
			break;
		case KEYCODE_FLICK_JP_CHAR_YA:
			consonant = 'y';
			break;
		case KEYCODE_FLICK_JP_CHAR_RA:
			consonant = 'r';
			break;
		case KEYCODE_FLICK_JP_CHAR_WA:
			switch (flick) {
			case FLICK_STATE_NONE:
				if (isShifted) {
					mService.processKey('W');
				} else {
					mService.processKey('w');
				}
				mService.processKey('a');
				break;
			case FLICK_STATE_LEFT:
				mService.processKey('w');
				mService.processKey('o');
				break;
			case FLICK_STATE_UP:
				if (isShifted) {
					mService.processKey('N');
				} else {
					mService.processKey('n');
				}
				mService.processKey('n');
				break;
			case FLICK_STATE_RIGHT:
				mService.processKey('-');
				break;
			case FLICK_STATE_DOWN:
				mService.processKey('[');
				break;
			}
			return;
		case KEYCODE_FLICK_JP_CHAR_TEN:
			switch (flick) {
			case FLICK_STATE_NONE:
				mService.processKey(',');
				break;
			case FLICK_STATE_LEFT:
				mService.processKey('.');
				break;
			case FLICK_STATE_UP:
				mService.processKey('?');
				break;
			case FLICK_STATE_RIGHT:
				mService.processKey('!');
				break;
			case FLICK_STATE_DOWN:
				mService.processKey(']');
				break;
			}
			return;
		default:
			return;
		}

		if (isShifted) {
			mService.processKey(Character.toUpperCase(consonant));
		} else {
			mService.processKey(consonant);
		}
		mService.processKey(vowel);

		if (isLeftCurve(flick)) {
			if ((consonant == 't' && vowel == 'u') || (consonant == 'y' && (vowel == 'a' || vowel == 'u' || vowel == 'o'))) {
				mService.changeLastChar(mSmallKanaMap);
			}
		}
	}

	@Override
	protected boolean onLongPress(Keyboard.Key key) {
		int code = key.codes[0];
		if (code == KEYCODE_FLICK_JP_ENTER) {
			mService.keyDownUp(KeyEvent.KEYCODE_SEARCH);
			isEnterLongPressed = true;
			return true;
		} else if (code == KEYCODE_FLICK_JP_SPACE) {
			//mService.sendToMushroom();
			((InputMethodManager)mService.getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
			isSpaceLongPressed = true;
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
			String[] labels = mFlickGuideLabelList.get(primaryCode);
			if (labels == null) return;

			for (int i=0; i<7; i++) {
				if (isHiragana) {
					mCurrentPopupLabels[i] = labels[i];
				} else {
					mCurrentPopupLabels[i] = SKKUtils.hirakana2katakana(labels[i]);
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
			break;
		case Keyboard.KEYCODE_DELETE:
			if (!mService.handleBackspace()) {
				mService.keyDownUp(KeyEvent.KEYCODE_DEL);
			}
			break;
		case KEYCODE_FLICK_JP_LEFT:
			if (!mService.handleLeftKey()) {
				mService.keyDownUp(KeyEvent.KEYCODE_DPAD_LEFT);
			}
			break;
		case KEYCODE_FLICK_JP_RIGHT:
			if (!mService.handleRightKey()) {
				mService.keyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
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
		case KEYCODE_FLICK_JP_KOMOJI:
			mService.changeLastChar(mRotateKanaMap);
			break;
		case KEYCODE_FLICK_JP_MOJI:
			mService.processKey('q');
			break;
		case KEYCODE_FLICK_JP_TOQWERTY:
			mService.toggleSKK();
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
