package jp.gr.java_conf.na2co3.skk;

import android.util.Log;

import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;

class SKKUserDictionary extends SKKDictionary {
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
				SKKUtils.dlog("user dict: loaded");
			}
		} catch (Exception e) {
			Log.e("SKK", "Error in opening the user dic: " + e.toString());
			isValid = false;
		}
	}

	void put(String key, String value) {
		mOldKey = key;
		mOldValue = get(key);

		try {
			mBTree.insert(key, value, true);
			SKKUtils.dlog("add to user dict: key=" + key + " new_val=" + value);

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
			SKKUtils.dlog("user dict: committed changes");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}