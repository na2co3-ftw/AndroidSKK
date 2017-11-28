package jp.gr.java_conf.na2co3.skk.engine;

public interface SKKState {
    void handleKanaKey(SKKEngine context);
    void processKey(SKKEngine context, int pcode);
    void processText(SKKEngine context, String text, boolean isShifted);
    void onFinishRomaji(SKKEngine context);
    void beforeBackspace(SKKEngine context);
    void afterBackspace(SKKEngine context);
    boolean handleCancel(SKKEngine context);
    CharSequence getComposingText(SKKEngine context);
    boolean isTransient();
    int getIcon();
}
