package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// Abbrevモード(▽モード)
public enum SKKAbbrevState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
    }

    public void processKey(SKKEngine context, int pcode) {
        StringBuilder composing = context.getComposing();
        StringBuilder kanjiKey = context.getKanjiKey();

        // スペースで変換するかそのままComposingに積む
        if (pcode == ' ') {
            if (composing.length() != 0) {
                kanjiKey.setLength(0);
                kanjiKey.append(composing);
                context.conversionStart(kanjiKey);
            }
        } else if (pcode == '.') {
            context.pickCurrentSuggestion();
        } else if (pcode == -1010) {
            // 全角変換
            int len = composing.length();
            StringBuilder buf = new StringBuilder(len);
            for (int i=0; i<len; i++) {
                buf.append((char) SKKUtils.hankaku2zenkaku(composing.charAt(i)));
            }
            context.commitTextSKK(buf, 1);
            context.changeState(SKKHiraganaState.INSTANCE);
        } else {
            composing.append((char) pcode);
            context.setComposingTextSKK(composing, 1);
            context.updateSuggestions(composing.toString());
        }
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        StringBuilder composing = context.getComposing();
        composing.append(text);
        context.setComposingTextSKK(composing, 1);
        context.updateSuggestions(composing.toString());
    }

    public void afterBackspace(SKKEngine context) {
        StringBuilder composing = context.getComposing();

        if (composing.length() == 0) {
            context.changeState(SKKHiraganaState.INSTANCE);
        } else {
            context.setComposingTextSKK(composing, 1);
            context.updateSuggestions(composing.toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
        return true;
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return R.drawable.immodeic_eng2jp; }
}
