package jp.gr.java_conf.na2co3.skk.engine;

// 通常状態(■モード)
public enum SKKNormalState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        return false;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        if (isShifted) {
            context.changeState(SKKKanjiState.INSTANCE);
            if (text != null) {
                SKKKanjiState.INSTANCE.processText(context, text, initial, false);
            }
        } else {
            if (text != null) {
                context.commitTextSKK(context.convertText(text), 1);
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}
    public void afterBackspace(SKKEngine context) {}

    public boolean handleCancel(SKKEngine context) { return false; }

    public boolean finish(SKKEngine context) { return false; }

    public void toggleKana(SKKEngine context) {
        context.changeMode(context.getToggledKanaMode(), false);
    }

    public CharSequence getComposingText(SKKEngine context) { return null; }

    public int getKeyboardType(SKKEngine context) { return -1; }

    public boolean isTransient() { return false; }
    public boolean isConverting() { return false; }

    public int getIcon() { return 0; }
}
