package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DictionaryManager {
	class Entry {
		public Entry(List<String> cd, List<List<String>> okr) {
			candidates = cd;
			okuri_blocks = okr;
		}

		List<String> candidates = null;
		List<List<String>> okuri_blocks = null;
	}

	private SKKDictionary mMainDict;
	private SKKUserDictionary mUserDict;

	DictionaryManager(SKKDictionary mainDict, SKKUserDictionary userDict) {
		mMainDict = mainDict;
		mUserDict = userDict;
	}

	void setUserDict(SKKUserDictionary userDict) {
		mUserDict = userDict;
	}

	List<String> findKanji(String key, String okuri) {
		SKKUtils.dlog("findKanji(): key = " + key + ", okuri = " + okuri);
		key = convSearchKey(key);
		okuri = convSearchOkuri(okuri);

		Entry mainEntry = parseEntry(mMainDict.get(key));
		Entry userEntry = parseEntry(mUserDict.get(key));

		if (BuildConfig.DEBUG) {
			SKKUtils.dlog("*** Main dict entry ***");
			if (mainEntry != null) {
				SKKUtils.dlog("  " + mainEntry.candidates.toString());
			}
			SKKUtils.dlog("*** User dict entry ***");
			if (userEntry != null) {
				SKKUtils.dlog("  *** Candidates ***");
				SKKUtils.dlog("  " + userEntry.candidates.toString());
				SKKUtils.dlog("  *** Okuri blocks ***");
				for (List<String> lst : userEntry.okuri_blocks) {
					SKKUtils.dlog("  " + lst.toString());
				}
			}

			if (mainEntry == null && userEntry == null) {
				SKKUtils.dlog("Dictoinary: Can't find Kanji for " + key);
				return null;
			}
		}

		List<String> list;
		if (mainEntry != null){
			list = mainEntry.candidates;
		} else {
			list = new ArrayList<String>();
		}

		if (userEntry != null) {
			int idx = 0;
			for (String s : userEntry.candidates) {
				if (okuri != null) {
					boolean found = false;
					for (List<String> lst : userEntry.okuri_blocks) {
						if (lst.get(0).equals(okuri) && lst.contains(s)) {
							found = true;
							break;
						}
					}
					if (!found) {continue;} //送りがなブロックに見つからなければ，追加しない
				}
				//個人辞書の候補を先頭に追加
				list.remove(s);
				list.add(idx, s);
				idx++;
			}
		}
		if (list.size() == 0) {list = null;}

		return list;
	}

	void addEntry(String key, String val, String okuri) {
		key = convSearchKey(key);
		okuri = convSearchOkuri(okuri);

		StringBuilder new_val = new StringBuilder();
		Entry entry = parseEntry(mUserDict.get(key));

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
		}

		mUserDict.put(key, new_val.toString());
	}

	Entry parseEntry(String str) {
		if (str == null) {
			return null;
		}

		List<String> cd = new ArrayList<String>();
		List<List<String>> okr = new ArrayList<List<String>>();
		String[] va_array;

		va_array = str.split("/");
		SKKUtils.dlog("length = " + va_array.length);

		if (va_array.length <= 0) {
			Log.e("SKK", "Invalid value found: " + str);
			return null;
		}

		// va_array[0]は常に空文字列なので1から始める
		for (int i=1; i<va_array.length; i++) {
			// 送りがなブロックが見つかったら中止
			if (va_array[i].startsWith("[")) break;
			cd.add(va_array[i]);
		}

		if (str.contains("[") && str.contains("]")) {
			// 送りがなブロック
			va_array = str.split("[\\[\\]]");
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

	List<String> findKeys(String key) {
		key = convSearchKey(key);

		List<String> list = mMainDict.findKeys(key);
		List<String> list2 = mUserDict.findKeys(key);
		int idx = 0;
		for (String s : list2) {
			//個人辞書のキーを先頭に追加
			list.remove(s);
			list.add(idx, s);
			idx++;
		}

		return mUserDict.findKeys(key);
	}

	String convSearchKey(String key) {
		key = SKKUtils.katakana2hirakana(key);
		if (key.contains("ゔ")) {
			key = key.replace("ゔ", "う゛");
		}
		return key;
	}

	String convSearchOkuri(String okuri) {
		if (okuri == null) {
			return null;
		}
		okuri = SKKUtils.katakana2hirakana(okuri);
		if (okuri.contains("ゔ")) {
			okuri = okuri.replace("ゔ", "う゛");
		}
		return okuri;
	}

	void rollBack() {
		mUserDict.rollBack();
	}

	void commitChanges() {
		mUserDict.commitChanges();
	}
}
