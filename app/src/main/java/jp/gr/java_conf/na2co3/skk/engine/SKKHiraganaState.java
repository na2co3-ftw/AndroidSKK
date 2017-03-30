package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

interface CommitKana {
    void commit(SKKEngine context, String hchr);
}

// ひらがなモード
public enum SKKHiraganaState implements SKKState, CommitKana {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        if (context.getToggleKanaKey()) {
            context.changeState(SKKASCIIState.INSTANCE);
        }
    }

    public void commit(SKKEngine context, String hchr) {
        context.commitTextSKK(hchr, 1);
        context.getComposing().setLength(0);
    }

    void processKana(SKKEngine context, int pcode, CommitKana callback) {
        StringBuilder composing = context.getComposing();

        String hchr; // かな1単位ぶん

        // シフトキーの状態をチェック
        boolean isUpper = Character.isUpperCase(pcode);
        if (isUpper) { // ローマ字変換のために小文字に戻す
            pcode = Character.toLowerCase(pcode);
        }

        if (composing.length() == 1) {
            hchr = RomajiConverter.INSTANCE.checkSpecialConsonants(composing.charAt(0), pcode);
            if (hchr != null) {
                callback.commit(context, hchr);
            }
        }
        if (isUpper) {
            // 漢字変換候補入力の開始。KanjiModeへの移行
            if (composing.length() > 0) {
                context.commitTextSKK(composing, 1);
                composing.setLength(0);
            }
            context.changeState(SKKKanjiState.INSTANCE);
            SKKKanjiState.INSTANCE.processKey(context, pcode);
        } else {
            composing.append((char) pcode);
            // 全角にする記号ならば全角，そうでなければローマ字変換
            hchr = context.getZenkakuSeparator(composing.toString());
            if (hchr == null) {
                hchr = RomajiConverter.INSTANCE.convert(composing.toString());
            }

            if (hchr != null) { // 確定できるものがあれば確定
                callback.commit(context, hchr);
            } else { // アルファベットならComposingに積む
                if (SKKUtils.isAlphabet(pcode)) {
                    context.setComposingTextSKK(composing, 1);
                } else {
                    context.commitTextSKK(composing, 1);
                    composing.setLength(0);
                }
            }
        }
    }

    public void processKey(SKKEngine context, int pcode) {
        if (context.changeInputMode(pcode, true)) { return; }
        processKana(context, pcode, this);
    }

    public void afterBackspace(SKKEngine context) {
        context.setComposingTextSKK(context.getComposing(), 1);
    }

    public boolean handleCancel(SKKEngine context) {
        if (context.isRegistering()) {
            context.cancelRegister();
            return true;
        } else {
            return context.reConversion();
        }
    }

    public boolean isTransient() { return false; }

    public int getIcon() { return R.drawable.immodeic_hiragana; }
}
