package jp.gr.java_conf.na2co3.skk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jp.gr.java_conf.na2co3.dialog.ConfirmationDialogFragment;
import jp.gr.java_conf.na2co3.dialog.SimpleMessageDialogFragment;

public class SKKUserDicTool extends AppCompatActivity {
    private RecordManager mRecMan;
    private BTree mBtree;
    private boolean isOpened = false;
    List<Tuple> mEntryList = new ArrayList<>();
    private SKKUserDicTool.EntryAdapter mAdapter;
    private File mExternalStorageDir = null;
    private static final int REQUEST_IMPORT = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdictool);
        mExternalStorageDir = Environment.getExternalStorageDirectory();

        ListView listView = (ListView)findViewById(R.id.ListDictool);
        listView.setEmptyView(findViewById(R.id.EmptyListItem));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int itemPosition = position;
                ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(getString(R.string.message_confirm_remove));
                dialog.setListener(
                    new ConfirmationDialogFragment.Listener() {
                        @Override
                        public void onPositiveClick() {
                            try {
                                mBtree.remove(mEntryList.get(itemPosition).getKey());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            mEntryList.remove(itemPosition);
                            mAdapter.notifyDataSetChanged();
                        }
                        public void onNegativeClick() {}
                    });
                dialog.show(getSupportFragmentManager(), "dialog");
            }
        });

        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.sendAppPrivateCommand(null, SKKService.ACTION_COMMIT_USERDIC, null);

        mAdapter = new EntryAdapter(this, mEntryList);
        ((ListView)findViewById(R.id.ListDictool)).setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_dic_tool, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_user_dic_tool_import:
                Intent intent = new Intent(SKKUserDicTool.this, FileChooser.class);
                intent.putExtra(FileChooser.KEY_MODE, FileChooser.MODE_OPEN);
                intent.putExtra(FileChooser.KEY_DIRNAME, mExternalStorageDir.getPath());
                startActivityForResult(intent, REQUEST_IMPORT);
                return true;
            case R.id.menu_user_dic_tool_export:
                try {
                    writeToExternalStorage();
                } catch (IOException e) {
                    SimpleMessageDialogFragment errorDialog = SimpleMessageDialogFragment.newInstance(getString(R.string.error_write_to_external_storage));
                    errorDialog.setListener(
                        new SimpleMessageDialogFragment.Listener() {
                            @Override
                            public void onClick() {}
                        });
                    errorDialog.show(getSupportFragmentManager(), "dialog");
                    return true;
                }
                SimpleMessageDialogFragment msgDialog = SimpleMessageDialogFragment.newInstance(getString(R.string.message_written_to_external_storage, mExternalStorageDir.getPath() + "/" + getString(R.string.dic_name_user) + ".txt"));
                msgDialog.setListener(
                    new SimpleMessageDialogFragment.Listener() {
                        @Override
                        public void onClick() {}
                    });
                msgDialog.show(getSupportFragmentManager(), "dialog");
                return true;
            case R.id.menu_user_dic_tool_clear:
                ConfirmationDialogFragment cfDialog = ConfirmationDialogFragment.newInstance(getString(R.string.message_confirm_clear));
                cfDialog.setListener(
                    new ConfirmationDialogFragment.Listener() {
                        @Override
                        public void onPositiveClick() { recreateUserDic(); }
                        public void onNegativeClick() {}
                    });
                cfDialog.show(getSupportFragmentManager(), "dialog");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        openUserDict();

        if (requestCode == REQUEST_IMPORT && resultCode == RESULT_OK) {
            String str = intent.getStringExtra(FileChooser.KEY_FILEPATH);
            if (str == null) return;
            try {
                SKKDictionary.loadFromTextDic(str, mRecMan, mBtree, false);
            } catch (IOException e) {
                if (e instanceof CharacterCodingException) {
                    Toast.makeText(SKKUserDicTool.this, getString(R.string.error_text_dic_coding), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SKKUserDicTool.this, getString(R.string.error_file_load, str), Toast.LENGTH_LONG).show();
                }
            }
            updateListItems();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isOpened) openUserDict();
    }

    @Override public void onPause() {
        closeUserDict();

        super.onPause();
    }

    private void recreateUserDic() {
        closeUserDict();

        String dicName = getString(R.string.dic_name_user);
        deleteFile(dicName + ".db");
        deleteFile(dicName + ".lg");

        try {
            mRecMan = RecordManagerFactory.createRecordManager(getFilesDir().getAbsolutePath() + "/" + dicName);
            mBtree = BTree.createInstance(mRecMan, new StringComparator());
            mRecMan.setNamedObject(getString(R.string.btree_name), mBtree.getRecid());
            mRecMan.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SKKUtils.dlog("New user dictionary created");
        isOpened = true;
        mEntryList.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void onFailToOpenUserDict() {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(getString(R.string.error_open_user_dic));
        dialog.setListener(
            new ConfirmationDialogFragment.Listener() {
                @Override
                public void onPositiveClick() {
                    try {
                        writeToExternalStorage();
                    } catch (IOException e) {
                        SKKUtils.dlog("onFailToOpenUserDict(): " + e.toString());
                    }
                    recreateUserDic();
                }
                public void onNegativeClick() {
                    finish();
                }
            });
        dialog.show(getSupportFragmentManager(), "dialog");

    }

    private void openUserDict() {
        long recID = 0;
        try {
            mRecMan = RecordManagerFactory.createRecordManager(getFilesDir().getAbsolutePath() + "/" +  getString(R.string.dic_name_user));
            recID = mRecMan.getNamedObject(getString(R.string.btree_name));
        } catch (IOException e) {
            onFailToOpenUserDict();
        }

        if (recID == 0) {
            onFailToOpenUserDict();
        } else {
            try {
                mBtree = BTree.load(mRecMan, recID);
            } catch (IOException e) {
                onFailToOpenUserDict();
            }
            isOpened = true;
        }

        updateListItems();
    }

    private void closeUserDict() {
        if (!isOpened) return;
        try {
            mRecMan.commit();
            mRecMan.close();
            isOpened = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateListItems() {
        if (!isOpened) {mEntryList.clear(); mAdapter.notifyDataSetChanged(); return;}

        Tuple tuple = new Tuple();
        TupleBrowser browser;

        mEntryList.clear();
        try {
            browser = mBtree.browse();

            while (browser.getNext(tuple)) {
                mEntryList.add(new Tuple((String)tuple.getKey(), (String)tuple.getValue()));
            }
        } catch (IOException e) {
            onFailToOpenUserDict();
        }

        mAdapter.notifyDataSetChanged();
    }

    private void writeToExternalStorage() throws IOException {
        if (!isOpened) return;
        File outputFile = new File(mExternalStorageDir, getString(R.string.dic_name_user)+".txt");

        Tuple tuple = new Tuple();
        TupleBrowser browser;

        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile), 1024);
        browser = mBtree.browse();

        while (browser.getNext(tuple)) {
            bw.write(tuple.getKey() + " " + tuple.getValue() + "\n");
        }
        bw.flush();
        bw.close();
    }

    private class EntryAdapter extends ArrayAdapter<Tuple> {
        private EntryAdapter(Context context, List<Tuple> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = (TextView)convertView;

            if (tv == null) {
                tv = (TextView) LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            Tuple item = getItem(position);
            tv.setText(item.getKey() + "  " + item.getValue());

            return tv;
        }
    }
}
