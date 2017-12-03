package jp.gr.java_conf.na2co3.skk.engine;

// Abbrev変換候補選択中(▼モード)
enum SKKAbbrevChooseState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        return SKKChooseState.INSTANCE.processKey(context, pcode);
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        SKKChooseState.INSTANCE.processText(context, text, initial, isShifted);
    }

    public void onFinishRomaji(SKKEngine context) {}

    public void beforeBackspace(SKKEngine context) {
        SKKChooseState.INSTANCE.beforeBackspace(context);
    }

    public void afterBackspace(SKKEngine context) {
        if (context.getConvKey().length() == 0) {
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            context.changeState(SKKAbbrevState.INSTANCE);
            context.updateSuggestions();
        }
    }

    public boolean handleCancel(SKKEngine context) {
        beforeBackspace(context);
        afterBackspace(context);
        return true;
    }

    public boolean finish(SKKEngine context) {
        return SKKChooseState.INSTANCE.finish(context);
    }

    public void toggleKana(SKKEngine context) {
        SKKChooseState.INSTANCE.toggleKana(context);
    }

    public CharSequence getComposingText(SKKEngine context) {
        return SKKChooseState.INSTANCE.getComposingText(context);
    }

    public int getKeyboardType(SKKEngine context) { return SKKEngine.KEYBOARD_ABBREV; }

    public boolean isTransient() { return true; }
    public boolean isConverting() { return true; }

    public int getIcon() { return 0; }
}
