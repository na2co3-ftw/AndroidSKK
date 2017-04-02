package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 全角英数モード
public enum SKKZenkakuState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
    }

    public void processKey(SKKEngine context, int pcode) {
        pcode = SKKUtils.hankaku2zenkaku(pcode);
        context.commitTextSKK(String.valueOf((char) pcode), 1);
    }

    public void afterBackspace(SKKEngine context) {}

    public boolean handleCancel(SKKEngine context) {
        return SKKHiraganaState.INSTANCE.handleCancel(context);
    }

    public boolean isTransient() { return false; }

    public int getIcon() { return R.drawable.immodeic_full_alphabet; }
}