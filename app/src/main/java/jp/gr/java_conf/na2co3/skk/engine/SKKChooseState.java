package jp.gr.java_conf.na2co3.skk.engine;

// 変換候補選択中(▼モード)
public enum SKKChooseState implements SKKState {
    INSTANCE;

    public boolean processKey(SKKEngine context, int pcode) {
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
            default:
                // 暗黙の確定
                context.pickCurrentCandidate();
                context.processKey(pcode);
                break;
        }
        return true;
    }

    public boolean processRomajiExtension(SKKEngine context, String text, boolean isShifted) {
        return false;
    }

    public void processText(SKKEngine context, String text, char initial, boolean isShifted) {
        // 暗黙の確定
        context.pickCurrentCandidate();
        SKKNormalState.INSTANCE.processText(context, text, initial, isShifted);
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
            context.changeState(SKKNormalState.INSTANCE);
        } else {
            context.changeState(SKKKanjiState.INSTANCE);
            context.updateSuggestions(context.getKanjiKey().toString());
        }
    }

    public boolean handleCancel(SKKEngine context) {
        beforeBackspace(context);
        afterBackspace(context);
        return true;
    }

    public boolean finish(SKKEngine context) {
        context.pickCurrentCandidate();
        return true;
    }

    public void toggleKana(SKKEngine context) {
        context.pickCurrentCandidate();
        SKKNormalState.INSTANCE.toggleKana(context);
    }

    public CharSequence getComposingText(SKKEngine context) {
        return context.convertText(context.getCurrentCandidate());
    }

    public int getKeyboardType(SKKEngine context) { return -1; }

    public boolean isTransient() { return true; }
    public boolean isConverting() { return true; }

    public int getIcon() { return 0; }
}
