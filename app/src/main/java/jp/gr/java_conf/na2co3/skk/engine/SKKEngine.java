package jp.gr.java_conf.na2co3.skk.engine;

import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.inputmethod.InputConnection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jp.gr.java_conf.na2co3.skk.SKKDictionary;
import jp.gr.java_conf.na2co3.skk.SKKPrefs;
import jp.gr.java_conf.na2co3.skk.SKKService;
import jp.gr.java_conf.na2co3.skk.SKKUserDictionary;
import jp.gr.java_conf.na2co3.skk.SKKUtils;
import jp.gr.java_conf.na2co3.skk.R;

public class SKKEngine {
    private SKKService mService;

    private SKKMode mMode = SKKHiraganaMode.INSTANCE;
    private SKKState mState = SKKNormalState.INSTANCE;

    private RomajiConverter mConverter = new RomajiConverter(this);

    // 候補のリスト
    // KanjiStateとAbbrevStateでは補完リスト，ChooseStateとAbbrevChooseStateでは変換候補リストになる
    private List<String> mCandidatesList;
    private int mCurrentCandidateIndex;

    // 漢字，Abbrev変換のキー 変換中は不変
    private StringBuilder mConvKey = new StringBuilder();
    // 送りがな
    private String mOkurigana = null;
    // 送りがなの最初の子音
    private String mOkuriConsonant = null;

    private List<SKKDictionary> mDicts;
    private SKKUserDictionary mUserDict;

    // 単語登録のための情報
    private Deque<RegistrationInfo> mRegistrationStack = new ArrayDeque<>();
    private class RegistrationInfo {
        String key;
        String okurigana;
        String okuriConsonant;
        String displayKey;
        StringBuilder entry;
        boolean abbrev;
        SKKMode mode;

        RegistrationInfo(String k, String o, String oc, String dk, boolean a, SKKMode m) {
            key = k;
            okurigana = o;
            okuriConsonant = oc;
            displayKey = dk;
            abbrev = a;
            mode = m;
            entry = new StringBuilder();
        }
    }

    private int mColorComposing;
    private int mColorConverting;

    private boolean mDisplayState = true;

    // 再変換のための情報
    private class ConversionInfo {
        String candidate;
        List<String> list;
        int index;
        String key;
        String okurigana;
        String okuriConconant;
        boolean abbrev;
        SKKMode mode;

        ConversionInfo(String cand, List<String> clist, int idx, String key, String okuri, String okuriConconant, boolean abbrev, SKKMode mode) {
            this.candidate = cand;
            this.list = clist;
            this.index = idx;
            this.key = key;
            this.okurigana = okuri;
            this.okuriConconant = okuriConconant;
            this.abbrev = abbrev;
            this.mode = mode;
        }
    }
    private SKKEngine.ConversionInfo mLastConversion = null;

    // 全角で入力する記号リスト
    private Map<String, String> mZenkakuSeparatorMap;

    public static final String LAST_CONVERTION_SMALL = "small";
    public static final String LAST_CONVERTION_DAKUTEN = "daku";
    public static final String LAST_CONVERTION_HANDAKUTEN = "handaku";
    public static final String LAST_CONVERTION_ROTATE = "rotate";

    public static final int KEYBOARD_HIRAGANA = 0;
    public static final int KEYBOARD_KATAKANA = 1;
    public static final int KEYBOARD_QWERTY = 2;
    public static final int KEYBOARD_ABBREV = 3;

    public SKKEngine(SKKService engine, List<SKKDictionary> dics, SKKUserDictionary userDic) {
        mService = engine;
        mDicts = dics;
        mUserDict = userDic;
        mZenkakuSeparatorMap = new HashMap<>();
        setZenkakuPunctuationMarks("en");
        Resources res = engine.getResources();
        mColorComposing = res.getColor(R.color.composing_composing);
        mColorConverting = res.getColor(R.color.composing_converting);
    }

    public void reopenDictionaries(List<SKKDictionary> dics) {
        for (SKKDictionary dic: mDicts) { dic.close(); }
        mDicts = dics;
    }

    public void setZenkakuPunctuationMarks(String type) {
        if (type.equals("en")) {
            mZenkakuSeparatorMap.put(".", "．");
            mZenkakuSeparatorMap.put(",", "，");
        } else if (type.equals("jp")) {
            mZenkakuSeparatorMap.put(".", "。");
            mZenkakuSeparatorMap.put(",", "、");
        } else if (type.equals("jp_en")) {
            mZenkakuSeparatorMap.put(".", "。");
            mZenkakuSeparatorMap.put(",", "，");
        } else {
            mZenkakuSeparatorMap.put(".", "．");
            mZenkakuSeparatorMap.put(",", "，");
        }
    }

    public void setDisplayState(boolean displayState) {
        mDisplayState = displayState;
    }

    public SKKState getState() { return mState; }
    public boolean canMoveCursor() { return (!mRegistrationStack.isEmpty() || mConverter.hasComposing() || mConvKey.length() != 0); }
    public void commitUserDictChanges() { mUserDict. commitChanges(); }

    public boolean ignoresKeyEvent() {
        return mMode == SKKASCIIMode.INSTANCE && mRegistrationStack.isEmpty();
    }

    public void processKey(int pcode) {
        if (pcode == ' ' && !mRegistrationStack.isEmpty() && !mState.isTransient()) {
            if (mRegistrationStack.peekFirst().entry.length() == 0) {
                return;
            }
        }
        if (!mState.processKey(this, pcode)) {
            mMode.processKey(this, pcode);
        }
        updateComposingText();
    }

    void processRomaji(int pcode) {
        mConverter.processKey(pcode);
    }

    void commitRomajiText(String text, char initial, boolean isUpper) {
        if (text != null) {
            if (mState.processRomajiExtension(this, text, isUpper)) {
                return;
            } else if (text.equals("q")) {
                mState.toggleKana(this);
                return;
            } else if (text.equals("l")) {
                if (isUpper) {
                    changeMode(SKKZenkakuMode.INSTANCE, true);
                } else {
                    changeMode(SKKASCIIMode.INSTANCE, true);
                }
                return;
            } else if (text.equals("/")) {
                changeState(SKKAbbrevState.INSTANCE, true);
                return;
            }
        }

        String hchr = mZenkakuSeparatorMap.get(text);
        if (hchr != null) {
            mState.processText(this, hchr, initial, isUpper);
        } else {
            mState.processText(this, text, initial, isUpper);
        }
    }

    void onFinishRomaji() {
        mState.onFinishRomaji(this);
    }

    public void processText(String text, boolean isShifted) {
        if (text.equals(" ") && !mRegistrationStack.isEmpty() && !mState.isTransient()) {
            if (mRegistrationStack.peekFirst().entry.length() == 0) {
                return;
            }
        }
        mConverter.flush();
        mState.processText(this, text, '\0', isShifted);
        mState.onFinishRomaji(this);
        updateComposingText();
    }

    public void handleKanaKey() {
        mConverter.flush();
        mState.finish(this);
        if (mState == SKKNormalState.INSTANCE && mMode == SKKHiraganaMode.INSTANCE) {
            if (SKKPrefs.getToggleKanaKey(mService)) {
                changeMode(SKKASCIIMode.INSTANCE, false);
            }
        } else {
            changeMode(SKKHiraganaMode.INSTANCE, false);
        }
        updateComposingText();
    }

    public boolean handleBackKey() {
        boolean shouldReset = false;
        if (!mRegistrationStack.isEmpty()) {
            mRegistrationStack.clear();
            shouldReset = true;
        } else if (mConverter.hasComposing()) {
            shouldReset = true;
        }

        if (mState.isTransient()) {
            changeState(SKKNormalState.INSTANCE);
            return true;
        } else if (shouldReset) {
            reset();
            return true;
        }

        return false;
    }

    public boolean handleEnter() {
        boolean result = false;
        if (mConverter.flush()) {
            result = true;
        }
        if (mState.finish(this)) {
            changeState(SKKNormalState.INSTANCE);
            result = true;
        }
        if (!result) {
            if (!mRegistrationStack.isEmpty()) {
                if (mRegistrationStack.peekFirst().entry.length() != 0) {
                    registerWord();
                } else {
                    cancelRegister();
                }
            } else {
                return false;
            }
        }

        updateComposingText();
        return true;
    }

    public boolean handleBackspace(boolean softKeyboard) {
        mState.beforeBackspace(this);

        if (mConverter.handleBackspace()) {
            mState.afterBackspace(this);
            updateComposingText();
            return true;
        }

        int olen = mOkurigana != null ? mOkurigana.length() : 0;
        int clen = mConvKey.length();

        if (softKeyboard && mState.isConverting()) {
            return handleCancel();
        }

        if (olen > 0) {
            if (olen == 1) {
                mOkurigana = null;
                mOkuriConsonant = null;
            } else {
                mOkurigana = mOkurigana.substring(0, olen - 1);
            }
        } else if (clen > 0) {
            mConvKey.deleteCharAt(clen-1);
        } else if (!mRegistrationStack.isEmpty()) {
            StringBuilder regEntry = mRegistrationStack.peekFirst().entry;
            if (regEntry.length() > 0) {
                regEntry.deleteCharAt(regEntry.length() - 1);
            } else if (softKeyboard) {
                return handleCancel();
            }
        } else if (!mState.isTransient()) {
            return false;
        }
        mState.afterBackspace(this);

        updateComposingText();
        return true;
    }

    public boolean handleCancel() {
        boolean result = false;
        if (mConverter.reset()) {
            result = true;
        }
        if (mState.handleCancel(this)) {
            result = true;
        } else if (!mRegistrationStack.isEmpty()) {
            cancelRegister();
            result = true;
        } else if (reConversion()) {
            result = true;
        }
        if (result) {
            updateComposingText();
        }
        return result;
    }

    public void toASCIIMode() {
        mConverter.flush();
        changeMode(SKKASCIIMode.INSTANCE, true);
        updateComposingText();
    }

    public void toAbbrevState() {
        mConverter.flush();
        changeState(SKKAbbrevState.INSTANCE, true);
        updateComposingText();
    }

    public void toggleKana() {
        mConverter.flush();
        mState.toggleKana(this);
        updateComposingText();
    }

    SKKMode getToggledKanaMode() {
        return mMode.getToggledKanaMode();
    }

    CharSequence convertText(CharSequence text) {
        return mMode.convertText(text);
    }

    /**
     * commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
     * @param text
     * @param newCursorPosition
     */
    void commitTextSKK(CharSequence text, int newCursorPosition) {
        InputConnection ic = mService.getCurrentInputConnection();
        if (ic == null) { return; }

        if (!mRegistrationStack.isEmpty()) {
            mRegistrationStack.peekFirst().entry.append(text);
        } else {
            ic.commitText(text, newCursorPosition);
        }
    }

    public void resetOnStartInput() {
        mConverter.reset();
        mConvKey.setLength(0);
        mOkurigana = null;
        mOkuriConsonant = null;
        mCandidatesList = null;
        if (mState.isTransient()) {
            changeState(SKKNormalState.INSTANCE);
        }
        int icon = getCurrentIcon();
        if (icon != 0) {
            mService.showStatusIcon(icon);
        } else {
            mService.hideStatusIcon();
        }

        // onStartInput()では，WebViewのときsetComposingText("", 1)すると落ちるようなのでやめる
    }

    public void chooseAdjacentSuggestion(boolean isForward) {
        if (mCandidatesList == null) { return; }
        if (isForward) {
            mCurrentCandidateIndex++;
        } else {
            mCurrentCandidateIndex--;
        }

        // 範囲外になったら反対側へ
        if (mCurrentCandidateIndex > mCandidatesList.size() - 1) {
            mCurrentCandidateIndex = 0;
        } else if (mCurrentCandidateIndex < 0) {
            mCurrentCandidateIndex = mCandidatesList.size() - 1;
        }

        mService.requestChooseCandidate(mCurrentCandidateIndex);
    }

    public void chooseAdjacentCandidate(boolean isForward) {
        if (mCandidatesList == null) { return; }
        if (isForward) {
            mCurrentCandidateIndex++;
        } else {
            mCurrentCandidateIndex--;
        }

        // 最初の候補より戻ると変換に戻る 最後の候補より進むと登録
        if (mCurrentCandidateIndex > mCandidatesList.size() - 1) {
            registerStart(mState == SKKAbbrevChooseState.INSTANCE);
            updateComposingText();
            return;
        } else if (mCurrentCandidateIndex < 0) {
            if (mOkurigana != null) {
                mConvKey.append(mOkurigana);
                mOkurigana = null;
                mOkuriConsonant = null;
            }
            if (mState == SKKChooseState.INSTANCE) {
                changeState(SKKKanjiState.INSTANCE);
            } else {
                changeState(SKKAbbrevState.INSTANCE);
            }
            updateSuggestions();
            updateComposingText();

            mCurrentCandidateIndex = 0;
            return;
        }

        mService.requestChooseCandidate(mCurrentCandidateIndex);
        updateComposingText();
    }

    public void pickCandidateViewManually(int index) {
        if (mState.isConverting()) {
            pickCandidate(index);
            updateComposingText();
        } else if (mState == SKKAbbrevState.INSTANCE || mState == SKKKanjiState.INSTANCE) {
            pickSuggestion(index);
            updateComposingText();
        }
    }

    public String prepareToMushroom(String clip) {
        String str;
        if (mState == SKKKanjiState.INSTANCE || mState == SKKAbbrevState.INSTANCE) {
            str = mConvKey.toString();
        } else {
            str = clip;
        }

        if (mState.isTransient()) {
            changeState(SKKNormalState.INSTANCE);
        } else {
            reset();
            mRegistrationStack.clear();
        }

        return str;
    }

    // 最後の文字の小文字，濁音，半濁音を順次変換する
    public void changeLastChar(String type) {
        if (mState == SKKKanjiState.INSTANCE && !mConverter.hasComposing()) {
            // ▽モード
            String s = mConvKey.toString();
            int idx = s.length() - 1;
            String lastchar = s.substring(idx);
            String new_lastchar = RomajiConverter.convertLastChar(lastchar, type);

            if (new_lastchar != null) {
                mConvKey.deleteCharAt(idx);
                mConvKey.append(new_lastchar);
                updateSuggestions();
                updateComposingText();
            }
            return;
        }

        if (mState == SKKChooseState.INSTANCE) {
            // 変換中 (送り仮名を変換)
            if (mOkurigana == null) return;
            String new_okuri = RomajiConverter.convertLastChar(mOkurigana, type);

            if (new_okuri != null) {
                String new_okuri_consonant = RomajiConverter.getConsonant(new_okuri);
                if (new_okuri_consonant != null) {
                    mOkuriConsonant = new_okuri_consonant;
                }
                mOkurigana = new_okuri;
                conversionStart(); //変換やりなおし
                updateComposingText();
            }
            return;
        }

        if (!mConverter.hasComposing() && mConvKey.length() == 0) {
            if (!mRegistrationStack.isEmpty()) {
                RegistrationInfo regInfo = mRegistrationStack.peekFirst();
                if (regInfo.entry.length() == 0) {
                    // 単語登録 (送り仮名を変換)
                    if (regInfo.okurigana == null) return;
                    String new_okuri = RomajiConverter.convertLastChar(regInfo.okurigana, type);

                    if (new_okuri != null) {
                        mConvKey.setLength(0);
                        mConvKey.append(regInfo.key);
                        String new_okuri_consonant = RomajiConverter.getConsonant(new_okuri);
                        if (new_okuri_consonant != null) {
                            mOkuriConsonant = new_okuri_consonant;
                        }
                        mRegistrationStack.removeFirst();
                        mOkurigana = new_okuri;
                        conversionStart(); //変換やりなおし
                        updateComposingText();
                    }
                    return;
                }
            }

            String lastchar;
            String new_lastchar;

            InputConnection ic = mService.getCurrentInputConnection();
            if (ic != null) {
                CharSequence cs = ic.getTextBeforeCursor(1, 0);
                if (cs != null) {
                    lastchar = cs.toString();
                    new_lastchar = RomajiConverter.convertLastChar(lastchar, type);

                    if (new_lastchar != null) {
                        if (!mRegistrationStack.isEmpty()) {
                            StringBuilder regEntry = mRegistrationStack.peekFirst().entry;
                            regEntry.deleteCharAt(regEntry.length()-1);
                            regEntry.append(new_lastchar);
                            updateComposingText();
                        } else {
                            ic.deleteSurroundingText(1, 0);
                            ic.commitText(new_lastchar, 1);
                        }
                    }
                }
            }
        }
    }


    boolean hasComposing() { return mConverter.hasComposing(); }
    StringBuilder getConvKey() { return mConvKey; }
    String getOkurigana() { return mOkurigana; }
    void setOkurigana(String okr) { mOkurigana = okr; }
    void setOkuriConsonant(String c) { mOkuriConsonant = c; }

    private void updateComposingText() {
        SpannableStringBuilder ct = new SpannableStringBuilder();

        if (!mRegistrationStack.isEmpty()) {
            for (Iterator<RegistrationInfo> iterator = mRegistrationStack.descendingIterator(); iterator.hasNext();) {
                RegistrationInfo regInfo = iterator.next();
                int bgStart = ct.length();
                if (mDisplayState) {
                    ct.append("▼");
                }
                ct.append(regInfo.displayKey);
                ct.append("：");
                ct.setSpan(new BackgroundColorSpan(mColorConverting), bgStart, ct.length(), Spanned.SPAN_COMPOSING);
                ct.append(regInfo.entry);
            }
        }

        BackgroundColorSpan bg = null;
        int bgStart = 0;

        if (mState.isConverting()) {
            bg = new BackgroundColorSpan(mColorConverting);
            bgStart = ct.length();
            if (mDisplayState) {
                ct.append("▼");
            }
        } else if (mState.isTransient()) {
            bg = new BackgroundColorSpan(mColorComposing);
            bgStart = ct.length();
            if (mDisplayState) {
                ct.append("▽");
            }
        }
        CharSequence text = mState.getComposingText(this);
        if (text != null) {
            ct.append(text);
        }
        ct.append(mConverter.getComposing());

        if (bg != null) {
            ct.setSpan(bg, bgStart, ct.length(), Spanned.SPAN_COMPOSING);
        }
        if (ct.length() != 0) {
            ct.setSpan(new UnderlineSpan(), 0, ct.length(), Spanned.SPAN_COMPOSING);
        }

        mService.setComposingText(ct, 1);
    }

    /***
     * 変換スタート
     */
    void conversionStart() {
        if (!conversionStartInternal(false, false)) {
            registerStart(false);
        }
    }

    void abbrevConversionStart() {
        if (!conversionStartInternal(true, false)) {
            registerStart(true);
        }
    }

    private boolean conversionStartInternal(boolean abbrev, boolean lastCandidate) {
        String str = mConvKey.toString();
        if (mOkuriConsonant != null) {
            str += mOkuriConsonant;
        }

        List<String> list = findCandidates(str);
        if (list == null) {
            return false;
        }

        if (abbrev) {
            changeState(SKKAbbrevChooseState.INSTANCE);
        } else {
            changeState(SKKChooseState.INSTANCE);
        }
        mCandidatesList = list;
        mCurrentCandidateIndex = lastCandidate ? list.size() - 1 : 0;
        mService.setCandidates(list);
        if (mCurrentCandidateIndex != 0) {
            mService.requestChooseCandidate(mCurrentCandidateIndex);
        }
        return true;
    }

    private boolean reConversion() {
        if (mLastConversion == null) { return false; }

        String s = mLastConversion.candidate;
        SKKUtils.dlog("last conversion: " + s);
        if (mService.prepareReConversion(s)) {
            mUserDict.rollBack();

            mConvKey.setLength(0);
            mConvKey.append(mLastConversion.key);
            mOkurigana = mLastConversion.okurigana;
            mOkuriConsonant = mLastConversion.okuriConconant;
            mCandidatesList = mLastConversion.list;
            mCurrentCandidateIndex = mLastConversion.index;
            mMode = mLastConversion.mode;
            mService.setCandidates(mCandidatesList);

            if (mLastConversion.abbrev) {
                changeState(SKKAbbrevChooseState.INSTANCE);
            } else {
                changeState(SKKChooseState.INSTANCE);
            }

            return true;
        }

        return false;
    }

    void updateSuggestions() {
        if (mConvKey.length() == 0) { return; }

        String text = mConvKey.toString();
        if (mOkuriConsonant != null) {
            text += mOkuriConsonant;
        }

        List<String> list = new ArrayList<>();
        for (SKKDictionary dic: mDicts) {
            list.addAll(dic.findKeys(text));
        }
        List<String> list2 = mUserDict.findKeys(text);
        int idx = 0;
        for (String s : list2) {
            //個人辞書のキーを先頭に追加
            list.remove(s);
            list.add(idx, s);
            idx++;
        }

        mCandidatesList = list;
        mCurrentCandidateIndex = 0;
        mService.setCandidates(list);
    }

    private void registerStart(boolean abbrev) {
        StringBuilder displayKey = new StringBuilder(mMode.convertText(mConvKey));
        if (mOkurigana != null) {
            displayKey.append("*");
            displayKey.append(mMode.convertText(mOkurigana));
        }

        RegistrationInfo regInfo = new RegistrationInfo(
                mConvKey.toString(),
                mOkurigana,
                mOkuriConsonant,
                displayKey.toString(),
                abbrev,
                mMode
        );
        mRegistrationStack.addFirst(regInfo);
        changeState(SKKNormalState.INSTANCE);
        //setComposingTextSKK("", 1);
    }

    private void registerWord() {
        RegistrationInfo regInfo = mRegistrationStack.removeFirst();
        if (regInfo.entry.length() != 0) {
            String regEntryStr = regInfo.entry.toString();
            if (regEntryStr.indexOf(';') != -1 || regEntryStr.indexOf('/') != -1) {
                // セミコロンとスラッシュのエスケープ
                regEntryStr = "(concat \""
                    + regEntryStr.replace(";", "\\073").replace("/", "\\057")
                    +  "\")";
            }
            String regKeyStr = regInfo.key;
            if (regInfo.okuriConsonant != null) {
                regKeyStr += regInfo.okuriConsonant;
            }
            mUserDict.addEntry(regKeyStr, regEntryStr, regInfo.okurigana);
            mUserDict.commitChanges();
            mMode = regInfo.mode;
            if (regInfo.okurigana == null || regInfo.okurigana.length() == 0) {
                commitTextSKK(mMode.convertText(regInfo.entry), 1);
            } else {
                commitTextSKK(mMode.convertText(regInfo.entry.append(regInfo.okurigana)), 1);
            }
            mService.changeSoftKeyboard(getCurrentKeyboardType());
        }
        reset();
    }

    private void cancelRegister() {
        RegistrationInfo regInfo = mRegistrationStack.removeFirst();
        mConvKey.setLength(0);
        mConvKey.append(regInfo.key);
        mOkurigana = regInfo.okurigana;
        mOkuriConsonant = regInfo.okuriConsonant;
        mMode = regInfo.mode;
        if (conversionStartInternal(regInfo.abbrev, true)) {
            return;
        }

        if (mOkurigana != null) {
            mConvKey.append(mOkurigana);
            mOkurigana = null;
            mOkuriConsonant = null;
        }
        if (regInfo.abbrev) {
            changeState(SKKAbbrevState.INSTANCE);
        } else {
            changeState(SKKKanjiState.INSTANCE);
        }
        updateSuggestions();
    }

    private List<String> findCandidates(String key) {
        List<String> list1 = new ArrayList<>();
        for (SKKDictionary dic: mDicts) {
            String[] cands = dic.getCandidates(key);
            if (cands != null) {
                Collections.addAll(list1, cands);
            }
        }

        List<String> list2 = null;

        SKKUserDictionary.Entry entry = mUserDict.getEntry(key);
        if (entry != null) {
            list2 = entry.candidates;
        }

        if (list1.isEmpty() && list2 == null) {
            SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
            return null;
        }

        if (list2 != null) {
            int idx = 0;
            for (String s : list2) {
                if (mOkurigana != null) {
                    boolean found = false;
                    for (List<String> lst : entry.okuri_blocks) {
                        if (lst.get(0).equals(mOkurigana) && lst.contains(s)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {continue;} //送りがなブロックに見つからなければ，追加しない
                }
                //個人辞書の候補を先頭に追加
                list1.remove(s);
                list1.add(idx, s);
                idx++;
            }
        }
        if (list1.size() == 0) {list1 = null;}

        return list1;
    }

    String getCurrentCandidate() {
        String candidate = SKKUtils.processConcatAndEscape(SKKUtils.removeAnnotation(mCandidatesList.get(mCurrentCandidateIndex)));
        if (mOkurigana != null) {
            return candidate.concat(mOkurigana);
        } else {
            return candidate;
        }

    }

    void pickCurrentCandidate() {
        pickCandidate(mCurrentCandidateIndex);
    }

    private void pickCandidate(int index) {
        if (!mState.isConverting()) {
            return;
        }

        String s = mCandidatesList.get(index);
        String candidate = SKKUtils.processConcatAndEscape(SKKUtils.removeAnnotation(s));
        if (mOkurigana != null) {
            candidate = mMode.convertText(candidate + mOkurigana).toString();
        } else {
            candidate = mMode.convertText(candidate).toString();
        }
        commitTextSKK(candidate, 1);

        String key = mConvKey.toString();
        if (mOkuriConsonant != null) {
            key += mOkuriConsonant;
        }
        mUserDict.addEntry(key, s, mOkurigana);
        // ユーザー辞書登録時はエスケープや注釈を消さない

        if (mRegistrationStack.isEmpty()) {
            mLastConversion = new SKKEngine.ConversionInfo(
                    candidate,
                    mCandidatesList,
                    index,
                    mConvKey.toString(),
                    mOkurigana,
                    mOkuriConsonant,
                    mState == SKKAbbrevChooseState.INSTANCE,
                    mMode);
        }

        changeState(SKKNormalState.INSTANCE);
    }

    void pickCurrentSuggestion() {
        pickSuggestion(mCurrentCandidateIndex);
    }

    private void pickSuggestion(int index) {
        String s = mCandidatesList.get(index);

        mConvKey.setLength(0);
        mConvKey.append(s);
        if (mState == SKKAbbrevState.INSTANCE) {
            abbrevConversionStart();
        } else if (mState == SKKKanjiState.INSTANCE) {
            int li = s.length() - 1;
            int last = s.codePointAt(li);
            if (SKKUtils.isAlphabet(last)) {
                mConvKey.deleteCharAt(li);
                processKey(Character.toUpperCase(last));
            } else {
                conversionStart();
            }
        }
    }

    private void reset() {
        mConverter.reset();
        mConvKey.setLength(0);
        mOkurigana = null;
        mOkuriConsonant = null;
        mCandidatesList = null;

        mService.setCandidatesViewShown(false);
//        mMetaKey.clearMetaKeyState();
//        if (mStickyMeta) {mMetaKey.clearMetaKeyState();}
    }

    void changeMode(SKKMode mode, boolean finish) {
        if (finish) {
            mState.finish(this);
        }
        mMode = mode;
        changeState(SKKNormalState.INSTANCE);
    }

    void changeState(SKKState state, boolean finish) {
        if (finish) {
            mState.finish(this);
        }
        changeState(state);
    }

    void changeState(SKKState state) {
        mState = state;

        if (!state.isTransient()) {
            reset();
        }
        mService.changeSoftKeyboard(getCurrentKeyboardType());

        int icon = getCurrentIcon();
        if (icon != 0) {
            mService.showStatusIcon(icon);
        } else {
            mService.hideStatusIcon();
        }
    }

    public int getCurrentKeyboardType() {
        int keyboardType = mState.getKeyboardType(this);
        if (keyboardType < 0) {
            keyboardType = mMode.getKeyboardType();
        }
        return keyboardType;
    }

    private int getCurrentIcon() {
        int icon = mState.getIcon();
        if (icon == 0) {
            icon = mMode.getIcon();
        }
        return icon;
    }
}
