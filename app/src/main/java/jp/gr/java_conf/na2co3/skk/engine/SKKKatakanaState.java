package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// カタカナモード
public enum SKKKatakanaState implements SKKState, CommitKana {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
    }

    public void commit(SKKEngine context, String hchr) {
        hchr = SKKUtils.hirakana2katakana(hchr);
        context.commitTextSKK(hchr, 1);
        context.getComposing().setLength(0);
    }

    public void processKey(SKKEngine context, int pcode) {
        if (context.changeInputMode(pcode, false)) { return; }

        SKKHiraganaState.INSTANCE.processKana(context, pcode, this);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        SKKHiraganaState.INSTANCE.processKanaText(context, text, isShifted, this);
    }

    public void afterBackspace(SKKEngine context) {
        SKKHiraganaState.INSTANCE.afterBackspace(context);
    }

    public boolean handleCancel(SKKEngine context) {
        return SKKHiraganaState.INSTANCE.handleCancel(context);
    }

    public boolean isTransient() { return false; }

    public int getIcon() { return R.drawable.immodeic_katakana; }
}
