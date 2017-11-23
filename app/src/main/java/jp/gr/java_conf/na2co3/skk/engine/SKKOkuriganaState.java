package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 送り仮名入力中(▽モード，*つき)
public enum SKKOkuriganaState implements SKKState {
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
        String okurigana = context.getOkurigana();

        if (composing.length() == 1 || okurigana == null) {
            // 「ん」か「っ」を処理したらここで終わり
            hchr = RomajiConverter.INSTANCE.checkSpecialConsonants(composing.charAt(0), pcode);
            if (hchr != null) {
                context.setOkurigana(hchr);
                context.setComposingTextSKK(SKKUtils.createTrimmedBuilder(kanjiKey).append('*').append(hchr).append((char) pcode), 1);
                composing.setLength(0);
                composing.append((char) pcode);
                return;
            }
        }
        // 送りがなが確定すれば変換，そうでなければComposingに積む
        composing.append((char) pcode);
        hchr = RomajiConverter.INSTANCE.convert(composing.toString());
        if (okurigana != null) { //「ん」か「っ」がある場合
            if (hchr != null) {
                composing.setLength(0);
                context.setOkurigana(okurigana + hchr);
                context.conversionStart(kanjiKey);
            } else {
                context.setComposingTextSKK(SKKUtils.createTrimmedBuilder(kanjiKey).append('*').append(okurigana).append(composing), 1);
            }
        } else {
            if (hchr != null) {
                composing.setLength(0);
                context.setOkurigana(hchr);
                context.conversionStart(kanjiKey);
            } else {
                context.setComposingTextSKK(SKKUtils.createTrimmedBuilder(kanjiKey).append('*').append(composing), 1);
            }
        }
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        StringBuilder composing = context.getComposing();
        StringBuilder kanjiKey = context.getKanjiKey();
        String okurigana = context.getOkurigana();

        if (composing.length() == 1 && composing.charAt(0) == 'n') {
            context.setOkurigana("ん");
            context.setComposingTextSKK(SKKUtils.createTrimmedBuilder(kanjiKey).append('*').append("ん").append(text), 1);
            composing.setLength(0);
            composing.append(text);
            return;
        }
        if (okurigana != null) { //「ん」か「っ」がある場合
            composing.setLength(0);
            context.setOkurigana(okurigana + text);
            context.conversionStart(kanjiKey);
        } else {
            composing.setLength(0);
            context.setOkurigana(text);
            context.conversionStart(kanjiKey);
        }
    }

    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        context.getComposing().setLength(0);
        context.setOkurigana(null);
        context.setComposingTextSKK(context.getKanjiKey(), 1);
        context.changeState(SKKKanjiState.INSTANCE);
    }

    public boolean handleCancel(SKKEngine context) {
        StringBuilder kanjiKey = context.getKanjiKey();
        context.getComposing().setLength(0);
        context.setOkurigana(null);
        kanjiKey.deleteCharAt(kanjiKey.length()-1);
        context.changeState(SKKKanjiState.INSTANCE);
        context.setComposingTextSKK(kanjiKey, 1);

        return true;
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
