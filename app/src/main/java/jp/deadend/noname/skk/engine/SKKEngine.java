package jp.deadend.noname.skk.engine;

import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.deadend.noname.skk.SKKDictionary;
import jp.deadend.noname.skk.SKKPrefs;
import jp.deadend.noname.skk.SKKService;
import jp.deadend.noname.skk.SKKUserDictionary;
import jp.deadend.noname.skk.SKKUtils;

public class SKKEngine {
    private SKKService mService;

    private SKKState mState = SKKHiraganaState.INSTANCE;

    // 候補のリスト．KanjiStateとAbbrevStateでは補完リスト，ChooseStateでは変換候補リストになる
    private List<String> mCandidatesList;
    private int mCurrentCandidateIndex;

    // ひらがなや英単語などの入力途中
    private StringBuilder mComposing = new StringBuilder();
    // 漢字変換のキー 送りありの場合最後がアルファベット 変換中は不変
    private StringBuilder mKanjiKey = new StringBuilder();
    // 送りがな 「っ」や「ん」が含まれる場合だけ二文字になる
    private String mOkurigana = null;

    private boolean isRegistering = false;
    // 単語登録のキー 登録作業中は不変
    private String mRegKey = null;
    // 単語登録する内容
    private StringBuilder mRegEntry = new StringBuilder();
    private String mRegOkurigana = null;

    private List<SKKDictionary> mDicts;
    private SKKUserDictionary mUserDict;

    // 再変換のための情報
    private class ConversionInfo {
        String candidate;
        List<String> list;
        int index;
        String kanjiKey;
        String okuri;

        ConversionInfo(String cand, List<String> clist, int idx, String key, String okuri) {
            this.candidate = cand;
            this.list = clist;
            this.index = idx;
            this.kanjiKey = key;
            this.okuri = okuri;
        }
    }
    private SKKEngine.ConversionInfo mLastConversion = null;

    // 全角で入力する記号リスト
    private Map<String, String> mZenkakuSeparatorMap;

    public static final String LAST_CONVERTION_SMALL = "small";
    public static final String LAST_CONVERTION_DAKUTEN = "daku";
    public static final String LAST_CONVERTION_HANDAKUTEN = "handaku";

    public SKKEngine(SKKService engine, List<SKKDictionary> dics, SKKUserDictionary userDic) {
        mService = engine;
        mDicts = dics;
        mUserDict = userDic;
        mZenkakuSeparatorMap = new HashMap<>();
        mZenkakuSeparatorMap.put("-", "ー");
        mZenkakuSeparatorMap.put("!", "！");
        mZenkakuSeparatorMap.put("?", "？");
        mZenkakuSeparatorMap.put("~", "〜");
        mZenkakuSeparatorMap.put("[", "「");
        mZenkakuSeparatorMap.put("]", "」");
        setZenkakuPunctuationMarks("en");
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

    public SKKState getState() { return mState; }
    public boolean isRegistering() { return isRegistering; }
    public boolean canMoveCursor() { return (isRegistering || mComposing.length() != 0 || mKanjiKey.length() != 0); }
    public void commitUserDictChanges() { mUserDict. commitChanges(); }

    public void processKey(int pcode) { mState.processKey(this, pcode); }

    public void handleKanaKey() { mState.handleKanaKey(this); }

    public boolean handleBackKey() {
        if (isRegistering) {
            isRegistering = false;
            mRegKey = null;
            mRegEntry.setLength(0);
        }

        if (mState.isTransient()) {
            changeState(SKKHiraganaState.INSTANCE);
            return true;
        } else {
            if (isRegistering) {
                reset();
                return true;
            }
        }

        return false;
    }

    public boolean handleEnter() {
        if (mState == SKKChooseState.INSTANCE) {
            pickCandidate(mCurrentCandidateIndex);
        } else if (mState == SKKAbbrevState.INSTANCE) {
            if (mComposing.length() > 0) {
                commitTextSKK(mComposing, 1);
                mComposing.setLength(0);
            }
            changeState(SKKHiraganaState.INSTANCE);
        } else if (mState == SKKKanjiState.INSTANCE || mState == SKKOkuriganaState.INSTANCE) {
            commitTextSKK(mKanjiKey, 1);
            changeState(SKKHiraganaState.INSTANCE);
        } else {
            if (mComposing.length() == 0) {
                if (isRegistering) {
                    registerWord();
                } else {
                    return false;
                }
            } else {
                commitTextSKK(mComposing, 1);
                mComposing.setLength(0);
            }
        }

        return true;
    }

    public boolean handleBackspace() {
        int clen = mComposing.length();
        int klen = mKanjiKey.length();

        // 変換中のものがない場合
        if (clen == 0 && klen == 0) {
            if (isRegistering) {
                int rlen = mRegEntry.length();
                if (rlen > 0) {
                    mRegEntry.deleteCharAt(rlen - 1);
                    setComposingTextSKK("", 1);
                }
            } else if (mState.isTransient()) {
                return true;
            } else {
                return false;
            }
        }

        if (clen > 0) {
            mComposing.deleteCharAt(clen-1);
        } else if (klen > 0) {
            mKanjiKey.deleteCharAt(klen-1);
        }
        mState.afterBackspace(this);

        return true;
    }

    public boolean handleCancel() {
        return mState.handleCancel(this);
    }

    /**
     * commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
     * @param text
     * @param newCursorPosition
     */
    public void commitTextSKK(CharSequence text, int newCursorPosition) {
        InputConnection ic = mService.getCurrentInputConnection();
        if (ic == null) { return; }

        if (isRegistering) {
            mRegEntry.append(text);
            setComposingTextSKK("", newCursorPosition);
        } else {
            ic.commitText(text, newCursorPosition);
        }
    }

    public void resetOnStartInput() {
        mComposing.setLength(0);
        mKanjiKey.setLength(0);
        mOkurigana = null;
        mCandidatesList = null;
        if (mState.isTransient()) {
            changeState(SKKHiraganaState.INSTANCE);
            mService.showStatusIcon(mState.getIcon());
        } else if (mState == SKKASCIIState.INSTANCE) {
            mService.hideStatusIcon();
        } else {
            mService.showStatusIcon(mState.getIcon());
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
            if (!isRegistering) { registerStart(mKanjiKey.toString()); }
            return;
        } else if (mCurrentCandidateIndex < 0) {
            if (mComposing.length() == 0) {
                // KANJIモードに戻る
                if (mOkurigana != null) {
                    mOkurigana = null;
                    mKanjiKey.deleteCharAt(mKanjiKey.length() -1);
                }
                changeState(SKKKanjiState.INSTANCE);
                setComposingTextSKK(mKanjiKey, 1);
                updateSuggestions(mKanjiKey.toString());
            } else {
                mKanjiKey.setLength(0);
                changeState(SKKAbbrevState.INSTANCE);
                setComposingTextSKK(mComposing, 1);
                updateSuggestions(mComposing.toString());
            }

            mCurrentCandidateIndex = 0;
        }

        mService.requestChooseCandidate(mCurrentCandidateIndex);
        setCurrentCandidateToComposing();
    }

    public void pickCandidateViewManually(int index) {
        if (mState == SKKChooseState.INSTANCE) {
            pickCandidate(index);
        } else if (mState == SKKAbbrevState.INSTANCE || mState == SKKKanjiState.INSTANCE) {
            pickSuggestion(index);
        }
    }

    public String prepareToMushroom(String clip) {
        String str;
        if (mState == SKKKanjiState.INSTANCE || mState == SKKAbbrevState.INSTANCE) {
            str = mKanjiKey.toString();
        } else {
            str = clip;
        }

        if (mState.isTransient()) {
            changeState(SKKHiraganaState.INSTANCE);
        } else {
            reset();
        }

        return str;
    }

    // 小文字大文字変換，濁音，半濁音に使う
    public void changeLastChar(String type) {
        if (mState == SKKKanjiState.INSTANCE && mComposing.length() == 0) {
            String s = mKanjiKey.toString();
            int idx = s.length() - 1;
            String lastchar = s.substring(idx);
            String new_lastchar = RomajiConverter.INSTANCE.convertLastChar(lastchar, type);

            if (new_lastchar != null) {
                mKanjiKey.deleteCharAt(idx);
                mKanjiKey.append(new_lastchar);
                setComposingTextSKK(mKanjiKey, 1);
                updateSuggestions(mKanjiKey.toString());
            }
            return;
        }

        if (mState == SKKChooseState.INSTANCE) {
            if (mOkurigana == null) return;
            String new_okuri = RomajiConverter.INSTANCE.convertLastChar(mOkurigana, type);

            if (new_okuri != null) {
                // 例外: 送りがなが「っ」になる場合は，どのみち必ずt段の音なのでmKanjiKeyはそのまま
                // 「ゃゅょ」で送りがなが始まる場合はないはず
                if (!type.equals(LAST_CONVERTION_SMALL)) {
                    String new_okuri_consonant = RomajiConverter.INSTANCE.getConsonantForVoiced(new_okuri);
                    mKanjiKey.deleteCharAt(mKanjiKey.length() - 1);
                    mKanjiKey.append(new_okuri_consonant);
                }
                mOkurigana = new_okuri;
                conversionStart(mKanjiKey); //変換やりなおし
            }
            return;
        }

        if (mComposing.length() == 0 && mKanjiKey.length() == 0) {
            String lastchar;
            String new_lastchar;

            InputConnection ic = mService.getCurrentInputConnection();
            if (ic != null) {
                CharSequence cs = ic.getTextBeforeCursor(1, 0);
                if (cs != null) {
                    lastchar = cs.toString();
                    new_lastchar = RomajiConverter.INSTANCE.convertLastChar(lastchar, type);

                    if (new_lastchar != null) {
                        if (isRegistering) {
                            mRegEntry.deleteCharAt(mRegEntry.length()-1);
                            mRegEntry.append(new_lastchar);
                            ic.setComposingText("▼" + mRegKey + "：" + mRegEntry, 1);
                        } else {
                            ic.deleteSurroundingText(1, 0);
                            ic.commitText(new_lastchar, 1);
                        }
                    }
                }
            }
        }
    }


    StringBuilder getComposing() { return mComposing; }
    StringBuilder getKanjiKey() { return mKanjiKey; }
    String getOkurigana() { return mOkurigana; }
    void setOkurigana(String okr) { mOkurigana = okr; }
    String getZenkakuSeparator(String key) { return mZenkakuSeparatorMap.get(key); }
    boolean getToggleKanaKey() { return SKKPrefs.getToggleKanaKey(mService); }

    /**
     * setComposingTextのラッパー 変換モードマーク等を追加する
     * @param text
     * @param newCursorPosition
     */
    void setComposingTextSKK(CharSequence text, int newCursorPosition) {
        InputConnection ic = mService.getCurrentInputConnection();
        if (ic == null) return;

        StringBuilder ct = new StringBuilder();

        if (isRegistering) {
            ct.append("▼");
            if (mRegOkurigana == null) {
                ct.append(mRegKey);
            } else {
                ct.append(mRegKey.substring(0, mRegKey.length()-1));
                ct.append("*");
                ct.append(mRegOkurigana);
            }
            ct.append("：");
            ct.append(mRegEntry);
        }

        if (mState == SKKAbbrevState.INSTANCE || mState == SKKKanjiState.INSTANCE || mState == SKKOkuriganaState.INSTANCE) {
            ct.append("▽");
        } else if (mState == SKKChooseState.INSTANCE) {
            ct.append("▼");
        }
        ct.append(text);

        ic.setComposingText(ct, newCursorPosition);
    }

    /***
     * 変換スタート
     * 送りありの場合，事前に送りがなをmOkuriganaにセットしておく
     * @param key 辞書のキー 送りありの場合最後はアルファベット
     */
    void conversionStart(StringBuilder key) {
        String str = key.toString();

        changeState(SKKChooseState.INSTANCE);

        List<String> list = findCandidates(str);
        if (list == null) {
            if (!isRegistering) {
                registerStart(str);
            } else {
                changeState(SKKHiraganaState.INSTANCE);
            }
            return;
        }

        mCandidatesList = list;
        mCurrentCandidateIndex = 0;
        mService.setCandidates(list);
        setCurrentCandidateToComposing();
    }

    boolean reConversion() {
        if (mLastConversion == null) { return false; }

        String s = mLastConversion.candidate;
        SKKUtils.dlog("last conversion: " + s);
        if (mService.prepareReConversion(s)) {
            mUserDict.rollBack();

            mComposing.setLength(0);
            mKanjiKey.setLength(0);
            mKanjiKey.append(mLastConversion.kanjiKey);
            mOkurigana = mLastConversion.okuri;
            mCandidatesList = mLastConversion.list;
            mCurrentCandidateIndex = mLastConversion.index;
            mService.setCandidates(mCandidatesList);
            setCurrentCandidateToComposing();

            changeState(SKKChooseState.INSTANCE);

            return true;
        }

        return false;
    }

    void updateSuggestions(String str) {
        if (str.length() == 0) { return; }

        List<String> list = new ArrayList<>();
        for (SKKDictionary dic: mDicts) {
            list.addAll(dic.findKeys(str));
        }
        List<String> list2 = mUserDict.findKeys(str);
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

    private void registerStart(String str) {
        mRegKey = str;
        mRegEntry.setLength(0);
        if (mOkurigana != null) {
            mRegOkurigana = mOkurigana;
        } else {
            mRegOkurigana = null;
        }
        isRegistering = true;
        changeState(SKKHiraganaState.INSTANCE);
        //setComposingTextSKK("", 1);
    }

    private void registerWord() {
        isRegistering = false;
        if (mRegEntry.length() != 0) {
            String regEntryStr = mRegEntry.toString();
            if (regEntryStr.indexOf(';') != -1 || regEntryStr.indexOf('/') != -1) {
                // セミコロンとスラッシュのエスケープ
                regEntryStr = "(concat \""
                    + regEntryStr.replace(";", "\\073").replace("/", "\\057")
                    +  "\")";
            }
            mUserDict.addEntry(mRegKey, regEntryStr, mRegOkurigana);
            mUserDict.commitChanges();
            if (mRegOkurigana == null || mRegOkurigana.length() == 0) {
                commitTextSKK(mRegEntry, 1);
            } else {
                commitTextSKK(mRegEntry.append(mRegOkurigana), 1);
            }
        }
        mRegKey = null;
        mRegOkurigana = null;
        mRegEntry.setLength(0);
        reset();
    }

    void cancelRegister() {
        isRegistering = false;
        mKanjiKey.setLength(0);
        mKanjiKey.append(mRegKey);
        mRegKey = null;
        mRegEntry.setLength(0);
        mComposing.setLength(0);
        changeState(SKKKanjiState.INSTANCE);
        setComposingTextSKK(mKanjiKey, 1);
        updateSuggestions(mKanjiKey.toString());
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

    private void setCurrentCandidateToComposing() {
        String candidate = SKKUtils.processConcatAndEscape(SKKUtils.removeAnnotation(mCandidatesList.get(mCurrentCandidateIndex)));
        if (mOkurigana != null) {
            setComposingTextSKK(candidate.concat(mOkurigana), 1);
        } else {
            setComposingTextSKK(candidate, 1);
        }

    }

    void pickCurrentCandidate() {
        pickCandidate(mCurrentCandidateIndex);
    }

    private void pickCandidate(int index) {
        if (mState != SKKChooseState.INSTANCE) {
            return;
        }

        String s = mCandidatesList.get(index);
        String candidate = SKKUtils.processConcatAndEscape(SKKUtils.removeAnnotation(s));

        commitTextSKK(candidate, 1);
        if (mOkurigana != null) { commitTextSKK(mOkurigana, 1); }
        mUserDict.addEntry(mKanjiKey.toString(), s, mOkurigana);
        // ユーザー辞書登録時はエスケープや注釈を消さない

        if (!isRegistering) {
            if (mOkurigana != null) {
                mLastConversion = new SKKEngine.ConversionInfo(candidate + mOkurigana, mCandidatesList, index, mKanjiKey.toString(), mOkurigana);
            } else {
                mLastConversion = new SKKEngine.ConversionInfo(candidate, mCandidatesList, index, mKanjiKey.toString(), null);
            }
        }

        changeState(SKKHiraganaState.INSTANCE);
    }

    void pickCurrentSuggestion() {
        pickSuggestion(mCurrentCandidateIndex);
    }

    private void pickSuggestion(int index) {
        String s = mCandidatesList.get(index);

        if (mState == SKKAbbrevState.INSTANCE) {
            setComposingTextSKK(s, 1);
            mComposing.setLength(0);
            mComposing.append(s);
            conversionStart(mComposing);
        } else if (mState == SKKKanjiState.INSTANCE) {
            setComposingTextSKK(s, 1);
            int li = s.length() - 1;
            int last = s.codePointAt(li);
            if (SKKUtils.isAlphabet(last)) {
                mKanjiKey.setLength(0);
                mKanjiKey.append(s.substring(0, li));
                mComposing.setLength(0);
                processKey(Character.toUpperCase(last));
            } else {
                mKanjiKey.setLength(0);
                mKanjiKey.append(s);
                mComposing.setLength(0);
                conversionStart(mKanjiKey);
            }
        }
    }

    private void reset() {
        mComposing.setLength(0);
        mKanjiKey.setLength(0);
        mOkurigana = null;
        mCandidatesList = null;

        //setCandidatesViewShown()ではComposingTextがflushされるので消す
        InputConnection ic = mService.getCurrentInputConnection();
        if (ic != null) {
            ic.setComposingText("", 1);
        }
        mService.setCandidatesViewShown(false);
//        mMetaKey.clearMetaKeyState();
//        if (mStickyMeta) {mMetaKey.clearMetaKeyState();}
    }

    boolean changeInputMode(int pcode, boolean toKatakana) {
        // 入力モード変更操作．変更したらtrue
        switch (pcode) {
            case 'q':
                if (toKatakana) {
                    changeState(SKKKatakanaState.INSTANCE);
                } else {
                    changeState(SKKHiraganaState.INSTANCE);
                }
                return true;
            case 'l':
                if (mComposing.length() != 1 || mComposing.charAt(0) != 'z') {
                    changeState(SKKASCIIState.INSTANCE);
                    return true;
                } // 「→」を入力するための例外
                break;
            case 'L':
                changeState(SKKZenkakuState.INSTANCE);
                return true;
            case '/':
                if (mComposing.length() == 0) {
                    changeState(SKKAbbrevState.INSTANCE);
                    return true;
                }
                break;
        }

        return false;
    }

    void changeState(SKKState state) {
        mState = state;

        if (!state.isTransient()) {
            reset();
            mService.changeSoftKeyboard(state);
            if (isRegistering) { setComposingTextSKK("", 1); }
            // ComposingTextのflush回避のためreset()で一旦消してるので，登録中はここまで来てからComposingText復活
        }
        if (state == SKKAbbrevState.INSTANCE) {
            setComposingTextSKK("", 1);
            mService.changeSoftKeyboard(state);
        }

        if (state == SKKASCIIState.INSTANCE) {
            mService.hideStatusIcon();
        } else {
            mService.showStatusIcon(state.getIcon());
        }
    }
}
