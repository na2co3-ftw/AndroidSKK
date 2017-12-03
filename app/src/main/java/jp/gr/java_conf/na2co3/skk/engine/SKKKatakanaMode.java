package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// カタカナモード
enum SKKKatakanaMode implements SKKMode {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        context.processRomaji(pcode);
    }

    public CharSequence convertText(CharSequence text) {
        return SKKUtils.hirakana2katakana(text);
    }

    public SKKMode getToggledKanaMode() { return SKKHiraganaMode.INSTANCE; }

    public int getKeyboardType() { return SKKEngine.KEYBOARD_KATAKANA; }

    public int getIcon() { return R.drawable.immodeic_katakana; }
}
