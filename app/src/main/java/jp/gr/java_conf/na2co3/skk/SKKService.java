package jp.gr.java_conf.na2co3.skk;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.InputType;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import jp.gr.java_conf.na2co3.dialog.ListMenuServiceDialog;
import jp.gr.java_conf.na2co3.skk.engine.*;

public class SKKService extends InputMethodService {
    private CandidateViewContainer mCandidateViewContainer;
    private CandidateView mCandidateView;
    private FlickJPKeyboardView mFlickJPInputView = null;
    private QwertyKeyboardView mQwertyInputView = null;
    private AbbrevKeyboardView mAbbrevKeyboardView = null;
    private SKKKeyboardView mCurrentInputView = null;
    private int mScreenHeight;

    private SKKEngine mEngine;

    private DictionaryProcessor mDictionary;

    // onKeyDown()でEnterキーのイベントを消費したかどうかのフラグ．onKeyUp()で判定するのに使う
    private boolean isEnterUsed = false;

    private boolean isCandidatesViewShown = false;

    private SKKMetaKey mMetaKey = new SKKMetaKey(this);
    private boolean mStickyMeta = false;
    private boolean mSandS = false;
    private boolean mSpacePressed = false;
    private boolean mSandSUsed = false;

    private boolean mUseSoftKeyboard = false;

    private boolean mHasComposingText = false;

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

    static final String ACTION_COMMIT_USERDIC = "jp.gr.java_conf.na2co3.ACTION_COMMIT_USERDIC";
    static final String ACTION_READ_PREFS = "jp.gr.java_conf.na2co3.ACTION_READ_PREFS";
    static final String ACTION_RELOAD_DICS = "jp.gr.java_conf.na2co3.ACTION_RELOAD_DICS";

    private List<SKKDictionary> openDictionaries() {
        List<SKKDictionary> result = new ArrayList<>();
        String dd = getFilesDir().getAbsolutePath();
        SKKUtils.dlog("dict dir: " + dd);

        result.add(new SKKDictionary(dd + "/" + getString(R.string.dic_name_main), getString(R.string.btree_name)));
        if (!result.get(0).isValid()) {
            Toast.makeText(SKKService.this, getString(R.string.error_dic), Toast.LENGTH_LONG).show();
            stopSelf();
        }

        String val = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefkey_optional_dics), "");
        if (val.length() > 0) {
            String[] vals = val.split("/");
            for (int i=1; i<vals.length; i=i+2) {
                result.add(new SKKDictionary(dd + "/" + vals[i], getString(R.string.btree_name)));
                int last = result.size()-1;
                if (!result.get(last).isValid()) { result.remove(last); }
            }
        }

        return result;
    }

    private SKKUserDictionary openUserDictionary() {
        String dd = getFilesDir().getAbsolutePath();
        SKKUserDictionary dic = new SKKUserDictionary(dd + "/" + getString(R.string.dic_name_user), getString(R.string.btree_name));
        if (!dic.isValid()) {
            Toast.makeText(SKKService.this, getString(R.string.error_user_dic), Toast.LENGTH_LONG).show();
            stopSelf();
        }

        return dic;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(getApplicationContext()));

        mDictionary = new DictionaryProcessor(openDictionaries(), openUserDictionary());
        mEngine = new SKKEngine(SKKService.this, mDictionary);

        mMushroomReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    mMushroomWord = extras.getString(SKKMushroom.REPLACE_KEY);
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
        mStickyMeta = SKKPrefs.getStickyMeta(context);
        mSandS = SKKPrefs.getSandS(context);
        mEngine.setZenkakuPunctuationMarks(SKKPrefs.getKutoutenType(context));
        mEngine.setDisplayState(SKKPrefs.getDisplayState(context));

        mUseSoftKeyboard = checkUseSoftKeyboard();
        updateInputViewShown();

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

        mFlickJPInputView.prepareNewKeyboard(getApplicationContext(), keyWidth, mScreenHeight*keyHeight/(4*100), SKKPrefs.getKeyPosition(context));
        mQwertyInputView.setFlickSensitivity(SKKPrefs.getFlickSensitivity(context));
        mQwertyInputView.changeKeyHeight(mScreenHeight*keyHeight/(4*100));
        mAbbrevKeyboardView.changeKeyHeight(mScreenHeight*keyHeight/(4*100));
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

    // 画面回転等の時にIMEの表示画面が再生成されるのでInputViewを破棄する
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mFlickJPInputView = null;
        mQwertyInputView = null;
        mAbbrevKeyboardView = null;
        mCurrentInputView = null;
        super.onConfigurationChanged(newConfig);
    }

    /**
    * This is the point where you can do all of your UI initialization.  It
    * is called after creation and any configuration change.
    */
    @Override public void onInitializeInterface() {
        mUseSoftKeyboard = checkUseSoftKeyboard();
        updateInputViewShown();
        mScreenHeight = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight();
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public boolean onEvaluateInputViewShown() {
        return mUseSoftKeyboard;
    }

    private void createInputView() {
        Context context = getApplicationContext();
        mFlickJPInputView = new FlickJPKeyboardView(context, null);
        mFlickJPInputView.setService(this);

        mQwertyInputView = getLayoutInflater().inflate(R.layout.qwerty, null).findViewById(R.id.keyboard_view);
        mQwertyInputView.setService(this);

        mAbbrevKeyboardView = getLayoutInflater().inflate(R.layout.abbrev, null).findViewById(R.id.keyboard_view);
        mAbbrevKeyboardView.setService(this);

        readPrefsForInputView();
    }

    @Override
    public View onCreateInputView() {
        createInputView();

        SKKKeyboardView inputView = getInputViewByType(mEngine.getCurrentKeyboardType());
        mCurrentInputView = inputView;
        return inputView;
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
        mHasComposingText = false;

        mEngine.resetOnStartInput();
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                mEngine.toASCIIMode();
                break;
            case InputType.TYPE_CLASS_TEXT:
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_URI
                    || variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                    mEngine.toASCIIMode();
                }
                break;
        }
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
            SKKUtils.dlog("commit user dictionary!");
            mDictionary.commitChanges();
        } else if (action.equals(ACTION_READ_PREFS)) {
            readPrefs();
        } else if (action.equals(ACTION_RELOAD_DICS)) {
            mDictionary.reopenDictionaries(openDictionaries());
        }
    }

    /**
    * Use this to monitor key events being delivered to the
    * application. We get first crack at them, and can either resume
    * them or let them continue to the app.
    */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mEngine.ignoresKeyEvent()) { return super.onKeyUp(keyCode, event); }

        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if (mStickyMeta) {
                    mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                if (mStickyMeta) {
                    mMetaKey.releaseMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
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

    // 設定された修飾キーが押されている（未設定なら何も押されていない）
    private boolean checkMetaState(int meta, int state) {
        if (meta == 0 && state == 0) { return true; }
        if (meta == KeyEvent.META_ALT_ON && mStickyMeta) {
            if ((mMetaKey.useMetaState() & KeyEvent.META_ALT_ON) != 0) { return true; }
        }

        return ((state & meta) != 0);
    }

    /**
    * Use this to monitor key events being delivered to the
    * application. We get first crack at them, and can either resume
    * them or let them continue to the app.
    */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Context context = getApplicationContext();
        int metaState = event.getMetaState();
        SKKState engineState = mEngine.getState();

        // Process special keys
        if (keyCode == SKKPrefs.getKanaKey(context)
                && checkMetaState(SKKPrefs.getModKanaKey(context), metaState)) {
            mEngine.handleKanaKey();
            return true;
        }

        if (mEngine.ignoresKeyEvent()) {
            return super.onKeyDown(keyCode, event);
        }

        if (keyCode == SKKPrefs.getCancelKey(context)
                && checkMetaState(SKKPrefs.getModCancelKey(context), metaState)) {
            if (handleCancel()) {return true;}
        }

        if (keyCode == 'q'
                && checkMetaState(SKKPrefs.getModCancelKey(context), metaState)) {
            processKey(-1010);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_TAB) {
            if (engineState.isTransient() && !engineState.isConverting()) {
                boolean isShifted = false;
                if (mStickyMeta) {
                    if ((mMetaKey.useMetaState() & KeyEvent.META_SHIFT_ON) != 0) {
                        isShifted = true;
                    }
                } else if (mSandS) {
                    if (mSpacePressed) {
                        isShifted = true;
                        mSandSUsed = true;
                    }
                } else {
                    if ((event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0) { isShifted = true; }
                }
                mEngine.chooseAdjacentSuggestion(!isShifted);
                return true;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if (mStickyMeta) {
                    mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.SHIFT_KEY);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                if (mStickyMeta) {
                    mMetaKey.pressMetaKey(SKKMetaKey.MetaKey.ALT_KEY);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (mEngine.handleBackKey()) {return true;}
                break;
            case KeyEvent.KEYCODE_DEL:
                if (handleBackspace(false)) {return true;}
                break;
            case KeyEvent.KEYCODE_ENTER:
                if (handleEnter()) {return true;}
                break;
            case KeyEvent.KEYCODE_SPACE:
                if (mSandS) {
                    mSpacePressed = true;
                } else {
                    processKey(' ');
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (handleDpad(keyCode)) { return true; }
                break;
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to
                // process it and do the appropriate action.
                if (translateKeyDown(event)) {
                    return true;
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
    * This translates incoming hard key events in to edit operations
    * on an InputConnection.
    */
    private boolean translateKeyDown(KeyEvent event) {
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

    void processKey(int pcode) { mEngine.processKey(pcode); }
    void processText(String text, boolean isShifted) { mEngine.processText(text, isShifted); }
    void handleKanaKey() { mEngine.handleKanaKey(); }
    boolean handleCancel() { return mEngine.handleCancel(); }
    void toASCIIMode() { mEngine.toASCIIMode(); }
    void toAbbrevState() { mEngine.toAbbrevState(); }
    void toggleKana() { mEngine.toggleKana(); }

    boolean handleBackspace(boolean softKeyboad) {
        if (mStickyMeta) {mMetaKey.useMetaState();}
        return mEngine.handleBackspace(softKeyboad);
    }

    boolean handleEnter() {
        if (mStickyMeta) {mMetaKey.useMetaState();}

        if (mEngine.handleEnter()) {
            isEnterUsed = true;
            return true;
        } else {
            return false;
        }
    }

    boolean handleDpad(int keyCode) {
        if (mStickyMeta) {mMetaKey.useMetaState();}
        if (mEngine.getState().isConverting()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mEngine.chooseAdjacentCandidate(false);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mEngine.chooseAdjacentCandidate(true);
            }
        } else if (!mEngine.canMoveCursor()) {
            return false;
        }

        return true;
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

    void sendToMushroom() {
        String clip;
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        CharSequence cs = cm.getText();
        if (cs == null) {
            clip = "";
        } else {
            clip = cs.toString();
        }

        String str = mEngine.prepareToMushroom(clip);

        Intent mushroom = new Intent(this, SKKMushroom.class);
        mushroom.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mushroom.putExtra(SKKMushroom.REPLACE_KEY, str);
        startActivity(mushroom);
    }

    void showInputMethodPicker() {
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
    }

    void showMenuDialog() {
        ListMenuServiceDialog dialog = new ListMenuServiceDialog(new String[] {
                getString(R.string.label_input_method),
                getString(R.string.label_pref_activity),
                getString(R.string.label_mushroom)
        });
        dialog.setListener(
                new ListMenuServiceDialog.Listener() {
                    @Override
                    public void onClick(int which) {
                        switch (which) {
                            case 0:
                                showInputMethodPicker();
                                break;
                            case 1:
                                Intent intent = new Intent(SKKService.this, SKKPrefs.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                break;
                            case 2:
                                sendToMushroom();
                                break;
                        }
                    }
                }
        );
        dialog.show(mCurrentInputView);
    }

    void changeLastChar(String type) {
        mEngine.changeLastChar(type);
    }

    public void setCandidates(List<String> list) {
        if (!isCandidatesViewShown) {
            if (mUseSoftKeyboard || SKKPrefs.getUseCandidatesView(this)) {
                setCandidatesViewShown(true);
            }
        }

        if (list != null) {
            mCandidateView.setContents(list);
        }
    }

    public void requestChooseCandidate(int index) {
        if (mCandidateView != null) { mCandidateView.choose(index); }
    }

    public void pickCandidateViewManually(int index) {
        mEngine.pickCandidateViewManually(index);
    }

    // カーソル直前に引数と同じ文字列があるなら，それを消してtrue なければfalse
    public boolean prepareReConversion(String candidate) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && candidate.equals(ic.getTextBeforeCursor(candidate.length(), 0))) {
            ic.deleteSurroundingText(candidate.length(), 0);
            return true;
        }

        return false;
    }

    private SKKKeyboardView getInputViewByType(int keyboardType) {
        SKKKeyboardView inputView = null;
        if (keyboardType == SKKEngine.KEYBOARD_HIRAGANA) {
            mFlickJPInputView.setHiraganaMode();
            inputView = mFlickJPInputView;
        } else if (keyboardType == SKKEngine.KEYBOARD_KATAKANA) {
            mFlickJPInputView.setKatakanaMode();
            inputView = mFlickJPInputView;
        } else if (keyboardType == SKKEngine.KEYBOARD_QWERTY){
            inputView = mQwertyInputView;
        } else if (keyboardType == SKKEngine.KEYBOARD_ABBREV) {
            inputView = mAbbrevKeyboardView;
        }
        return inputView;
    }

    public void changeSoftKeyboard(int keyboardType) {
        if (mUseSoftKeyboard) {
            SKKKeyboardView inputView = getInputViewByType(keyboardType);

            if (inputView != null && mCurrentInputView != inputView) {
                setInputView(inputView);
                mCurrentInputView = inputView;
            }
        }
    }

    @Override
    public void showStatusIcon(int iconRes) {
        if (!mUseSoftKeyboard) {
            if (iconRes != 0) { super.showStatusIcon(iconRes); }
        }
    }

    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            mHasComposingText = false;
            return false;
        }
        if (!mHasComposingText && text.length() == 0) {
            return true;
        }
        mHasComposingText = text.length() != 0;
        return ic.setComposingText(text, newCursorPosition);
    }
}
