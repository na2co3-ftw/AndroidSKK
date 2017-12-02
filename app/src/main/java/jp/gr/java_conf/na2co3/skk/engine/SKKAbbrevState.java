package jp.gr.java_conf.na2co3.skk.engine;

import jp.gr.java_conf.na2co3.skk.R;
import jp.gr.java_conf.na2co3.skk.SKKUtils;

// Abbrevモード(▽モード)
public enum SKKAbbrevState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
        StringBuilder composing = context.getComposing();
        StringBuilder kanjiKey = context.getKanjiKey();

        // スペースで変換するかそのままComposingに積む
        if (pcode == ' ') {
            if (composing.length() != 0) {
                kanjiKey.setLength(0);
                kanjiKey.append(composing);
                context.conversionStart(kanjiKey);
            }
        } else if (pcode == '.') {
            context.pickCurrentSuggestion();
        } else if (pcode == -1010) {
            // 全角変換
            context.commitTextSKK(SKKUtils.hankaku2zenkaku(composing), 1);
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            composing.append((char) pcode);
            context.updateSuggestions(composing.toString());
        }
        return true;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        StringBuilder composing = context.getComposing();
        composing.append(text);
        context.updateSuggestions(composing.toString());
    }

    public void onFinishRomaji(SKKEngine context) {}
    public void beforeBackspace(SKKEngine context) {}

    public void afterBackspace(SKKEngine context) {
        StringBuilder composing = context.getComposing();

        if (composing.length() == 0) {
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            context.updateSuggestions(composing.toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        context.changeState(SKKNormalState.INSTANCE);
        return true;
    }

    public boolean finish(SKKEngine context) {
        StringBuilder composing = context.getComposing();
        if (composing.length() > 0) {
            context.commitTextSKK(composing, 1);
            composing.setLength(0);
        }
        return true;
    }

    public void toggleKana(SKKEngine context) {}

    public CharSequence getComposingText(SKKEngine context) {
        return context.getComposing();
    }

    public int getKeyboardType(SKKEngine context) { return SKKEngine.KEYBOARD_ABBREV; }

    public boolean isTransient() { return true; }

    public int getIcon() { return R.drawable.immodeic_eng2jp; }
}
