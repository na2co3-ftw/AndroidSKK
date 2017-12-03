package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;

// ひらがなモード
enum SKKHiraganaMode implements SKKMode {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        context.processRomaji(pcode);
    }

    public CharSequence convertText(CharSequence text) {
        return text;
    }

    public SKKMode getToggledKanaMode() { return SKKKatakanaMode.INSTANCE; }

    public int getKeyboardType() { return SKKEngine.KEYBOARD_HIRAGANA; }

    public int getIcon() { return R.drawable.immodeic_hiragana; }
}
