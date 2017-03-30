package jp.deadend.noname.skk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jp.deadend.noname.dialog.ConfirmationDialogFragment;
import jp.deadend.noname.dialog.TextInputDialogFragment;

public class SKKDicManager extends AppCompatActivity {
    private File mExternalStorageDir = null;
    private static final int REQUEST_TEXTDIC = 0;

    private List<Tuple> mDics = new ArrayList<>();
    private TupleAdapter mAdapter;

    private String mAddingDic = null; // workaround
    private boolean mChanged = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dic_manager);
        mExternalStorageDir = Environment.getExternalStorageDirectory();

        findViewById(R.id.ButtonAddDic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(SKKDicManager.this, FileChooser.class);
                intent.putExtra(FileChooser.KEY_MODE, FileChooser.MODE_OPEN);
                intent.putExtra(FileChooser.KEY_DIRNAME, mExternalStorageDir.getPath());
                startActivityForResult(intent, REQUEST_TEXTDIC);
            }
        });

        mDics.add(new Tuple(getString(R.string.label_dicmanager_ldic), ""));
        String val = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefkey_optional_dics), "");
        if (val.length() > 0) {
            String[] vals = val.split("/");
            for (int i=0; i<vals.length; i=i+2) {
                mDics.add(new Tuple(vals[i], vals[i+1]));
            }
        }
        mAdapter = new TupleAdapter(this, mDics);

        ListView listView = (ListView)findViewById(R.id.ListDicManager);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int itemPosition = position;
                if (position == 0) { return; }
                ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(getString(R.string.message_confirm_remove_dic));
                dialog.setListener(
                    new ConfirmationDialogFragment.Listener() {
                        @Override
                        public void onPositiveClick() {
                            String dicName = (String)mDics.get(itemPosition).getValue();
                            deleteFile(dicName + ".db");
                            deleteFile(dicName + ".lg");
                            mDics.remove(itemPosition);
                            mAdapter.notifyDataSetChanged();
                            mChanged = true;
                        }
                        public void onNegativeClick() {}
                    });
                dialog.show(getSupportFragmentManager(), "dialog");
            }
        });
    }

    @Override
    protected void onPause() {
        if (mChanged) {
            StringBuilder dics = new StringBuilder();
            for (int i = 1; i < mDics.size(); i++) {
                dics.append(mDics.get(i).getKey());
                dics.append("/");
                dics.append(mDics.get(i).getValue());
                dics.append("/");
            }

            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(getString(R.string.prefkey_optional_dics), dics.toString())
                .commit();


            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.sendAppPrivateCommand(null, SKKService.ACTION_RELOAD_DICS, null);
        }

        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_TEXTDIC) {
            if (resultCode == RESULT_OK) {
                String str = intent.getStringExtra(FileChooser.KEY_FILEPATH);
                if (str == null) { return; }
                mAddingDic = str;
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mAddingDic != null) {
            addDic(mAddingDic); // use dialog after fragments resumed
            mAddingDic = null;
        }
    }

    private void addDic(String filePath) {
        final String dicName = loadDic(filePath);
        if (dicName != null) {
            TextInputDialogFragment dialog = TextInputDialogFragment.newInstance(getString(R.string.label_dicmanager_input_name));
            dialog.setSingleLine(true);
            dialog.setCancelable(false);
            dialog.setListener(
                new TextInputDialogFragment.Listener() {
                    @Override
                    public void onPositiveClick(String result) {
                        if (result.length() == 0) {
                            result = getString(R.string.label_dicmanager_optionaldic);
                        } else {
                            result = result.replace("/", "");
                        }
                        String name = result;
                        int suffix = 1;
                        while (containsName(name)) {
                            suffix++;
                            name = result + "(" + suffix + ")";
                        }
                        mDics.add(new Tuple(result, dicName));
                        mAdapter.notifyDataSetChanged();
                        mChanged = true;
                    }
                    public void onNegativeClick() {
                        deleteFile(dicName + ".db");
                        deleteFile(dicName + ".lg");
                    }
                });
            dialog.show(getSupportFragmentManager(), "dialog");
        }
    }

    private String loadDic(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        String dicName;
        if (name.startsWith("SKK-JISYO.")) {
            dicName = "skk_dict_" + name.substring(10);
        } else {
            dicName = "skk_dict_" + name.replace(".", "_");
        }

        File filesDir = getFilesDir();
        File[] files = filesDir.listFiles();
        for (File f: files) {
            if (f.getName().equals(dicName + ".db")) { return null; }
        }

        RecordManager recMan = null;
        try {
            recMan = RecordManagerFactory.createRecordManager(filesDir.getAbsolutePath() + "/" + dicName);
            BTree btree = BTree.createInstance(recMan, new StringComparator());
            recMan.setNamedObject(getString(R.string.btree_name), btree.getRecid());
            recMan.commit();
            SKKDictionary.loadFromTextDic(filePath, recMan, btree, true);
        } catch (IOException e) {
            if (e instanceof CharacterCodingException) {
                Toast.makeText(SKKDicManager.this, getString(R.string.error_text_dic_coding), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(SKKDicManager.this, getString(R.string.error_file_load, filePath), Toast.LENGTH_LONG).show();
            }
            Log.e("SKK", "SKKDicManager#loadDic() Error: " + e.toString());
            if (recMan != null) {
                try {
                    recMan.close();
                } catch (IOException ee) {
                    Log.e("SKK", "SKKDicManager#loadDic() can't close(): " + ee.toString());
                }
            }
            deleteFile(dicName + ".db");
            deleteFile(dicName + ".lg");
            return null;
        }

        try {
            recMan.close();
        } catch (IOException ee) {
            Log.e("SKK", "SKKDicManager#loadDic() can't close(): " + ee.toString());
            return null;
        }

        return dicName;
    }

    private boolean containsName(String s) {
        for (Tuple item: mDics) {
            if (s.equals(item.getKey())) { return true; }
        }

        return false;
    }

    private class TupleAdapter extends ArrayAdapter<Tuple> {
        private TupleAdapter(Context context, List<Tuple> items) {
           super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = (TextView)convertView;

            if (tv == null) {
                tv = (TextView)LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            Tuple item = getItem(position);
            tv.setText((String)item.getKey());

            return tv;
        }
    }
}
