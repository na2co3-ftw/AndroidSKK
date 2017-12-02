package jp.gr.java_conf.na2co3.skk.engine;

// ASCIIモード
public enum SKKASCIIMode implements SKKMode {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        context.processText(String.valueOf((char) pcode), false);
    }

    public CharSequence convertText(CharSequence text) {
        return text;
    }

    public SKKMode getToggledKanaMode() { return null; }

    public int getKeyboardType() { return SKKEngine.KEYBOARD_QWERTY; }

    public int getIcon() { return 0; }
}
