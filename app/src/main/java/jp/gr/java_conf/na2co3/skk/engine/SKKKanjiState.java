package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 漢字変換のためのひらがな入力中(▽モード)
public enum SKKKanjiState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {}

    public void processKey(SKKEngine context, int pcode) {
        // シフトキーの状態をチェック
        boolean isUpper = Character.isUpperCase(pcode);
        if (isUpper) { // ローマ字変換のために小文字に戻す
            pcode = Character.toLowerCase(pcode);
        }

        StringBuilder composing = context.getComposing();
        StringBuilder kanjiKey = context.getKanjiKey();
        String hchr; // かな1単位ぶん

        if (composing.length() == 1) {
            hchr = RomajiConverter.INSTANCE.checkSpecialConsonants(composing.charAt(0), pcode);
            if (hchr != null) {
                kanjiKey.append(hchr);
                context.setComposingTextSKK(kanjiKey, 1);
                composing.setLength(0);
            }
        }
        if (pcode == 'q') {
            // カタカナ変換
            if (kanjiKey.length() > 0) {
                String str = SKKUtils.hirakana2katakana(kanjiKey.toString());
                context.commitTextSKK(str, 1);
            }
            context.changeState(SKKHiraganaState.INSTANCE);
        } else if (pcode == ' ' || pcode == '>') {
            // 変換開始
            // 最後に単体の'n'で終わっている場合、'ん'に変換
            if (composing.length() == 1 && composing.charAt(0) == 'n') {
                kanjiKey.append('ん');
                context.setComposingTextSKK(kanjiKey, 1);
            }
            if (pcode == '>') {
                // 接頭辞入力
                kanjiKey.append('>');
            }
            composing.setLength(0);
            context.conversionStart(kanjiKey);
        } else if (pcode == '.') {
            context.pickCurrentSuggestion();
        } else if (isUpper && kanjiKey.length() > 0) {
            // 送り仮名開始
            // 最初の平仮名はついシフトキーを押しっぱなしにしてしまうた
            // め、kanjiKeyの長さをチェックkanjiKeyの長さが0の時はシフトが
            // 押されていなかったことにして下方へ継続させる
            kanjiKey.append((char) pcode); //送りありの場合子音文字追加
            composing.setLength(0);
            if (SKKUtils.isVowel(pcode)) { // 母音なら送り仮名決定，変換
                context.setOkurigana(RomajiConverter.INSTANCE.convert(String.valueOf((char) pcode)));
                context.conversionStart(kanjiKey);
            } else { // それ以外は送り仮名モード
                composing.append((char) pcode);
                context.setComposingTextSKK(SKKUtils.createTrimmedBuilder(kanjiKey).append('*').append((char)pcode), 1);
                context.changeState(SKKOkuriganaState.INSTANCE);
            }
        } else {
            // 未確定
            composing.append((char) pcode);
            hchr = context.getZenkakuSeparator(composing.toString());
            if (hchr == null) {
                hchr = RomajiConverter.INSTANCE.convert(composing.toString());
            }

            if (hchr != null) {
                composing.setLength(0);
                kanjiKey.append(hchr);
                context.setComposingTextSKK(kanjiKey, 1);
            } else {
                context.setComposingTextSKK(kanjiKey.toString() + composing.toString(), 1);
            }
            context.updateSuggestions(kanjiKey.toString());
        }
    }

    public void afterBackspace(SKKEngine context) {
        StringBuilder kanjiKey = context.getKanjiKey();
        StringBuilder composing = context.getComposing();

        if (kanjiKey.length() == 0 && composing.length() == 0) {
            context.changeState(SKKHiraganaState.INSTANCE);
        } else {
            context.setComposingTextSKK(kanjiKey.toString() + composing.toString(), 1);
            context.updateSuggestions(kanjiKey.toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
        return true;
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
