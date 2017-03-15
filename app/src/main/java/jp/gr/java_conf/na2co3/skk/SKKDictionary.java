package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

class SKKDictionary {
	static final String BTREE_NAME = "skk_dict";
	String mDicFile;

	RecordManager mRecMan;
	long          mRecID;
	protected BTree mBTree;
	protected boolean isValid;

	SKKDictionary() {
	}

	SKKDictionary(String dic) {
		isValid = true;
		mDicFile = dic;

		try {
			mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
			mRecID = mRecMan.getNamedObject(BTREE_NAME);

			if (mRecID == 0) {
				Log.e("SKK", "Dictionary not found: " + mDicFile);
				isValid = false;
			}

			mBTree = BTree.load(mRecMan, mRecID);
		} catch (Exception e) {
			Log.e("SKK", "Error in opening the dictionary: " + e.toString());
			isValid = false;
		}
	}

	boolean isValid() {
		return isValid;
	}

	List<String> getCandidates(String key) {
		List<String> list = new ArrayList<String>();
		String[] va_array;

		if (key.contains("ゔ")) {
			key = key.replace("ゔ", "う゛");
		}

		SKKUtils.dlog("findValue(): key = " + key);

		String value = null;
		try {
			value = (String)mBTree.find(key);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (value == null) return null;

		va_array = value.split("/");
		SKKUtils.dlog("dic: " + mDicFile + " " + value);
		SKKUtils.dlog("length = " + va_array.length);

		if (va_array.length <= 0) {
			Log.e("SKK", "Invalid value found: Key=" + key + " value=" + value);
			return null;
		}

		// va_array[0]は常に空文字列なので1から始める
		for (int i=1; i<va_array.length; i++) {
			list.add(va_array[i]);
		}

		return list;
	}

	List<String> findKeys(String key) {
		List<String> list = new ArrayList<String>();
		Tuple         tuple = new Tuple();
		TupleBrowser  browser;
		String str = null;

		if (key.contains("ゔ")) {
			key = key.replace("ゔ", "う゛");
		}

		try {
			browser = mBTree.browse(key);

			int i=0;
			while (i<5) {
				if (!browser.getNext(tuple)) {break;}
				str = (String)tuple.getKey();
				if (!str.startsWith(key)) {break;}
				if (SKKUtils.isAlphabet(str.charAt(str.length()-1)) && !SKKUtils.isAlphabet(str.charAt(0))) {continue;} // 送りありエントリは飛ばす

				list.add(str);
				i++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return list;
	}
}