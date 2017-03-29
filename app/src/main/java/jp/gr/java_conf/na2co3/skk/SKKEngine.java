package jp.gr.java_conf.na2co3.skk;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static jp.gr.java_conf.na2co3.skk.InputState.*;
import static jp.gr.java_conf.na2co3.skk.InputMode.*;

public class SKKEngine extends InputMethodService {
	private CandidateViewContainer mCandidateViewContainer;
	private CandidateView mCandidateView;
	private FlickJPKeyboardView mFlickJPInputView = null;
	private QwertyKeyboardView mQwertyInputView = null;
	private int mScreenHeight;
	private int mScreenWidth;

	public int mChoosedIndex;
	// 候補はsuggestionsと違ってハードキーモードでも使う（CandidateViewが出てないときがある）ので
	// こっちでもListを保持する必要がある
	private List<String> mCandidateList;

	private InputMode mInputMode = HIRAKANA;
	private InputState mInputState = NORMAL;

	private boolean isSKKOn = true;

	// onKeyDown()でEnterキーのイベントを消費したかどうかのフラグ．onKeyUp()で判定するのに使う
	private boolean isEnterUsed = false;

	// ひらがなや英単語などの入力途中
	private StringBuilder mComposing = new StringBuilder();
	// 漢字変換のキー 送りありの場合最後がアルファベット 変換中は不変
	private StringBuilder mKanji = new StringBuilder();
	// 送りがな 「っ」や「ん」が含まれる場合だけ二文字になる
	private String mOkurigana = null;

	// 単語登録の状況
	private Stack<RegisterInfo> mRegisterStack = new Stack<>();

	private class RegisterInfo {
		String mKey;	// 単語登録のキー
		String mOkurigana = null;
		StringBuilder mEntry = new StringBuilder();	// 単語登録する内容

		RegisterInfo(String key, String okurigana) {
			mKey = key;
			mOkurigana = okurigana;
		}
	}

	private boolean isCandidatesViewShown = false;
	private int mCandidatesOpCount = 0;
	private int mOpCount = 0;

	private static final String DICTIONARY = "skk_dict_btree";
	private static final String USER_DICT = "skk_userdict";
	private DictionaryManager mDictionary;

	private SKKMetaKey mMetaKey = new SKKMetaKey(this);
	private boolean mStickyMeta = false;
	private boolean mSandS = false;
	private boolean mSpacePressed = false;
	private boolean mSandSUsed = false;

	private boolean mUseSoftKeyboard = false;

	private class ConversionInfo {
		String candidate;
		List<String> list;
		int index;
		int opCount;
		String kanjiKey;
		String okuri;

		ConversionInfo(String cand, List<String> clist, int idx, int count, String key, String okuri) {
			this.candidate = cand;
			this.list = clist;
			this.index = idx;
			this.opCount = count;
			this.kanjiKey = key;
			this.okuri = okuri;
		}
	}
	private ConversionInfo mLastConversion = null;

	private BroadcastReceiver mMushroomReceiver;
	private String mMushroomWord = null;
	private Handler hMushroom = new Handler();
	private Runnable rMushroom = new Runnable() {
		public void run() {
			if (mMushroomWord != null) {
				if (mMushroomWord.length() > 0) {
					getCurrentInputConnection().commitText(mMushroomWord, 1);
					mMushroomWord = null;
					keyDownUp(KeyEvent.KEYCODE_DPAD_CENTER);
				}
			}
		}
	};

	static final String ACTION_COMMIT_USERDIC = "jp.gr.java_conf.na2co3.skk.ACTION_COMMIT_USERDIC";
	static final String ACTION_RELOAD_USERDIC = "jp.gr.java_conf.na2co3.skk.ACTION_RELOAD_USERDIC";
	static final String ACTION_READ_PREFS = "jp.gr.java_conf.na2co3.skk.ACTION_READ_PREFS";

	// ローマ字辞書
	private Map<String, String> mRomajiMap = new HashMap<String, String>();
	{
		Map<String, String> m = mRomajiMap;
		m.put("a", "あ");m.put("i", "い");m.put("u", "う");m.put("e", "え");m.put("o", "お");
		m.put("ka", "か");m.put("ki", "き");m.put("ku", "く");m.put("ke", "け");m.put("ko", "こ");
		m.put("sa", "さ");m.put("si", "し");m.put("su", "す");m.put("se", "せ");m.put("so", "そ");
		m.put("ta", "た");m.put("ti", "ち");m.put("tu", "つ");m.put("te", "て");m.put("to", "と");
		m.put("na", "な");m.put("ni", "に");m.put("nu", "ぬ");m.put("ne", "ね");m.put("no", "の");
		m.put("ha", "は");m.put("hi", "ひ");m.put("hu", "ふ");m.put("he", "へ");m.put("ho", "ほ");
		m.put("ma", "ま");m.put("mi", "み");m.put("mu", "む");m.put("me", "め");m.put("mo", "も");
		m.put("ya", "や");m.put("yi", "い");m.put("yu", "ゆ");m.put("ye", "いぇ");m.put("yo", "よ");
		m.put("ra", "ら");m.put("ri", "り");m.put("ru", "る");m.put("re", "れ");m.put("ro", "ろ");
		m.put("wa", "わ");m.put("wi", "うぃ");m.put("we", "うぇ");m.put("wo", "を");m.put("nn", "ん");
		m.put("ga", "が");m.put("gi", "ぎ");m.put("gu", "ぐ");m.put("ge", "げ");m.put("go", "ご");
		m.put("za", "ざ");m.put("zi", "じ");m.put("zu", "ず");m.put("ze", "ぜ");m.put("zo", "ぞ");
		m.put("da", "だ");m.put("di", "ぢ");m.put("du", "づ");m.put("de", "で");m.put("do", "ど");
		m.put("ba", "ば");m.put("bi", "び");m.put("bu", "ぶ");m.put("be", "べ");m.put("bo", "ぼ");
		m.put("pa", "ぱ");m.put("pi", "ぴ");m.put("pu", "ぷ");m.put("pe", "ぺ");m.put("po", "ぽ");
		m.put("va", "ゔぁ");m.put("vi", "ゔぃ");m.put("vu", "ゔ");m.put("ve", "ゔぇ");m.put("vo", "ゔぉ");

		m.put("xa", "ぁ");m.put("xi", "ぃ");m.put("xu", "ぅ");m.put("xe", "ぇ");m.put("xo", "ぉ");
		m.put("xtu", "っ");m.put("xke", "ヶ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("fa", "ふぁ");m.put("fi", "ふぃ");m.put("fu", "ふ");m.put("fe", "ふぇ");m.put("fo", "ふぉ");

		m.put("xya", "ゃ");                 m.put("xyu", "ゅ");                 m.put("xyo", "ょ");
		m.put("kya", "きゃ");               m.put("kyu", "きゅ");               m.put("kyo", "きょ");
		m.put("gya", "ぎゃ");               m.put("gyu", "ぎゅ");               m.put("gyo", "ぎょ");
		m.put("sya", "しゃ");               m.put("syu", "しゅ");               m.put("syo", "しょ");
		m.put("sha", "しゃ");m.put("shi", "し");m.put("shu", "しゅ");m.put("she", "しぇ");m.put("sho", "しょ");
		m.put("ja",  "じゃ");m.put("ji",  "じ");m.put("ju", "じゅ");m.put("je", "じぇ");m.put("jo", "じょ");
		m.put("cha", "ちゃ");m.put("chi", "ち");m.put("chu", "ちゅ");m.put("che", "ちぇ");m.put("cho", "ちょ");
		m.put("tya", "ちゃ");               m.put("tyu", "ちゅ");m.put("tye", "ちぇ");m.put("tyo", "ちょ");
		m.put("tha", "てゃ");m.put("thi", "てぃ");m.put("thu", "てゅ");m.put("the", "てぇ");m.put("tho", "てょ");
		m.put("dha", "でゃ");m.put("dhi", "でぃ");m.put("dhu", "でゅ");m.put("dhe", "でぇ");m.put("dho", "でょ");
		m.put("dya", "ぢゃ");m.put("dyi", "ぢぃ");m.put("dyu", "ぢゅ");m.put("dye", "ぢぇ");m.put("dyo", "ぢょ");
		m.put("nya", "にゃ");               m.put("nyu", "にゅ");               m.put("nyo", "にょ");
		m.put("hya", "ひゃ");               m.put("hyu", "ひゅ");               m.put("hyo", "ひょ");
		m.put("pya", "ぴゃ");               m.put("pyu", "ぴゅ");               m.put("pyo", "ぴょ");
		m.put("bya", "びゃ");               m.put("byu", "びゅ");               m.put("byo", "びょ");
		m.put("mya", "みゃ");               m.put("myu", "みゅ");               m.put("myo", "みょ");
		m.put("rya", "りゃ");               m.put("ryu", "りゅ");m.put("rye", "りぇ");m.put("ryo", "りょ");
		m.put("z,", "‥");m.put("z-", "〜");m.put("z.", "…");m.put("z/", "・");m.put("z[", "『");m.put("z]", "』");m.put("zh", "←");m.put("zj", "↓");m.put("zk", "↑");m.put("zl", "→");
	}
	// 全角で入力する記号リスト
	private Map<String, String> mZenkakuSeparatorMap = new HashMap<String, String>();
	{
		Map<String, String> m = mZenkakuSeparatorMap;
		m.put("-", "ー");m.put("!", "！");m.put("?", "？");m.put("~", "〜");m.put("[", "「");m.put("]", "」");
	}

	// フリック入力用
	private Map<String, String> mConsonantMap = new HashMap<String, String>();
	{
		Map<String, String> m = mConsonantMap;
		m.put("あ", "a");m.put("い", "i");m.put("う", "u");m.put("え", "e");m.put("お", "o");
		m.put("ぁ", "x");m.put("ぃ", "x");m.put("ぅ", "x");m.put("ぇ", "x");m.put("ぉ", "x");
		m.put("か", "k");m.put("き", "k");m.put("く", "k");m.put("け", "k");m.put("こ", "k");
		m.put("が", "g");m.put("ぎ", "g");m.put("ぐ", "g");m.put("げ", "g");m.put("ご", "g");
		m.put("さ", "s");m.put("し", "s");m.put("す", "s");m.put("せ", "s");m.put("そ", "s");
		m.put("ざ", "z");m.put("じ", "z");m.put("ず", "z");m.put("ぜ", "z");m.put("ぞ", "z");
		m.put("た", "t");m.put("ち", "t");m.put("つ", "t");m.put("て", "t");m.put("と", "t");
		m.put("だ", "d");m.put("ぢ", "d");m.put("づ", "d");m.put("で", "d");m.put("ど", "d");
		m.put("な", "n");m.put("に", "n");m.put("ぬ", "n");m.put("ね", "n");m.put("の", "n");
		m.put("は", "h");m.put("ひ", "h");m.put("ふ", "h");m.put("へ", "h");m.put("ほ", "h");
		m.put("ば", "b");m.put("び", "b");m.put("ぶ", "b");m.put("べ", "b");m.put("ぼ", "b");
		m.put("ぱ", "p");m.put("ぴ", "p");m.put("ぷ", "p");m.put("ぺ", "p");m.put("ぽ", "p");
		m.put("ま", "m");m.put("み", "m");m.put("む", "m");m.put("め", "m");m.put("も", "m");
		m.put("や", "y");                 m.put("ゆ", "y");                 m.put("よ", "y");
		m.put("ら", "r");m.put("り", "r");m.put("る", "r");m.put("れ", "r");m.put("ろ", "r");
		m.put("わ", "w");m.put("ゐ", "w");                 m.put("ゑ", "w");m.put("を", "w");
		m.put("ん", "n");m.put("っ", "t");m.put("ゔ", "v");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(getApplicationContext()));

		String dd = getFilesDir().getAbsolutePath();
		SKKUtils.dlog("dict dir: " + dd);
		SKKDictionary mainDict = new SKKDictionary(dd + "/" + DICTIONARY);
		if (!mainDict.isValid()) {
			Toast.makeText(SKKEngine.this, getString(R.string.error_dic), Toast.LENGTH_LONG).show();
			stopSelf();
		}
		SKKUserDictionary userDict = new SKKUserDictionary(dd + "/" + USER_DICT);
		if (!userDict.isValid()) {
			Toast.makeText(SKKEngine.this, getString(R.string.error_user_dic), Toast.LENGTH_LONG).show();
			stopSelf();
		}
		mDictionary = new DictionaryManager(mainDict, userDict);

		mMushroomReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					mMushroomWord = extras.getString(SKKMushroom.REPLACE_KEY);
					SKKUtils.dlog("mMushroomWord: " + mMushroomWord);
				}
				hMushroom.postDelayed(rMushroom, 250);

				//~ if (mMushroomWord != null) {
					//~ ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
					//~ cm.setText(mMushroomWord);
				//~ }
			}
		};

		IntentFilter filter = new IntentFilter(SKKMushroom.ACTION_BROADCAST);
		filter.addCategory(SKKMushroom.CATEGORY_BROADCAST);
		registerReceiver(mMushroomReceiver, filter);

		readPrefs();
	}

	private void readPrefs() {
		Context context = getApplicationContext();
		mCandidatesOpCount = SKKPrefs.getCandidatesOpCount(context);
		mStickyMeta = SKKPrefs.getStickyMeta(context);
		mSandS = SKKPrefs.getSandS(context);
		String kutouten = SKKPrefs.getKutoutenType(context);
		if (kutouten.equals("en")) {
			mZenkakuSeparatorMap.put(".", "．");
			mZenkakuSeparatorMap.put(",", "，");
		} else if (kutouten.equals("jp")) {
			mZenkakuSeparatorMap.put(".", "。");
			mZenkakuSeparatorMap.put(",", "、");
		} else if (kutouten.equals("jp_en")) {
			mZenkakuSeparatorMap.put(".", "。");
			mZenkakuSeparatorMap.put(",", "，");
		} else {
			mZenkakuSeparatorMap.put(".", "．");
			mZenkakuSeparatorMap.put(",", "，");
		}

		mUseSoftKeyboard = checkUseSoftKeyboard();

		if (mFlickJPInputView != null) {readPrefsForInputView();}
		if (mCandidateViewContainer != null) {
			int sp = SKKPrefs.getCandidatesSize(context);
			int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
			mCandidateViewContainer.setSize(px);
		}
	}

	private void readPrefsForInputView() {
		Context context = getApplicationContext();
		Configuration config = getResources().getConfiguration();
		int keyHeight = 30;
		int keyWidth = 100;
		if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
			keyHeight = SKKPrefs.getKeyHeightPort(context);
			keyWidth = SKKPrefs.getKeyWidthPort(context);
		} else if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			keyHeight = SKKPrefs.getKeyHeightLand(context);
			keyWidth = SKKPrefs.getKeyWidthLand(context);
		}

		int leftGap = 0;
		if (SKKPrefs.getKeyPosition(context).equals("center")) {
			leftGap = mScreenWidth * (100 - keyWidth) / 200;
		} else if (SKKPrefs.getKeyPosition(context).equals("right")) {
			leftGap = mScreenWidth * (100 - keyWidth) / 100;
		}
		mFlickJPInputView.prepareNewKeyboard(getApplicationContext(), keyWidth, mScreenHeight*keyHeight/(4*100), leftGap);
		mFlickJPInputView.readPrefs(context);
		mQwertyInputView.setFlickSensitivity(SKKPrefs.getFlickSensitivity(context));
		mQwertyInputView.changeKeyHeight(mScreenHeight*keyHeight/(4*100));
	}


	boolean checkUseSoftKeyboard() {
		boolean result = true;
		String use_softkey = SKKPrefs.getUseSoftKey(getApplicationContext());
		if (use_softkey.equals("on")) {
			SKKUtils.dlog("software keyboard forced");
			result = true;
		} else if (use_softkey.equals("off")) {
			SKKUtils.dlog("software keyboard disabled");
			result = false;
		} else {
			Configuration config = getResources().getConfiguration();
			if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
				result = false;
			} else if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
				result = true;
			}
		}

		if (result) {hideStatusIcon();}

		return result;
	}

	/**
	* This is the point where you can do all of your UI initialization.  It
	* is called after creation and any configuration change.
	*/
	@Override public void onInitializeInterface() {
		mUseSoftKeyboard = checkUseSoftKeyboard();
		mScreenHeight = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
		mScreenWidth = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
    }

	@Override
	public boolean onEvaluateInputViewShown() {
		return mUseSoftKeyboard;
	}

	private void createInputView() {
		Context context = getApplicationContext();
		mFlickJPInputView = new FlickJPKeyboardView(context, null);
		((FlickJPKeyboardView)mFlickJPInputView).setService(this);
		mQwertyInputView = new QwertyKeyboardView(context, null);
		((QwertyKeyboardView)mQwertyInputView).setService(this);

		readPrefsForInputView();
	}

	@Override
	public View onCreateInputView() {
		if (onEvaluateInputViewShown()) {
			createInputView();

			if (isSKKOn) {
				if (mInputMode == KATAKANA) {
					mFlickJPInputView.setKatakanaMode();
				}
				return mFlickJPInputView;
			} else {
				return mQwertyInputView;
			}
		}

		return null;
	}

	/**
	* This is the main point where we do our initialization of the
	* input method to begin operating on an application. At this
	* point we have been bound to the client, and are now receiving
	* all of the detailed information about the target of our edits.
	*/
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);

		if (mStickyMeta) {mMetaKey.clearMetaKeyState();}
		if (mSandS) {mSpacePressed = false; mSandSUsed = false;}
		mComposing.setLength(0);
		mKanji.setLength(0);
		mOkurigana = null;
		mCandidateList = null;
		// 普通にreset()すると，WebViewのときsetComposingText("", 1)で落ちるようなので回避

		if (isSKKOn) changeMode(mInputMode, false);
	}

	/**
	* Called by the framework when your view for showing candidates
	* needs to be generated, like {@link #onCreateInputView}.
	*/
	@Override
	public View onCreateCandidatesView() {
		mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(R.layout.candidates, null);
		mCandidateViewContainer.initViews();
		mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
		mCandidateView.setService(this);
		mCandidateView.setContainer(mCandidateViewContainer);

		Context context = getApplicationContext();
		int sp = SKKPrefs.getCandidatesSize(context);
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
		mCandidateViewContainer.setSize(px);

		return mCandidateViewContainer;
	}

	@Override
	public void onStartCandidatesView(EditorInfo info, boolean restarting) {
		isCandidatesViewShown = true;
	}

	@Override
	public void onFinishCandidatesView(boolean finishingInput) {
		isCandidatesViewShown = false;
		super.onFinishCandidatesView(finishingInput);
	}

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
		super.onFinishInput();

		mComposing.setLength(0);
		//~ updateCandidates();

		setCandidatesViewShown(false);
	}

	@Override
	public void onDestroy() {
		mDictionary.commitChanges();
		unregisterReceiver(mMushroomReceiver);

		super.onDestroy();
	}

	// never use fullscreen mode
	@Override
	public boolean onEvaluateFullscreenMode() {
		return false;
	}

	public void onAppPrivateCommand(String action, Bundle data) {
		if (action.equals(ACTION_COMMIT_USERDIC)) {
			mDictionary.commitChanges();
		} else if (action.equals(ACTION_READ_PREFS)) {
			readPrefs();
		} else if (action.equals(ACTION_RELOAD_USERDIC)) {
			SKKUserDictionary userDict = new SKKUserDictionary(getFilesDir().getAbsolutePath() + "/" + USER_DICT);
			if (!userDict.isValid()) {
				Toast.makeText(SKKEngine.this, getString(R.string.error_user_dic), Toast.LENGTH_LONG).show();
				stopSelf();
			}
			mDictionary.setUserDict(userDict);
		}
	}

	/**
	* Use this to monitor key events being delivered to the
	* application. We get first crack at them, and can either resume
	* them or let them continue to the app.
	*/
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!isSKKOn) return super.onKeyUp(keyCode, event);

		switch (keyCode) {
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			if (mStickyMeta) {
				mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_SPACE:
			if (mSandS) {
				mSpacePressed = false;
				if (!mSandSUsed) {processKey(' ');}
				mSandSUsed = false;
				return true;
			}
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			if (mStickyMeta) {
				mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_ENTER:
			if (isEnterUsed) {
				isEnterUsed = false;
				return true;
			}
			break;
		default:
			break;
		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	* Use this to monitor key events being delivered to the
	* application. We get first crack at them, and can either resume
	* them or let them continue to the app.
	*/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Context context = getApplicationContext();
		int kanaKey = SKKPrefs.getKanaKey(context);
		int modKanaKey = SKKPrefs.getModKanaKey(context);
		int cancelKey = SKKPrefs.getCancelKey(context);
		int modCancelKey = SKKPrefs.getModCancelKey(context);
		boolean toggleKanaKey = SKKPrefs.getToggleKanaKey(context);

		if (!isSKKOn) {
			if (keyCode == kanaKey) { // かなモードに移行するためのキー(設定可)
				int meta = event.getMetaState();
				if ((modKanaKey == 0 && meta == 0) || ((modKanaKey & meta) != 0)) {
					// 設定された修飾キーが押されている（未設定なら何も押されていない）
					toggleSKK();
					return true;
				}
			}

			return super.onKeyDown(keyCode, event);
		}

		if (keyCode == kanaKey) {
			int meta = event.getMetaState();
			if ((modKanaKey == 0 && meta == 0) || ((modKanaKey & meta) != 0)) {
				switch (mInputMode) {
				case ZENKAKU:
				case KATAKANA:
					changeMode(HIRAKANA);
					break;
				case HIRAKANA:
					if (toggleKanaKey && mRegisterStack.isEmpty()) toggleSKK();
					break;
				default:
					break;
				}
				return true;
			}
		}

		if (keyCode == cancelKey) {
			int meta = event.getMetaState();
			if ((modCancelKey == 0 && meta == 0) || ((modCancelKey & meta) != 0)) {
				if (handleCancel()) {return true;}
			}
		}

		// Process special keys
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (handleBackKey()) {return true;}
			break;
		case KeyEvent.KEYCODE_DEL:
			if (handleBackspace()) {return true;}
			break;
		case KeyEvent.KEYCODE_ENTER:
			if (handleEnter()) {return true;}
			break;
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			if (mStickyMeta) {
				mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_SPACE:
			if (mSandS) {
				mSpacePressed = true;
			} else {
				processKey(' ');
			}
			return true;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			if (mStickyMeta) {
				mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (handleLeftKey()) {return true;}
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (handleRightKey()) {return true;}
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (mStickyMeta) {mMetaKey.useMetaState();}
			if (!mRegisterStack.isEmpty() || mComposing.length() != 0 || mKanji.length() != 0) {
				return true;
			}
			break;
		default:
			// For all other keys, if we want to do transformations on
			// text being entered with a hard keyboard, we need to
			// process it and do the appropriate action.
			if (translateKeyDown(keyCode, event)) {
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	* This translates incoming hard key events in to edit operations
	* on an InputConnection.
	*/
	private boolean translateKeyDown(int keyCode, KeyEvent event) {
		int c;
		if (mStickyMeta) {
			c = event.getUnicodeChar(mMetaKey.useMetaState());
		} else {
			if (mSandS && mSpacePressed) {
				c = event.getUnicodeChar(KeyEvent.META_SHIFT_ON);
				mSandSUsed = true;
			} else {
				c = event.getUnicodeChar();
			}
		}

		InputConnection ic = getCurrentInputConnection();
		if (c == 0 || ic == null) {
			return false;
		}

		processKey(c);

		return true;
	}

	void processKey(int pcode) {
		if (mInputMode == ZENKAKU) {
			// 全角変換しcommitして終了
			pcode = SKKUtils.hankaku2zenkaku(pcode);
			commitTextSKK(String.valueOf((char) pcode), 1);
			return;
		}
		switch (mInputState) {
		case ENG2JP:
			// スペースで変換するかそのままComposingに積む
			if (pcode == ' ') {
				if (mComposing.length() != 0) {
					mKanji.setLength(0);
					mKanji.append(mComposing);
					conversionStart(mKanji);
				}
			} else {
				mComposing.append((char) pcode);
				setComposingTextSKK(mComposing, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case CHOOSE:
			// 最初の候補より戻ると変換に戻る 最後の候補より進むと登録
			switch (pcode) {
			case ' ':
				if (mChoosedIndex == mCandidateList.size() - 1) {
					registerStart(mKanji.toString());
				} else {
					chooseNextCandidate();
				}
				break;
			case '>':
				// 接尾辞入力
				pickCandidate(mChoosedIndex);
				changeState(KANJI);
				mKanji.append('>');
				setComposingTextSKK(mKanji, 1);
				break;
			case 'x':
				if (mChoosedIndex == 0) {
					if (mComposing.length() != 0) {
						// KANJIモードに戻る
						if (mOkurigana != null) {
							mOkurigana = null;
							mKanji.deleteCharAt(mKanji.length() -1);
						}
						setComposingTextSKK(mKanji, 1);
						changeState(KANJI);
					} else {
						mKanji.setLength(0);
						setComposingTextSKK(mComposing, 1);
						changeState(ENG2JP);
					}
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				} else {
					choosePreviousCandidate();
				}
				break;
			default:
				pickCandidate(mChoosedIndex);
				processKey(pcode);
				break;
			}
			break;
		case NORMAL:
		case KANJI:
		case OKURIGANA:
			// モード変更操作
			if (mInputState == NORMAL) {
				switch (pcode) {
				case 'q':
					if (mInputMode == HIRAKANA) {
						changeMode(KATAKANA);
					} else {
						changeMode(HIRAKANA);
					}
					return;
				case 'l':
					if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
						if (mRegisterStack.isEmpty()) toggleSKK();
						return;
					} // 「→」を入力するための例外
					break;
				case 'L':
					changeMode(ZENKAKU);
					return;
				case '/':
					if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
						reset();
						changeState(ENG2JP);
						if (!mRegisterStack.isEmpty()) {
							setComposingTextSKK("", 1);
						}
						return;
					} // 中黒を入力するための例外
					break;
				default:
					break;
				}
			}

			doJapaneseConversion(pcode);

			break;
		default:
			SKKUtils.dlog("Unknown mode!");
			break;
		}
	}

	// processKey()が長くて疲れるので，日本語変換関係だけ分けたもの
	private void doJapaneseConversion(int pcode) {
		String hchr = null; // かな1単位ぶん

		// シフトキーの状態をチェック
		boolean isUpper = Character.isUpperCase(pcode);
		if (isUpper) { // ローマ字変換のために小文字に戻す
			pcode = Character.toLowerCase(pcode);
		}

		switch (mInputState) {
		case OKURIGANA:
			hchr = checkSpecialConsonants(pcode);
			// 「ん」か「っ」を処理したらここで終わり
			if (hchr != null) {
				if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
				mOkurigana = hchr;
				setComposingTextSKK(createTrimmedBuilder(mKanji).append('*').append(hchr).append((char)pcode), 1);
				mComposing.setLength(0);
				mComposing.append((char)pcode);
				return;
			}
			// 送りがなが確定すれば変換，そうでなければComposingに積む
			mComposing.append((char) pcode);
			hchr = mRomajiMap.get(mComposing.toString());
			if (mOkurigana != null) { //「ん」か「っ」がある場合
				if (hchr != null) {
					if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
					mComposing.setLength(0);
					mOkurigana = mOkurigana + hchr;
					conversionStart(mKanji);
				} else {
					setComposingTextSKK(createTrimmedBuilder(mKanji).append('*').append(mOkurigana).append(mComposing), 1);
				}
			} else {
				if (hchr != null) {
					if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
					mComposing.setLength(0);
					mOkurigana = hchr;
					conversionStart(mKanji);
				} else {
					setComposingTextSKK(createTrimmedBuilder(mKanji).append('*').append(mComposing), 1);
				}
			}
			break;
		case NORMAL:
			hchr = checkSpecialConsonants(pcode);
			if (hchr != null) {
				if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
				commitTextSKK(hchr, 1);
				mComposing.setLength(0);
			}
			if (isUpper) {
				// 漢字変換候補入力の開始。KANJIへの移行
				if (mComposing.length() > 0) {
					commitTextSKK(mComposing, 1);
					mComposing.setLength(0);
				}
				changeState(KANJI);
				doJapaneseConversion(pcode);
			} else {
				mComposing.append((char) pcode);
				// 全角にする記号ならば全角，そうでなければローマ字変換
				hchr = mZenkakuSeparatorMap.get(mComposing.toString());
				if (hchr == null) {
					hchr = mRomajiMap.get(mComposing.toString());
				}

				if (hchr != null) { // 確定できるものがあれば確定
					if (mInputMode == KATAKANA) {
						hchr = SKKUtils.hirakana2katakana(hchr);
					}
					mComposing.setLength(0);
					commitTextSKK(hchr, 1);
				} else { // アルファベットならComposingに積む
					if (SKKUtils.isAlphabet(pcode)) {
						setComposingTextSKK(mComposing, 1);
					} else {
						commitTextSKK(mComposing, 1);
						mComposing.setLength(0);
					}
				}
			}
			break;
		case KANJI:
			hchr = checkSpecialConsonants(pcode);
			if (hchr != null) {
				if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
				mKanji.append(hchr);
				setComposingTextSKK(mKanji, 1);
				mComposing.setLength(0);
			}
			if (pcode == 'q') {
				// トグル変換
				if (mKanji.length() > 0) {
					String str = mKanji.toString();
					if (mInputMode == HIRAKANA) {
						str = SKKUtils.hirakana2katakana(str);
					} else {
						str = SKKUtils.katakana2hirakana(str);
					}
					commitTextSKK(str, 1);
				}
				changeState(NORMAL);
			} else if (pcode == ' ' || pcode == '>') {
				// 変換開始
				// 最後に単体の'n'で終わっている場合、'ん'に変換
				if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
					mKanji.append(mInputMode == HIRAKANA ? 'ん' : 'ン');
					setComposingTextSKK(mKanji, 1);
				}
				if (pcode == '>') {
					// 接頭辞入力
					mKanji.append('>');
				}
				mComposing.setLength(0);
				conversionStart(mKanji);
			} else if (isUpper && mKanji.length() > 0) {
				// 送り仮名開始
				// 最初の平仮名はついシフトキーを押しっぱなしにしてしまうた
				// め、mKanjiの長さをチェックmKanjiの長さが0の時はシフトが
				// 押されていなかったことにして下方へ継続させる
				mKanji.append((char) pcode); //送りありの場合子音文字追加
				mComposing.setLength(0);
				if (SKKUtils.isVowel(pcode)) { // 母音なら送り仮名決定，変換
					mOkurigana  = mRomajiMap.get(String.valueOf((char) pcode));
					if (mInputMode == KATAKANA) {mOkurigana = SKKUtils.hirakana2katakana(mOkurigana);}
					conversionStart(mKanji);
				} else { // それ以外は送り仮名モード
					mComposing.append((char) pcode);
					setComposingTextSKK(createTrimmedBuilder(mKanji).append('*').append((char)pcode), 1);
					changeState(OKURIGANA);
				}
			} else {
				// 未確定
				mComposing.append((char) pcode);
				hchr = mZenkakuSeparatorMap.get(mComposing.toString());
				if (hchr == null) {
					hchr = mRomajiMap.get(mComposing.toString());
				}

				if (hchr != null) {
					if (mInputMode == KATAKANA) {hchr = SKKUtils.hirakana2katakana(hchr);}
					mComposing.setLength(0);
					mKanji.append(hchr);
					setComposingTextSKK(mKanji, 1);
				} else {
					setComposingTextSKK(mKanji.toString() + mComposing.toString(), 1);
				}
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		default:
			SKKUtils.dlog("Unknown mode!");
			break;
		}
	}

	private StringBuilder createTrimmedBuilder(StringBuilder orig) {
		StringBuilder ret = new StringBuilder(orig);
		ret.deleteCharAt(ret.length()-1);
		return ret;
	}

	void processText(String text, boolean isShifted) {
		if (mInputMode == ZENKAKU) {
			commitTextSKK(text, 1);
			return;
		}
		switch (mInputState) {
			case ENG2JP:
				mComposing.append(text);
				setComposingTextSKK(mComposing, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
				break;
			case CHOOSE:
				pickCandidate(mChoosedIndex);
				processText(text, isShifted);
				break;
			case OKURIGANA:
				if (mInputMode == KATAKANA) {text = SKKUtils.hirakana2katakana(text);}
				if (mComposing.length() > 0) {
					String hchr = mComposing.toString();
					if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
						hchr = mInputMode == HIRAKANA ? "ん" : "ン";
					}
					mComposing.setLength(0);
					mOkurigana = hchr + text;
				} else {
					mOkurigana = text;
				}
				conversionStart(mKanji);
				break;
			case NORMAL:
				if (mComposing.length() > 0) {
					String hchr = mComposing.toString();
					if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
						hchr = mInputMode == HIRAKANA ? "ん" : "ン";
					}
					commitTextSKK(hchr, 1);
					mComposing.setLength(0);
				}
				if (isShifted) {
					// 漢字変換候補入力の開始。KANJIへの移行
					changeState(KANJI);
					processText(text, false);
				} else {
					if (mInputMode == KATAKANA) {
						text = SKKUtils.hirakana2katakana(text);
					}
					commitTextSKK(text, 1);
				}
				break;
			case KANJI:
				if (mComposing.length() > 0) {
					if (mComposing.length() == 1 && mComposing.charAt(0) == 'n') {
						mKanji.append(mInputMode == HIRAKANA ? "ん" : "ン");
					} else {
						mKanji.append(mComposing);
					}
					setComposingTextSKK(mKanji, 1);
					mComposing.setLength(0);
				}
				if (isShifted && mKanji.length() > 0) {
					// 送り仮名
					String okuri_consonant = mConsonantMap.get(text.substring(0, 1));
					if (okuri_consonant != null) {
						mKanji.append(okuri_consonant);
					}
					if (mInputMode == KATAKANA) {text = SKKUtils.hirakana2katakana(text);}
					mOkurigana = text;
					conversionStart(mKanji);
				} else {
					// 未確定
					if (mInputMode == KATAKANA) {text = SKKUtils.hirakana2katakana(text);}
					mKanji.append(text);
					setComposingTextSKK(mKanji, 1);
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				}
				break;
			default:
				SKKUtils.dlog("Unknown mode!");
				break;
		}
	}

	// commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
	private void commitTextSKK(CharSequence text, int newCursorPosition) {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null) return;

		if (!mRegisterStack.isEmpty()) {
			mRegisterStack.peek().mEntry.append(text);
			setComposingTextSKK("", newCursorPosition);
		} else {
			ic.commitText(text, newCursorPosition);
		}
	}

	void sendText(CharSequence text) {
		getCurrentInputConnection().commitText(text, 1);
	}

	//setComposingTextのラッパー
	private void setComposingTextSKK(CharSequence text, int newCursorPosition) {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null) return;

		SpannableStringBuilder ct = new SpannableStringBuilder();
		BackgroundColorSpan bg = null;
		int bgStart = 0;

		if (!mRegisterStack.isEmpty()) {
			for (RegisterInfo regInfo : mRegisterStack) {
				bgStart = ct.length();
				ct.append("▼");
				if (regInfo.mOkurigana == null) {
					ct.append(regInfo.mKey);
				} else {
					ct.append(regInfo.mKey.substring(0, regInfo.mKey.length()-1));
					ct.append("*");
					ct.append(regInfo.mOkurigana);
				}
				ct.append("：");
				ct.setSpan(new BackgroundColorSpan(Color.argb(64, 255, 96, 0)), bgStart, ct.length(), Spanned.SPAN_COMPOSING);
				ct.append(regInfo.mEntry);
			}
		}

		if (mInputState != NORMAL) {
			switch (mInputState) {
			case KANJI:
			case ENG2JP:
			case OKURIGANA:
				bg = new BackgroundColorSpan(Color.argb(64, 0, 96, 255));
				bgStart = ct.length();
				ct.append("▽");
				break;
			case CHOOSE:
				bg = new BackgroundColorSpan(Color.argb(64, 255, 96, 0));
				bgStart = ct.length();
				ct.append("▼");
				break;
			default:
				break;
			}
		}
		ct.append(text);

		if (bg != null) {
			ct.setSpan(bg, bgStart, ct.length(), Spanned.SPAN_COMPOSING);
		}
		ct.setSpan(new UnderlineSpan(), 0, ct.length(), Spanned.SPAN_COMPOSING);

		ic.setComposingText(ct, newCursorPosition);
	}

	// 変換スタート
	// composingに辞書のキー 送りありの場合最後はアルファベット
	// 送りありの場合は送りがな自体をmOkuriganaに入れておく
	private void conversionStart(StringBuilder composing) {
		String str = composing.toString();

		changeState(CHOOSE);

		List<String> list = mDictionary.findKanji(str, mOkurigana);
		if (list == null) {
			registerStart(str);
			return;
		}

		mChoosedIndex = 0;

		if (mOkurigana != null) {
			setComposingTextSKK(SKKUtils.removeAnnotation(list.get(0)).concat(mOkurigana), 1);
		} else {
			setComposingTextSKK(SKKUtils.removeAnnotation(list.get(0)), 1);
		}

		mCandidateList = list;
		mOpCount = 1;
		if (mCandidatesOpCount == 1 || mUseSoftKeyboard) {
			setCandidatesToCandidateView();
		}
	}

	private void reConversionStart() {
		if (mLastConversion == null) { return; }

		mDictionary.rollBack();

		mComposing.setLength(0);
		mKanji.setLength(0);
		mKanji.append(mLastConversion.kanjiKey);
		mOkurigana = mLastConversion.okuri;
		mCandidateList = mLastConversion.list;
		mChoosedIndex = mLastConversion.index;

		changeState(CHOOSE);

		if (mOkurigana != null) {
			setComposingTextSKK(SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex)).concat(mOkurigana), 1);
		} else {
			setComposingTextSKK(SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex)), 1);
		}

		mOpCount = mLastConversion.opCount;

		chooseNextCandidate();
	}

	private void registerStart(String str) {
		mRegisterStack.push(new RegisterInfo(str, mOkurigana));
		changeState(NORMAL);
		//setComposingTextSKK("", 1);
	}

	// mComposingと合わせて"ん"・"っ"になるか判定
	// ならなかったらnull
	private String checkSpecialConsonants(int pcode) {
		if (mComposing.length() != 1 || mOkurigana != null) return null;

		char first = mComposing.charAt(0);
		if (first == 'n') {
			if (!SKKUtils.isVowel(pcode) && pcode != 'n' && pcode != 'y') {
				return "ん";
			}
		} else if (first == pcode) {
			return "っ";
		}

		return null;
	}

	private boolean handleBackKey() {
		switch (mInputState) {
		case NORMAL:
			if (!mRegisterStack.isEmpty()) {
				mRegisterStack.pop();
				reset();
				if (!mRegisterStack.isEmpty()) {
					setComposingTextSKK("", 1);
				}

				return true;
			}
			break;
		case KANJI:
		case CHOOSE:
		case ENG2JP:
		case OKURIGANA:
			if (!mRegisterStack.isEmpty()) {
				mRegisterStack.pop();
			}
			changeState(NORMAL);

			return true;
		}

		return false;
	}

	boolean handleLeftKey() {
		// Shift・Altキーの状態を消費
		if (mStickyMeta) {mMetaKey.useMetaState();}
		if (mInputState == CHOOSE) {
			choosePreviousCandidate();
		} else if (mRegisterStack.isEmpty() && mComposing.length() == 0 && mKanji.length() == 0) {
			return false;
		}

		return true;
	}

	boolean handleRightKey() {
		if (mStickyMeta) {mMetaKey.useMetaState();}
		if (mInputState == CHOOSE) {
			chooseNextCandidate();
		} else if (mRegisterStack.isEmpty() && mComposing.length() == 0 && mKanji.length() == 0) {
			return false;
		}

		return true;
	}

	boolean handleEnter() {
		// Shift・Altキーの状態を消費
		if (mStickyMeta) {mMetaKey.useMetaState();}

		switch (mInputState) {
		case CHOOSE:
			pickCandidate(mChoosedIndex);
			break;
		case ENG2JP:
			if (mComposing.length() > 0) {
				commitTextSKK(mComposing, 1);
				mComposing.setLength(0);
			}
			changeState(NORMAL);
			break;
		case KANJI:
		case OKURIGANA:
			commitTextSKK(mKanji, 1);
			mComposing.setLength(0);
			mKanji.setLength(0);
			changeState(NORMAL);
			break;
		default:
			if (mComposing.length() == 0) {
				if (!mRegisterStack.isEmpty()) {
					// 単語登録終了
					RegisterInfo regInfo = mRegisterStack.pop();
					if (regInfo.mEntry.length() != 0) {
						mDictionary.addEntry(regInfo.mKey, regInfo.mEntry.toString(), regInfo.mOkurigana);
						mDictionary.commitChanges();
						String entry = regInfo.mEntry.toString();
						if (mInputMode == KATAKANA) {entry = SKKUtils.hirakana2katakana(entry);}
						commitTextSKK(entry, 1);
						if (regInfo.mOkurigana != null) {
							commitTextSKK(regInfo.mOkurigana, 1);
						}
					}
					reset();
					if (!mRegisterStack.isEmpty()) {
						setComposingTextSKK("", 1);
					}
				} else {
					return false;
				}
			} else {
				commitTextSKK(mComposing, 1);
				mComposing.setLength(0);
			}
		}

		isEnterUsed = true;
		return true;
	}

	boolean handleBackspace() {
		// Shift・Altキーの状態を消費
		if (mStickyMeta) {mMetaKey.useMetaState();}

		int clen = mComposing.length();
		int klen = mKanji.length();

		if (clen == 0 && klen == 0 || mInputMode == ZENKAKU) {
			if (!mRegisterStack.isEmpty()) {
				StringBuilder regEntry = mRegisterStack.peek().mEntry;
				if (regEntry.length() > 0) {
					regEntry.deleteCharAt(regEntry.length()-1);
					setComposingTextSKK("", 1);
				} else if (mUseSoftKeyboard) {
					return handleCancel();
				}
			} else if (mInputState == ENG2JP) {
				changeState(NORMAL);
			} else {
				return false;
			}

			return true;
		}

		if (mUseSoftKeyboard && mInputState == CHOOSE) {
			return handleCancel();
		}

		if (clen > 0) {
			mComposing.deleteCharAt(clen-1);
		} else if (klen > 0) {
			mKanji.deleteCharAt(klen-1);
		}
		clen = mComposing.length();
		klen = mKanji.length();

		switch (mInputState) {
		case NORMAL:
			setComposingTextSKK(mComposing, 1);
			break;
		case KANJI:
			if (klen == 0 && clen == 0) {
				changeState(NORMAL);
			} else {
				setComposingTextSKK(mKanji.toString() + mComposing.toString(), 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case ENG2JP:
			if (clen == 0) {
				changeState(NORMAL);
			} else {
				setComposingTextSKK(mComposing, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			break;
		case OKURIGANA:
			mComposing.setLength(0);
			mOkurigana = null;
			setComposingTextSKK(mKanji, 1);
			changeState(KANJI);
			break;
		case CHOOSE:
			if (klen == 0) {
				changeState(NORMAL);
			} else {
				if (clen > 0) { // 英語変換中
					changeState(ENG2JP);
					setComposingTextSKK(mComposing, 1);
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				} else { // 漢字変換中
					if (mOkurigana != null) {
						mOkurigana = null;
					}
					changeState(KANJI);
					setComposingTextSKK(mKanji, 1);
					if (mUseSoftKeyboard) {
						updateSuggestions();
					}
				}
			}
			break;
		default:
			SKKUtils.dlog("handleBackspace() do nothing");
			break;
		}

		return true;
	}

	boolean handleCancel() {
		switch (mInputState) {
		case NORMAL:
			if (!mRegisterStack.isEmpty()) {
				RegisterInfo regInfo = mRegisterStack.pop();
				mKanji.setLength(0);
				mKanji.append(regInfo.mKey);
				if (regInfo.mOkurigana != null) {
					mKanji.deleteCharAt(mKanji.length() - 1);
				}
				mComposing.setLength(0);
				changeState(KANJI);
				setComposingTextSKK(mKanji, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			} else {
				if (mLastConversion == null) { return false; }
				String s = mLastConversion.candidate;
				SKKUtils.dlog("last conversion: " + s);
				InputConnection ic = getCurrentInputConnection();
				if (ic != null && s.equals(ic.getTextBeforeCursor(s.length(), 0))) {
					ic.deleteSurroundingText(s.length(), 0);
					reConversionStart();
				}
			}
			break;
		case KANJI:
		case ENG2JP:
			mComposing.setLength(0);
			mKanji.setLength(0);
			changeState(NORMAL);
			break;
		case CHOOSE:
			if (mComposing.length() > 0) { // 英語変換中
				changeState(ENG2JP);
				setComposingTextSKK(mComposing, 1);
			} else { // 漢字変換中
				if (mOkurigana != null) {
					mOkurigana = null;
					mKanji.deleteCharAt(mKanji.length() -1);
				}
				changeState(KANJI);
				setComposingTextSKK(mKanji, 1);
			}
			if (mUseSoftKeyboard) {
				updateSuggestions();
			}
			break;
		case OKURIGANA:
			mComposing.setLength(0);
			mOkurigana = null;
			mKanji.deleteCharAt(mKanji.length()-1);
			changeState(KANJI);
			setComposingTextSKK(mKanji, 1);
			break;
		default:
			return false;
		}

		return true;
	}

	void sendToMushroom() {
		String str;
		if (mInputState == KANJI || mInputState == ENG2JP) {
			str = mKanji.toString();
		} else {
			ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			CharSequence cs = cm.getText();
			if (cs == null) {
				str = "";
			} else {
				str = cs.toString();
			}
		}

		if (mInputState == NORMAL) {
			mRegisterStack.clear();
			reset();
		} else {
			mRegisterStack.clear();
			changeState(NORMAL);
		}

		try {
			Intent mushroom = new Intent(this, SKKMushroom.class);
			mushroom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mushroom.putExtra(SKKMushroom.REPLACE_KEY, str);
			startActivity(mushroom);
		} catch (ActivityNotFoundException e) {
		}
	}

	void showInputMethodPicker() {
		((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
	}

	void showMenuDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.ime_name));
		builder.setItems(new CharSequence[] {"入力方法", "設定", "マッシュルーム"},
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
							case 0:
								showInputMethodPicker();
								break;
							case 1:
								Intent intent = new Intent(SKKEngine.this, SKKPrefs.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
										Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
								break;
							case 2:
								sendToMushroom();
								break;
						}
					}
				});
		builder.setCancelable(true);
		AlertDialog dialog = builder.create();
		Window window = dialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		if (isSKKOn) {
			lp.token = mFlickJPInputView.getWindowToken();
		} else {
			lp.token = mQwertyInputView.getWindowToken();
		}
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		dialog.show();
	}

	// 小文字大文字変換，濁音，半濁音に使う
	void changeLastChar(Map<String, String> map) {
		if (mInputState == KANJI && mComposing.length() == 0) {
			String s = mKanji.toString();
			int idx = s.length() - 1;
			String lastchar = s.substring(idx);
			String new_lastchar = map.get(lastchar);

			if (new_lastchar != null) {
				mKanji.deleteCharAt(idx);
				mKanji.append(new_lastchar);
				setComposingTextSKK(mKanji, 1);
				if (mUseSoftKeyboard) {
					updateSuggestions();
				}
			}
			return;
		}

		if (mInputState == CHOOSE) {
			if (mOkurigana == null) return;
			String new_okuri = map.get(mOkurigana);

			if ((mOkurigana.equals("つ") && new_okuri.equals("っ")) || (mOkurigana.equals("っ") && new_okuri.equals("つ"))) {
				// 変換やりなおしをしなくてもいい例外
				// 送りがなが「っ」になる場合は，どのみち必ずt段の音なので
				mOkurigana = new_okuri;
				setComposingTextSKK(SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex)).concat(mOkurigana), 1);
			} else if (new_okuri != null) {
				String new_okuri_consonant = mConsonantMap.get(new_okuri);
				mKanji.deleteCharAt(mKanji.length() - 1);
				mKanji.append(new_okuri_consonant);
				mOkurigana = new_okuri;
				conversionStart(mKanji); //変換やりなおし
			}
			return;
		}

		if (mComposing.length() == 0 && mKanji.length() == 0) {
			if (!mRegisterStack.isEmpty() && mRegisterStack.peek().mEntry.length() == 0) {
				RegisterInfo regInfo = mRegisterStack.peek();
				if (regInfo.mOkurigana == null) return;
				String new_okuri = map.get(regInfo.mOkurigana);

				if (new_okuri != null) {
					String new_okuri_consonant = mConsonantMap.get(new_okuri);
					mRegisterStack.pop();
					mKanji.setLength(0);
					mKanji.append(regInfo.mKey.substring(0, regInfo.mKey.length() - 1));
					mKanji.append(new_okuri_consonant);
					mOkurigana = new_okuri;
					conversionStart(mKanji); //変換やりなおし
				}
				return;
			}

			String lastchar = null;
			String new_lastchar = null;

			InputConnection ic = getCurrentInputConnection();
			if (ic != null) {
				CharSequence cs = ic.getTextBeforeCursor(1, 0);
				if (cs != null) {
					lastchar = cs.toString();
					new_lastchar = map.get(lastchar);

					if (new_lastchar != null) {
						if (!mRegisterStack.isEmpty()) {
							StringBuilder regEntry = mRegisterStack.peek().mEntry;
							regEntry.deleteCharAt(regEntry.length()-1);
							regEntry.append(new_lastchar);
							setComposingTextSKK("", 1);
						} else {
							ic.deleteSurroundingText(1, 0);
							ic.commitText(new_lastchar, 1);
						}
					}
				}
			}
		}
	}

	private void updateSuggestions() {
		mChoosedIndex = 0;

		String str = null;
		switch (mInputState) {
		case ENG2JP:
			str = mComposing.toString();
			break;
		case KANJI:
			str = mKanji.toString();
			break;
		default:
			SKKUtils.dlog("updateSuggestions(): " + mInputState);
			setSuggestions(null);
			return;
		}

		if (str.length() == 0) {setSuggestions(null); return;}

		setSuggestions(mDictionary.findKeys(str));
	}

	private void setSuggestions(List<String> suggestions) {
		setCandidatesViewShown(true);

		if (mCandidateView != null) {
			mCandidateView.setSuggestions(suggestions);
		}
	}

	private void choosePreviousCandidate() {
		if (mCandidateList == null) return;

		mChoosedIndex--;
		if (mChoosedIndex < 0) {
			mChoosedIndex = mCandidateList.size() - 1;
		}

		chooseCandidate();
	}

	private void chooseNextCandidate() {
		if (mCandidateList == null) return;

		mChoosedIndex++;
		if (mChoosedIndex >= mCandidateList.size()) {
			mChoosedIndex = 0;
		}

		chooseCandidate();
	}

	private void chooseCandidate() {
		mOpCount++;
		if (mCandidatesOpCount != 0 && !isCandidatesViewShown && mOpCount >= mCandidatesOpCount) {
			setCandidatesToCandidateView();
		}
		if (mCandidateView != null) {
			mCandidateView.choose(mChoosedIndex);
		}

		String cad = SKKUtils.removeAnnotation(mCandidateList.get(mChoosedIndex));
		if (mInputMode == KATAKANA) {cad = SKKUtils.hirakana2katakana(cad);}
		if (mOkurigana != null) {
			cad = cad.concat(mOkurigana);
		}
		setComposingTextSKK(cad, 1);
	}

	private void setCandidatesToCandidateView() {
		if (mCandidateList != null && mCandidateList.size() > 0) {
			setCandidatesViewShown(true);
		}
		if (mCandidateView != null) {
			mCandidateView.setSuggestions(mCandidateList);
		}
	}

	private void pickCandidate(int index) {
		if (mInputState != CHOOSE) {
			return;
		}

		if (mCandidateList.size() > 0) {
			String s = mCandidateList.get(index);
			String s_noAnnotation = SKKUtils.removeAnnotation(s);
			if (mInputMode == KATAKANA) {s_noAnnotation = SKKUtils.hirakana2katakana(s_noAnnotation);}

			commitTextSKK(s_noAnnotation, 1);
			if (mOkurigana != null) commitTextSKK(mOkurigana, 1);
			mDictionary.addEntry(mKanji.toString(), s, mOkurigana);

			if (mRegisterStack.isEmpty()) {
				if (mOkurigana != null) {
					mLastConversion = new ConversionInfo(s_noAnnotation + mOkurigana, mCandidateList, index, mOpCount, mKanji.toString(), mOkurigana);
				} else {
					mLastConversion = new ConversionInfo(s_noAnnotation, mCandidateList, index, mOpCount, mKanji.toString(), null);
				}
			}

			changeState(NORMAL);
		}
	}

	public void pickCandidateViewManually(int index) {
		if (mInputState == CHOOSE) {
			pickCandidate(index);
			return;
		}

		String s = mCandidateView.get(index);
		if (!s.equals("")) {
			if (mInputState == ENG2JP) {
				setComposingTextSKK(s, 1);
				mComposing.setLength(0);
				mComposing.append(s);
				conversionStart(mComposing);
			} else if (mInputState == KANJI) {
				if (mInputMode == KATAKANA) {s = SKKUtils.hirakana2katakana(s);}
				setComposingTextSKK(s, 1);
				int li = s.length() - 1;
				int last = s.codePointAt(li);
				if (SKKUtils.isAlphabet(last)) {
					mKanji.setLength(0);
					mKanji.append(s.substring(0, li));
					mComposing.setLength(0);
					processKey(Character.toUpperCase(last));
				} else {
					mKanji.setLength(0);
					mKanji.append(s);
					mComposing.setLength(0);
					conversionStart(mKanji);
				}
			}
		}
	}

	/**
	* Helper to send a key down / key up pair to the current editor.
	*/
	void keyDownUp(int keyEventCode) {
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
			ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
		}
	}

	void pressEnter() {
		InputConnection ic = getCurrentInputConnection();
		if (ic == null) { return; }
		EditorInfo editorInfo = getCurrentInputEditorInfo();

		switch (editorInfo.imeOptions
			& (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
			case EditorInfo.IME_ACTION_DONE:
				ic.performEditorAction(EditorInfo.IME_ACTION_DONE);
				break;
			case EditorInfo.IME_ACTION_GO:
				ic.performEditorAction(EditorInfo.IME_ACTION_GO);
				break;
			case EditorInfo.IME_ACTION_NEXT:
				ic.performEditorAction(EditorInfo.IME_ACTION_NEXT);
				break;
			case EditorInfo.IME_ACTION_SEARCH:
				ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH);
				break;
			case EditorInfo.IME_ACTION_SEND:
				ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
				break;
			default:
				keyDownUp(KeyEvent.KEYCODE_ENTER);
				break;
		}
	}

	private void reset() {
		mComposing.setLength(0);
		mKanji.setLength(0);
		mOkurigana = null;
		mCandidateList = null;

		//setCandidatesViewShown()ではComposingTextがflushされるので消す
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			ic.setComposingText("", 1);
		}
		setCandidatesViewShown(false);
//		mMetaKey.clearMetaKeyState();
	}

	void toggleSKK() {
		if (isSKKOn) {
			if (mInputState == CHOOSE) {
				pickCandidate(mChoosedIndex);
			}
			isSKKOn = false;
			hideStatusIcon();
			mRegisterStack.clear();
			reset();
			if (mUseSoftKeyboard) {
				setInputView(mQwertyInputView);
			}
		} else {
			isSKKOn = true;
			if (mUseSoftKeyboard) {
				setInputView(mFlickJPInputView);
			}
			changeState(NORMAL);
		}
		if (mStickyMeta) {mMetaKey.clearMetaKeyState();}
	}

	// change the mode and set the status icon
	private void changeMode(InputMode mode) {
		changeMode(mode, true);
	}

	private void changeMode(InputMode mode, boolean doReset) {
		if (!isSKKOn) return;

		int icon = 0;

		if (doReset) reset();

		switch (mode) {
			case HIRAKANA:
				mInputMode = HIRAKANA;
				icon = R.drawable.immodeic_hiragana;
				if (mUseSoftKeyboard) {
					if (mFlickJPInputView != null) {
						mFlickJPInputView.setHiraganaMode();
					}
				}
				break;
			case KATAKANA:
				mInputMode = KATAKANA;
				icon = R.drawable.immodeic_katakana;
				if (mUseSoftKeyboard) {
					if (mFlickJPInputView != null) {
						mFlickJPInputView.setKatakanaMode();
					}
				}
				break;
			case ZENKAKU:
				mInputMode = ZENKAKU;
				changeState(NORMAL, doReset);
				icon = R.drawable.immodeic_full_alphabet;
				break;
//			case ENG2JP:
//				mInputMode = ENG2JP;
//				icon = R.drawable.immodeic_eng2jp;
//				setComposingTextSKK("", 1);
//				break;
			default:
				break;
		}

		if (mUseSoftKeyboard) {
			hideStatusIcon();
		} else if (icon != 0) {
			showStatusIcon(icon);
		}
		if (!mRegisterStack.isEmpty() && doReset) {
			setComposingTextSKK("", 1);
		}
		// ComposingTextのflush回避のためreset()で一旦消してるので，登録中はここまで来てからComposingText復活
	}

	private void changeState(InputState state) {
		changeState(state, state == NORMAL);
	}

	private void changeState(InputState state, boolean doReset) {
		if (doReset) reset();
		mInputState = state;
		if (!mRegisterStack.isEmpty() && doReset || state == ENG2JP) {
			setComposingTextSKK("", 1);
		}
		// ComposingTextのflush回避のためreset()で一旦消してるので，登録中はここまで来てからComposingText復活
	}
}
