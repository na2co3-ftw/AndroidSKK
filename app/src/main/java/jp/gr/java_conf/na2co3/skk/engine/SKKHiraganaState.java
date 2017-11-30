package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;

// ひらがなモード
public enum SKKHiraganaState implements SKKState {
    INSTANCE;

    public void processKey(SKKEngine context, int pcode) {
       context.processRomaji(pcode);
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        if (text != null) {
            switch (text) {
                case "q":
                    context.changeState(SKKKatakanaState.INSTANCE);
                    return;
                case "l":
                    if (isShifted) {
                        context.changeState(SKKZenkakuState.INSTANCE);
                    } else {
                        context.changeState(SKKASCIIState.INSTANCE);
                    }
                    return;
                case "/":
                    context.changeState(SKKAbbrevState.INSTANCE);
                    return;
            }
        }

        if (isShifted) {
            context.changeState(SKKKanjiState.INSTANCE);
            if (text != null) {
                SKKKanjiState.INSTANCE.processText(context, text, false);
            }
        } else {
            if (text != null) {
                context.commitTextSKK(text, 1);
            }
        }
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}
    public void afterBackspace(SKKEngine context) {}

    public boolean handleCancel(SKKEngine context) { return false; }

    public boolean finish(SKKEngine context) { return false; }

    public boolean toggleKana(SKKEngine context) { return false; }

    public CharSequence getComposingText(SKKEngine context) {
        return context.getComposing();
    }

    public int getKeyboardType(SKKEngine context) { return SKKEngine.KEYBOARD_HIRAGANA; }

    public boolean isTransient() { return false; }

    public int getIcon() { return R.drawable.immodeic_hiragana; }
}
