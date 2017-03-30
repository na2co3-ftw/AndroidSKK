package jp.deadend.noname.skk.engine;

// ASCIIモード
public enum SKKASCIIState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        context.changeState(SKKHiraganaState.INSTANCE);
    }

    public void processKey(SKKEngine context, int pcode) {
        context.commitTextSKK(String.valueOf((char) pcode), 1);
    }

    public void afterBackspace(SKKEngine context) {}
    public boolean handleCancel(SKKEngine context) { return false; }
    public boolean isTransient() { return false; }
    public int getIcon() { return 0; }
}
