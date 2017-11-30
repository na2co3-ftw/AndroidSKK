package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// カタカナモード
public enum SKKKatakanaState implements SKKState {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        SKKHiraganaState.INSTANCE.processKey(context, pcode);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        if (text != null) {
            switch (text) {
                case "q":
                    context.changeState(SKKHiraganaState.INSTANCE);
                    return;
                case "l":
                    if (isShifted) {
                        context.changeState(SKKZenkakuState.INSTANCE);
                    } else {
                        context.changeState(SKKASCIIState.INSTANCE);
                    }
                    return;
                case "/":
                    context.changeState(SKKAbbrevState.INSTANCE);
                    return;
            }
        }

        if (isShifted) {
            context.changeState(SKKKanjiState.INSTANCE);
            if (text != null) {
                SKKKanjiState.INSTANCE.processText(context, text, false);
            }
        } else {
            if (text != null) {
                context.commitTextSKK(SKKUtils.hirakana2katakana(text), 1);
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        SKKHiraganaState.INSTANCE.afterBackspace(context);
    }

    public boolean handleCancel(SKKEngine context) {
        return SKKHiraganaState.INSTANCE.handleCancel(context);
    }

    public boolean finish(SKKEngine context) { return false; }

    public boolean toggleKana(SKKEngine context) { return false; }

    public CharSequence getComposingText(SKKEngine context) {
        return context.getComposing();
    }

    public boolean isTransient() { return false; }

    public int getIcon() { return R.drawable.immodeic_katakana; }
}
