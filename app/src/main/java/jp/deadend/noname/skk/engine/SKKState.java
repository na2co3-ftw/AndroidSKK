package jp.deadend.noname.skk.engine;

public interface SKKState {
    void handleKanaKey(SKKEngine context);
    void processKey(SKKEngine context, int pcode);
    void afterBackspace(SKKEngine context);
    boolean handleCancel(SKKEngine context);
    boolean isTransient();
    int getIcon();
}
