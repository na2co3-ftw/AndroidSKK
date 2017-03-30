package jp.deadend.noname.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;

class SKKKeyboard extends Keyboard {
    private int mNumRow;

    @Override
    public int getHeight() {
        return getKeyHeight() * mNumRow;
    }

    void changeKeyHeight(int px) {
        int y = 0;
        int rowNo = 0;
        for (Keyboard.Key key : getKeys()) {
            key.height = px;
            if (key.y != y) {
                y = key.y;
                rowNo++;
            }
            key.y = px*rowNo;
        }
        setKeyHeight(px);
        getNearestKeys(0, 0);
        //somehow adding this fixed a weird bug where bottom row keys could not be pressed if keyboard height is too tall.. from the Keyboard source code seems like calling this will recalculate some values used in keypress detection calculation
    }

    SKKKeyboard(Context context, int xmlLayoutResId, int numRow) {
        super(context, xmlLayoutResId);
        mNumRow = numRow;
    }
}
