package jp.gr.java_conf.na2co3.skk.engine;

// ASCIIモード
public enum SKKASCIIState implements SKKState {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
        context.commitTextSKK(String.valueOf((char) pcode), 1);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        context.commitTextSKK(text, 1);
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}
    public void afterBackspace(SKKEngine context) {}
    public boolean handleCancel(SKKEngine context) { return false; }
    public boolean finish(SKKEngine context) { return false; }
    public boolean toggleKana(SKKEngine context) { return false; }
    public boolean isTransient() { return false; }
    public CharSequence getComposingText(SKKEngine context) { return null; }
    public int getIcon() { return 0; }
}
