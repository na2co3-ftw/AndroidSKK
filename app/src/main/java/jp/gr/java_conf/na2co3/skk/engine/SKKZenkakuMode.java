package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// 全角英数モード
public enum SKKZenkakuMode implements SKKMode {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        pcode = SKKUtils.hankaku2zenkaku(pcode);
        context.processText(String.valueOf((char) pcode), false);
    }

    public CharSequence convertText(CharSequence text) {
        return SKKUtils.hankaku2zenkaku(text);
    }

    public SKKMode getToggledKanaMode() { return null; }

    public int getKeyboardType() { return SKKEngine.KEYBOARD_QWERTY; }

    public int getIcon() { return R.drawable.immodeic_full_alphabet; }
}
