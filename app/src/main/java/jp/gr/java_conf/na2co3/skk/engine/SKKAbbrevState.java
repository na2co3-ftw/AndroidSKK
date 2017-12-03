package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// Abbrevモード(▽モード)
enum SKKAbbrevState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        StringBuilder convKey = context.getConvKey();

        // スペースで変換するかそのままComposingに積む
        if (pcode == ' ') {
            if (convKey.length() != 0) {
                context.abbrevConversionStart();
            }
        } else if (pcode == '.') {
            context.pickCurrentSuggestion();
        } else if (pcode == -1010) {
            // 全角変換
            context.commitTextSKK(SKKUtils.hankaku2zenkaku(convKey), 1);
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            convKey.append((char) pcode);
            context.updateSuggestions();
        }
        return true;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        context.getConvKey().append(text);
        context.updateSuggestions();
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        SKKKanjiState.INSTANCE.afterBackspace(context);
    }

    public boolean handleCancel(SKKEngine context) {
        return SKKKanjiState.INSTANCE.handleCancel(context);
    }

    public boolean finish(SKKEngine context) {
        context.commitTextSKK(context.getConvKey(), 1);
        return true;
    }

    public void toggleKana(SKKEngine context) {}

    public CharSequence getComposingText(SKKEngine context) {
        return context.getConvKey();
    }

    public int getKeyboardType(SKKEngine context) { return SKKEngine.KEYBOARD_ABBREV; }

    public boolean isTransient() { return true; }
    public boolean isConverting() { return false; }

    public int getIcon() { return R.drawable.immodeic_eng2jp; }
}
