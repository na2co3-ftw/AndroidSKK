package jp.deadend.noname.skk.engine

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.UnderlineSpan
import androidx.core.content.res.ResourcesCompat
import jp.deadend.noname.skk.*
import java.util.ArrayDeque

class SKKEngine(
        private val mService: SKKService,
        private var mDicts: List<SKKDictionary>,
        private val mUserDict: SKKUserDictionary
) {
    var state: SKKState = SKKHiraganaState
        private set

    // 候補のリスト．KanjiStateとAbbrevStateでは補完リスト，ChooseStateでは変換候補リストになる
    private var mCandidatesList: List<String>? = null
    private var mCurrentCandidateIndex = 0

    // ひらがなや英単語などの入力途中
    internal val mComposing = StringBuilder()
    // 漢字変換のキー 送りありの場合最後がアルファベット 変換中は不変
    internal val mKanjiKey = StringBuilder()
    // 送りがな 「っ」や「ん」が含まれる場合だけ二文字になる
    internal var mOkurigana: String? = null

    // 全角で入力する記号リスト
    private val mZenkakuSeparatorMap = mutableMapOf(
        "-" to "ー", "!" to "！", "?" to "？", "~" to "〜",
        "[" to "「", "]" to "」", "(" to "(", ")" to ")",
        "・" to "・", "…" to "…"
    )
    val isRegistering: Boolean
        get() = !mRegistrationStack.isEmpty()
    internal val toggleKanaKey: Boolean
        get() = skkPrefs.toggleKanaKey

    // 単語登録のための情報
    private val mRegistrationStack = ArrayDeque<RegistrationInfo>()
    private class RegistrationInfo(val key: String, val okurigana: String?) {
        val entry = StringBuilder()
    }

    // 再変換のための情報
    private class ConversionInfo(
            val candidate: String,
            val list: List<String>,
            val index: Int,
            val kanjiKey: String,
            val okurigana: String?
    )
    private var mLastConversion: ConversionInfo? = null

    private val mColorEditing: Int
    private val mColorConverting: Int

    init {
        setZenkakuPunctuationMarks("en")

        val r = mService.resources
        mColorEditing = ResourcesCompat.getColor(r, R.color.input_editing, null)
        mColorConverting = ResourcesCompat.getColor(r, R.color.input_converting, null)
    }

    fun reopenDictionaries(dics: List<SKKDictionary>) {
        for (dic in mDicts) { dic.close() }
        mDicts = dics
    }

    fun setZenkakuPunctuationMarks(type: String) {
        when (type) {
            "en" -> {
                mZenkakuSeparatorMap["."] = "．"
                mZenkakuSeparatorMap[","] = "，"
            }
            "jp" -> {
                mZenkakuSeparatorMap["."] = "。"
                mZenkakuSeparatorMap[","] = "、"
            }
            "jp_en" -> {
                mZenkakuSeparatorMap["."] = "。"
                mZenkakuSeparatorMap[","] = "，"
            }
            else -> {
                mZenkakuSeparatorMap["."] = "．"
                mZenkakuSeparatorMap[","] = "，"
            }
        }
    }

    fun commitUserDictChanges() {
        mUserDict.commitChanges()
    }

    fun processKey(pcode: Int) {
        if (pcode == ' '.code && !state.isTransient && mComposing.isEmpty()) {
            val regInfo = mRegistrationStack.peekFirst()
            if (regInfo != null && regInfo.entry.isEmpty()) {
                return
            }
        }
        state.processKey(this, pcode)
    }

    fun handleKanaKey() {
        state.handleKanaKey(this)
    }

    fun handleBackKey(): Boolean {
        if (!mRegistrationStack.isEmpty()) {
            mRegistrationStack.removeFirst()
            mService.onFinishRegister()
        }

        if (state.isTransient) {
            changeState(SKKHiraganaState)
            return true
        } else if (!mRegistrationStack.isEmpty()) {
            reset()
            return true
        }

        return false
    }

    fun handleEnter(): Boolean {
        when (state) {
            SKKChooseState, SKKNarrowingState -> pickCandidate(mCurrentCandidateIndex)
            SKKKanjiState, SKKOkuriganaState, SKKAbbrevState -> {
                commitTextSKK(mKanjiKey)
                changeState(SKKHiraganaState)
            }
            else -> {
                if (mComposing.isEmpty()) {
                    if (!mRegistrationStack.isEmpty()) {
                        registerWord()
                    } else {
                        return false
                    }
                } else {
                    commitTextSKK(mComposing)
                    mComposing.setLength(0)
                }
            }
        }

        return true
    }

    fun handleBackspace(): Boolean {
        if (state == SKKNarrowingState || state == SKKChooseState) {
            state.afterBackspace(this)
            return true
        }

        val clen = mComposing.length
        val klen = mKanjiKey.length

        // 変換中のものがない場合
        if (clen == 0 && klen == 0) {
            if (state == SKKKanjiState || state == SKKAbbrevState) {
                changeState(SKKHiraganaState)
                return true
            }
            val firstEntry = mRegistrationStack.peekFirst()?.entry
            if (firstEntry != null) {
                if (firstEntry.isNotEmpty()) {
                    firstEntry.deleteCharAt(firstEntry.length - 1)
                    setComposingTextSKK("", 1)
                } else {
                    cancelRegister()
                    return true
                }
            } else {
                return state.isTransient
            }
        }

        if (clen > 0) {
            mComposing.deleteCharAt(clen - 1)
        } else if (klen > 0) {
            mKanjiKey.deleteCharAt(klen - 1)
        }
        state.afterBackspace(this)

        return true
    }

    fun handleCancel(): Boolean {
        return state.handleCancel(this)
    }

    /**
     * commitTextのラッパー 登録作業中なら登録内容に追加し，表示を更新
     * @param text
     */
    fun commitTextSKK(text: CharSequence) {
        val ic = mService.currentInputConnection ?: return

        val firstEntry = mRegistrationStack.peekFirst()?.entry
        if (firstEntry != null) {
            firstEntry.append(text)
            setComposingTextSKK("", 1)
        } else {
            ic.commitText(text, 1)
        }
    }

    fun resetOnStartInput() {
        mComposing.setLength(0)
        mKanjiKey.setLength(0)
        mOkurigana = null
        mCandidatesList = null
        when {
            state.isTransient -> {
                changeState(SKKHiraganaState)
                mService.showStatusIcon(state.icon)
            }
            state === SKKASCIIState -> mService.hideStatusIcon()
            else -> mService.showStatusIcon(state.icon)
        }

        // onStartInput()では，WebViewのときsetComposingText("", 1)すると落ちるようなのでやめる
    }

    fun chooseAdjacentSuggestion(isForward: Boolean) {
        val candList = mCandidatesList ?: return

        if (isForward) {
            mCurrentCandidateIndex++
        } else {
            mCurrentCandidateIndex--
        }

        // 範囲外になったら反対側へ
        if (mCurrentCandidateIndex > candList.size - 1) {
            mCurrentCandidateIndex = 0
        } else if (mCurrentCandidateIndex < 0) {
            mCurrentCandidateIndex = candList.size - 1
        }

        mService.requestChooseCandidate(mCurrentCandidateIndex)
        mKanjiKey.setLength(0)
        mKanjiKey.append(candList[mCurrentCandidateIndex])
        setComposingTextSKK(mKanjiKey, 1)
    }

    fun chooseAdjacentCandidate(isForward: Boolean) {
        val candList = mCandidatesList ?: return

        if (isForward) {
            mCurrentCandidateIndex++
        } else {
            mCurrentCandidateIndex--
        }

        // 最初の候補より戻ると変換に戻る 最後の候補より進むと登録
        if (mCurrentCandidateIndex > candList.size - 1) {
            if (state === SKKChooseState) {
                registerStart(mKanjiKey.toString())
                return
            } else if (state === SKKNarrowingState) {
                mCurrentCandidateIndex = 0
            }
        } else if (mCurrentCandidateIndex < 0) {
            if (state === SKKChooseState) {
                if (mComposing.isEmpty()) {
                    // KANJIモードに戻る
                    if (mOkurigana != null) {
                        mOkurigana = null
                        mKanjiKey.deleteCharAt(mKanjiKey.length - 1)
                    }
                    changeState(SKKKanjiState)
                    setComposingTextSKK(mKanjiKey, 1)
                    updateSuggestions(mKanjiKey.toString())
                } else {
                    mKanjiKey.setLength(0)
                    changeState(SKKAbbrevState)
                    setComposingTextSKK(mComposing, 1)
                    updateSuggestions(mComposing.toString())
                }

                mCurrentCandidateIndex = 0
            } else if (state === SKKNarrowingState) {
                mCurrentCandidateIndex = candList.size - 1
            }
        }

        mService.requestChooseCandidate(mCurrentCandidateIndex)
        setCurrentCandidateToComposing()
    }

    fun pickCandidateViewManually(index: Int) {
        if (state === SKKChooseState || state === SKKNarrowingState) {
            pickCandidate(index)
        } else if (state === SKKAbbrevState || state === SKKKanjiState) {
            pickSuggestion(index)
        }
    }

    fun prepareToMushroom(clip: String): String {
        val str = if (state === SKKKanjiState || state === SKKAbbrevState) {
            mKanjiKey.toString()
        } else {
            clip
        }

        if (state.isTransient) {
            changeState(SKKHiraganaState)
        } else {
            reset()
            mRegistrationStack.clear()
        }

        return str
    }

    // 小文字大文字変換，濁音，半濁音に使う
    fun changeLastChar(type: String) {
        when {
            state === SKKKanjiState && mComposing.isEmpty() && mKanjiKey.isNotEmpty() -> {
                val s = mKanjiKey.toString()
                val idx = s.length - 1
                val newLastChar = RomajiConverter.convertLastChar(s.substring(idx), type) ?: return

                mKanjiKey.deleteCharAt(idx)
                mKanjiKey.append(newLastChar)
                setComposingTextSKK(mKanjiKey, 1)
                updateSuggestions(mKanjiKey.toString())
            }
            state === SKKNarrowingState && mComposing.isEmpty() && SKKNarrowingState.mHint.isNotEmpty() -> {
                val hint = SKKNarrowingState.mHint
                val idx = hint.length - 1
                val newLastChar = RomajiConverter.convertLastChar(hint.substring(idx), type) ?: return

                hint.deleteCharAt(idx)
                hint.append(newLastChar)
                narrowCandidates(hint.toString())
            }
            state === SKKChooseState -> {
                val okuri = mOkurigana ?: return
                val newOkuri = RomajiConverter.convertLastChar(okuri, type) ?: return

                val consonantForVoiced = RomajiConverter.getConsonantForVoiced(newOkuri)
                if (consonantForVoiced != null) {
                    mKanjiKey.deleteCharAt(mKanjiKey.length - 1)
                    mKanjiKey.append(consonantForVoiced)
                }
                mOkurigana = newOkuri
                conversionStart(mKanjiKey) //変換やりなおし
            }
            mComposing.isEmpty() && mKanjiKey.isEmpty() -> {
                val regInfo = mRegistrationStack.peekFirst()
                if (regInfo != null && regInfo.entry.isEmpty()) {
                    val okuri = regInfo.okurigana ?: return
                    val newOkuri = RomajiConverter.convertLastChar(okuri, type) ?: return

                    mRegistrationStack.removeFirst()
                    mService.onFinishRegister()

                    mKanjiKey.setLength(0)
                    mKanjiKey.append(regInfo.key)
                    val consonantForVoiced = RomajiConverter.getConsonantForVoiced(newOkuri)
                    if (consonantForVoiced != null) {
                        mKanjiKey.deleteCharAt(mKanjiKey.length - 1)
                        mKanjiKey.append(consonantForVoiced)
                    }
                    mOkurigana = newOkuri
                    conversionStart(mKanjiKey) //変換やりなおし
                    return
                }

                val ic = mService.currentInputConnection ?: return
                val cs = ic.getTextBeforeCursor(1, 0) ?: return
                val newLastChar = RomajiConverter.convertLastChar(cs.toString(), type) ?: return

                val firstEntry = regInfo?.entry
                if (firstEntry != null) {
                    firstEntry.deleteCharAt(firstEntry.length - 1)
                    firstEntry.append(newLastChar)
                    setComposingTextSKK("", 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                    ic.commitText(newLastChar, 1)
                }
            }
        }
    }

    internal fun getZenkakuSeparator(key: String) = mZenkakuSeparatorMap[key]

    /**
     * setComposingTextのラッパー 変換モードマーク等を追加する
     * @param text
     * @param newCursorPosition
     */
    internal fun setComposingTextSKK(text: CharSequence, newCursorPosition: Int) {
        val ic = mService.currentInputConnection ?: return

        val ct = SpannableStringBuilder()

        if (!mRegistrationStack.isEmpty()) {
            val depth = mRegistrationStack.size
            repeat(depth) { ct.append("[") }
            ct.append("登録")
            repeat(depth) { ct.append("]") }

            val regInfo = mRegistrationStack.peekFirst()
            if (regInfo != null) {
                if (regInfo.okurigana == null) {
                    ct.append(regInfo.key)
                } else {
                    ct.append(regInfo.key.substring(0, regInfo.key.length - 1))
                    ct.append("*")
                    ct.append(regInfo.okurigana)
                }
                ct.append("：")
                ct.setSpan(BackgroundColorSpan(mColorConverting), 0, ct.length, Spanned.SPAN_COMPOSING)

                ct.append(regInfo.entry)
            }
        }

        var bg: BackgroundColorSpan? = null
        val bgStart = ct.length
        if (state === SKKAbbrevState || state === SKKKanjiState || state === SKKOkuriganaState) {
            bg = BackgroundColorSpan(mColorEditing)
            ct.append("▽")
        } else if (state === SKKChooseState || state === SKKNarrowingState) {
            bg = BackgroundColorSpan(mColorConverting)
            ct.append("▼")
        }
        ct.append(text)
        if (bg != null) {
            ct.setSpan(bg, bgStart, ct.length, Spanned.SPAN_COMPOSING)
        }

        if (state === SKKNarrowingState) {
            ct.append(" hint: ", BackgroundColorSpan(mColorConverting), Spanned.SPAN_COMPOSING)

            val hintStart = ct.length
            ct.append(SKKNarrowingState.mHint, mComposing)
            ct.setSpan(BackgroundColorSpan(mColorEditing), hintStart, ct.length, Spanned.SPAN_COMPOSING)
        }

        ct.setSpan(UnderlineSpan(), 0, ct.length, Spanned.SPAN_COMPOSING)
        ic.setComposingText(ct, newCursorPosition)
    }

    /***
     * 変換スタート
     * 送りありの場合，事前に送りがなをmOkuriganaにセットしておく
     * @param key 辞書のキー 送りありの場合最後はアルファベット
     */
    internal fun conversionStart(key: StringBuilder) {
        val str = key.toString()

        changeState(SKKChooseState)

        val list = findCandidates(str)
        if (list == null) {
            registerStart(str)
            return
        }

        mCandidatesList = list
        mCurrentCandidateIndex = 0
        mService.setCandidates(list)
        setCurrentCandidateToComposing()
    }

    internal fun narrowCandidates(hint: String) {
        val candidates = SKKNarrowingState.mOriginalCandidates ?: mCandidatesList ?: return
        if (SKKNarrowingState.mOriginalCandidates == null) {
            SKKNarrowingState.mOriginalCandidates = mCandidatesList
        }
        val hintKanjis = findCandidates(hint)
                ?.joinToString(
                    separator="",
                    transform={ processConcatAndEscape(removeAnnotation(it)) }
                ) ?: ""

        val narrowed = candidates.filter { str -> str.any { ch -> hintKanjis.contains(ch) } }
        if (narrowed.isNotEmpty()) {
            mCandidatesList = narrowed
            mCurrentCandidateIndex = 0
            mService.setCandidates(narrowed)
        }
        setCurrentCandidateToComposing()
    }

    internal fun reConversion(): Boolean {
        val lastConv = mLastConversion ?: return false

        val s = lastConv.candidate
        dlog("last conversion: $s")
        if (mService.prepareReConversion(s)) {
            mUserDict.rollBack()

            changeState(SKKChooseState)

            mComposing.setLength(0)
            mKanjiKey.setLength(0)
            mKanjiKey.append(lastConv.kanjiKey)
            mOkurigana = lastConv.okurigana
            mCandidatesList = lastConv.list
            mCurrentCandidateIndex = lastConv.index
            mService.setCandidates(mCandidatesList)
            mService.requestChooseCandidate(mCurrentCandidateIndex)
            setCurrentCandidateToComposing()

            return true
        }

        return false
    }

    internal fun updateSuggestions(str: String) {
        val list = mutableListOf<String>()

        if (str.isNotEmpty()) {
            for (dic in mDicts) {
                list.addAll(dic.findKeys(str))
            }
            val list2 = mUserDict.findKeys(str)
            for ((idx, s) in list2.withIndex()) {
                //個人辞書のキーを先頭に追加
                list.remove(s)
                list.add(idx, s)
            }
        }

        mCandidatesList = list
        mCurrentCandidateIndex = 0
        mService.setCandidates(list)
    }

    private fun registerStart(str: String) {
        mRegistrationStack.addFirst(RegistrationInfo(str, mOkurigana))
        changeState(SKKHiraganaState)
        //setComposingTextSKK("", 1);

        mService.onStartRegister()
    }

    private fun registerWord() {
        val regInfo = mRegistrationStack.removeFirst()
        if (regInfo.entry.isNotEmpty()) {
            var regEntryStr = regInfo.entry.toString()
            if (regEntryStr.indexOf(';') != -1 || regEntryStr.indexOf('/') != -1) {
                // セミコロンとスラッシュのエスケープ
                regEntryStr = (
                        "(concat \""
                        + regEntryStr.replace(";", "\\073").replace("/", "\\057")
                        + "\")"
                        )
            }
            mUserDict.addEntry(regInfo.key, regEntryStr, regInfo.okurigana)
            mUserDict.commitChanges()
            if (regInfo.okurigana.isNullOrEmpty()) {
                commitTextSKK(regInfo.entry)
            } else {
                commitTextSKK(regInfo.entry.append(regInfo.okurigana))
            }
        }
        reset()
        if (!mRegistrationStack.isEmpty()) setComposingTextSKK("", 1)

        mService.onFinishRegister()
    }

    internal fun cancelRegister() {
        val regInfo = mRegistrationStack.removeFirst()
        mKanjiKey.setLength(0)
        mKanjiKey.append(regInfo.key)
        mOkurigana = regInfo.okurigana
        mComposing.setLength(0)
        mService.onFinishRegister()

        changeState(SKKChooseState)
        val list = findCandidates(regInfo.key)
        if (list == null) {
            SKKChooseState.handleCancel(this)
            return
        }

        mCandidatesList = list
        mCurrentCandidateIndex = list.size - 1
        mService.setCandidates(list)
        mService.requestChooseCandidate(mCurrentCandidateIndex)
        setCurrentCandidateToComposing()
    }

    private fun findCandidates(key: String): List<String>? {
        val list1 = mDicts.mapNotNull { it.getCandidates(key) }
                .fold(listOf()) { acc: Iterable<String>, list: Iterable<String> -> acc.union(list) }
                .toMutableList()

        val entry = mUserDict.getEntry(key)
        val list2 = entry?.candidates

        if (list1.isEmpty() && list2 == null) {
            dlog("Dictoinary: Can't find Kanji for $key")
            return null
        }

        if (list2 != null) {
            var idx = 0
            for (s in list2) {
                when {
                    mOkurigana == null
                     || entry.okuriBlocks.any { it.first == mOkurigana && it.second == s } -> {
                        //個人辞書の候補を先頭に追加
                        list1.remove(s)
                        list1.add(idx, s)
                        idx++
                    }
                    !list1.contains(s) -> { list1.add(s) } //送りがなブロックにマッチしない場合も、無ければ最後に追加
                }
            }
        }

        return list1.ifEmpty { null }
    }

    fun setCurrentCandidateToComposing() {
        val candList = mCandidatesList ?: return
        val candidate = processConcatAndEscape(removeAnnotation(candList[mCurrentCandidateIndex]))
        if (mOkurigana != null) {
            setComposingTextSKK(candidate + mOkurigana, 1)
        } else {
            setComposingTextSKK(candidate, 1)
        }
    }

    internal fun pickCurrentCandidate() {
        pickCandidate(mCurrentCandidateIndex)
    }

    private fun pickCandidate(index: Int) {
        if (state !== SKKChooseState && state !== SKKNarrowingState) return
        val candList = mCandidatesList ?: return
        val candidate = processConcatAndEscape(removeAnnotation(candList[index]))

        mUserDict.addEntry(mKanjiKey.toString(), candList[index], mOkurigana)
        // ユーザー辞書登録時はエスケープや注釈を消さない

        commitTextSKK(candidate)
        val okuri = mOkurigana
        if (okuri != null) {
            commitTextSKK(okuri)
            if (mRegistrationStack.isEmpty()) {
                mLastConversion = ConversionInfo(
                        candidate + okuri, candList, index, mKanjiKey.toString(), okuri
                )
            }
        } else {
            if (mRegistrationStack.isEmpty()) {
                mLastConversion = ConversionInfo(
                        candidate, candList, index, mKanjiKey.toString(), null
                )
            }
        }

        changeState(SKKHiraganaState)
    }

    private fun pickSuggestion(index: Int) {
        val candList = mCandidatesList ?: return
        val s = candList[index]

        if (state === SKKAbbrevState) {
            setComposingTextSKK(s, 1)
            mKanjiKey.setLength(0)
            mKanjiKey.append(s)
            conversionStart(mKanjiKey)
        } else if (state === SKKKanjiState) {
            setComposingTextSKK(s, 1)
            val li = s.length - 1
            val last = s.codePointAt(li)
            if (isAlphabet(last)) {
                mKanjiKey.setLength(0)
                mKanjiKey.append(s.substring(0, li))
                mComposing.setLength(0)
                processKey(Character.toUpperCase(last))
            } else {
                mKanjiKey.setLength(0)
                mKanjiKey.append(s)
                mComposing.setLength(0)
                conversionStart(mKanjiKey)
            }
        }
    }

    private fun reset() {
        mComposing.setLength(0)
        mKanjiKey.setLength(0)
        mOkurigana = null
        mCandidatesList = null
        mService.clearCandidatesView()
        mService.currentInputConnection.setComposingText("", 1)
    }

    internal fun changeInputMode(pcode: Int, toKatakana: Boolean): Boolean {
        // 入力モード変更操作．変更したらtrue
        when (pcode) {
            'q'.code -> {
                if (toKatakana) {
                    changeState(SKKKatakanaState)
                } else {
                    changeState(SKKHiraganaState)
                }
                return true
            }
            'l'.code ->  {
                if (mComposing.length != 1 || mComposing[0] != 'z') {
                    changeState(SKKASCIIState)
                    return true
                }
            } // 「→」を入力するための例外
            'L'.code -> {
                changeState(SKKZenkakuState)
                return true
            }
            '/'.code -> if (mComposing.isEmpty()) {
                changeState(SKKAbbrevState)
                return true
            }
        }

        return false
    }

    internal fun changeState(state: SKKState) {
        this.state = state

        if (!state.isTransient) {
            reset()
            mService.changeSoftKeyboard(state)
            if (!mRegistrationStack.isEmpty()) setComposingTextSKK("", 1)
            // reset()で一旦消してるので， 登録中はここまで来てからComposingText復活
        }

        when (state) {
            SKKAbbrevState -> {
                setComposingTextSKK("", 1)
                mService.changeSoftKeyboard(state)
                mService.showStatusIcon(state.icon)
            }
            SKKASCIIState -> mService.hideStatusIcon()
            SKKNarrowingState -> {
                SKKNarrowingState.mHint.setLength(0)
                SKKNarrowingState.mOriginalCandidates = null
                setCurrentCandidateToComposing()
            }
            else -> mService.showStatusIcon(state.icon)
        }
    }

    companion object {
        const val LAST_CONVERTION_SMALL = "small"
        const val LAST_CONVERTION_ROTATE = "rotate"
    }
}
