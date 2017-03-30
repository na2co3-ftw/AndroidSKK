package jp.deadend.noname.skk.engine;

import jp.deadend.noname.skk.R;
import jp.deadend.noname.skk.SKKUtils;

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
