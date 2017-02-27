package jp.deadend.noname.skk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;

class SKKUserDictionary extends SKKDictionary {
	class Entry {
		public Entry(List<String> cd, List<List<String>> okr) {
			candidates = cd;
			okuri_blocks = okr;
		}

		List<String> candidates = null;
		List<List<String>> okuri_blocks = null;
	}

	private String mOldKey = null;
	private String mOldValue = null;

	SKKUserDictionary(String dic) {
		isValid = true;
		mDicFile = dic;

		try {
			mRecMan = RecordManagerFactory.createRecordManager(mDicFile);
			mRecID = mRecMan.getNamedObject(BTREE_NAME);

			if (mRecID == 0) {
				mBTree = BTree.createInstance(mRecMan, new StringComparator());
				mRecMan.setNamedObject(BTREE_NAME, mBTree.getRecid());
				mRecMan.commit();
				SKKUtils.dlog("New user dictionary created");
			} else {
				mBTree = BTree.load(mRecMan, mRecID);
			}
		} catch (Exception e) {
			Log.e("SKK", "Error in opening the user dic: " + e.toString());
			isValid = false;
		}
	}

	@Override
	List<String> getCandidates(String key) {
		Log.e("SKK", "Don't use SKKUserDictionary#getCandidates()");
		return null;
	}

	Entry getEntry(String key) {
		List<String> cd = new ArrayList<String>();
		List<List<String>> okr = new ArrayList<List<String>>();
		String[] va_array;

		SKKUtils.dlog("userdict getEntries(): key = " + key);

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
			// 送りがなブロックが見つかったら中止
			if (va_array[i].startsWith("[")) break;
			cd.add(va_array[i]);
		}

		if (value.contains("[") && value.contains("]")) {
			// 送りがなブロック
			va_array = value.split("[\\[\\]]");
			List<String> tmp_okr = null;
			String[] va_array2 = null;
			// va_array[0]は変換候補なので1から始める
			for (int i=1; i<va_array.length; i++) {
				if (va_array[i].equals("/")) {continue;}
				va_array2 = va_array[i].split("/");
				tmp_okr = new ArrayList<String>();
				for (int j=0; j<va_array2.length; j++) {
					tmp_okr.add(va_array2[j]);
				}
				okr.add(tmp_okr);
			}
		}

		return new Entry(cd, okr);
	}

	void addEntry(String key, String val, String okuri) {
		mOldKey = key;
		StringBuilder new_val = new StringBuilder();
		Entry entry = (Entry)getEntry(key);

		if (entry == null) {
			new_val.append("/");
			new_val.append(val);
			new_val.append("/");
			if (okuri != null) {
				new_val.append("[");
				new_val.append(okuri);
				new_val.append("/");
				new_val.append(val);
				new_val.append("/]/");
			}
			mOldValue = null;
		} else {
			List<String> cands = entry.candidates;
			cands.remove(val);
			cands.add(0, val);

			List<List<String>> okrs = entry.okuri_blocks;
			if (okuri != null) {
				boolean found = false;
				for (List<String> lst : okrs) {
					if (lst.get(0).equals(okuri)) {
						found = true;
						if (!lst.contains(val)) {lst.add(val);}
					}
				}
				if (!found) {
					List<String> new_okr = new ArrayList<String>();
					new_okr.add(okuri);
					new_okr.add(val);
					okrs.add(new_okr);
				}
			}

			for (String str : cands) {
				new_val.append("/");
				new_val.append(str);
			}
			for (List<String> lst : okrs) {
				new_val.append("/[");
				for (String str : lst) {
					new_val.append(str);
					new_val.append("/");
				}
				new_val.append("]");
			}
			new_val.append("/");

			try {
				mOldValue = (String)mBTree.find(key);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			mBTree.insert(key, new_val.toString(), true);
			SKKUtils.dlog("add to user dict: key=" + key + " new_val=" + new_val.toString());

			mRecMan.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void rollBack() {
		if (mOldKey == null) { return; }

		try {
			if (mOldValue == null) {
				mBTree.remove(mOldKey);
			} else {
				mBTree.insert(mOldKey, mOldValue, true);
			}

			mRecMan.commit();
			mOldValue = null;
			mOldKey = null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void commitChanges() {
		try {
			mRecMan.commit();
			SKKUtils.dlog("user dict: commited changes");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}