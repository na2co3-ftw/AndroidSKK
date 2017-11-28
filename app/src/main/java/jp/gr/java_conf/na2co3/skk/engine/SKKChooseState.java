package jp.gr.java_conf.na2co3.skk.engine;

// 変換候補選択中(▼モード)
public enum SKKChooseState implements SKKState {
    INSTANCE;

    public void handleKanaKey(SKKEngine context) {
        context.pickCurrentCandidate();
        if (context.getToggleKanaKey()) {
            context.changeState(SKKASCIIState.INSTANCE);
        } else {
            context.changeState(SKKHiraganaState.INSTANCE);
        }
    }

    public void processKey(SKKEngine context, int pcode) {
        StringBuilder kanjiKey = context.getKanjiKey();

        switch (pcode) {
            case ' ':
                context.chooseAdjacentCandidate(true);
                break;
            case '>':
                // 接尾辞入力
                context.pickCurrentCandidate();
                context.changeState(SKKKanjiState.INSTANCE);
                kanjiKey.append('>');
                break;
            case 'x':
                context.chooseAdjacentCandidate(false);
                break;
            case 'l':
                // 暗黙の確定
                context.pickCurrentCandidate();
                context.changeState(SKKASCIIState.INSTANCE);
            default:
                // 暗黙の確定
                context.pickCurrentCandidate();
                SKKHiraganaState.INSTANCE.processKey(context, pcode);
                break;
        }
    }

    public void processText(SKKEngine context, String text, boolean isShifted) {
        // 暗黙の確定
        context.pickCurrentCandidate();
        SKKHiraganaState.INSTANCE.processText(context, text, isShifted);
    }

    public void onFinishRomaji(SKKEngine context) {}

    public void beforeBackspace(SKKEngine context) {
        if (context.getOkurigana() != null) {
            context.getKanjiKey().append(context.getOkurigana());
            context.setOkurigana(null);
            context.setOkuriConsonant(null);
        }
    }

    public void afterBackspace(SKKEngine context) {
        if (context.getKanjiKey().length() == 0) {
            context.changeState(SKKHiraganaState.INSTANCE);
        } else {
            if (context.getComposing().length() > 0) { // Abbrevモード
                context.changeState(SKKAbbrevState.INSTANCE);
                context.updateSuggestions(context.getComposing().toString());
            } else { // 漢字変換中
                context.changeState(SKKKanjiState.INSTANCE);
                context.updateSuggestions(context.getKanjiKey().toString());
            }
        }
    }

    public boolean handleCancel(SKKEngine context) {
        beforeBackspace(context);
        afterBackspace(context);
        return true;
    }

    public CharSequence getComposingText(SKKEngine context) {
        return context.getCurrentCandidate();
    }

    public boolean isTransient() { return true; }

    public int getIcon() { return 0; }
}
