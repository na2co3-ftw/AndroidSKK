package jp.gr.java_conf.na2co3.skk;

import android.content.Context;
import android.inputmethodservice.Keyboard;

public class SKKKeyboard extends Keyboard {
	private static final int NUM_ROW = 4;

	@Override
	public int getHeight() {
		return getKeyHeight() * NUM_ROW;
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

	public SKKKeyboard(Context context, int xmlLayoutResId) {
		super(context, xmlLayoutResId);
	}
}
