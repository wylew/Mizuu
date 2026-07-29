/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.mizuu;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.miz.base.MizActivity;
import com.miz.db.DbAdapterSources;
import com.miz.functions.FileSource;
import com.miz.utils.IntentUtils;
import com.miz.utils.TypefaceUtils;

import java.io.File;

import static com.miz.functions.MizLib.FILESOURCE;
import static com.miz.functions.MizLib.MOVIE;
import static com.miz.functions.MizLib.TV_SHOW;
import static com.miz.functions.MizLib.TYPE;

public class AddFileSource extends MizActivity {

    private RadioGroup mContent, mFilesource;
    private static final int SELECT_FOLDER_REQUEST_CODE = 1234;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(R.string.addFileSourceTitle);

        Typeface mTypeface = TypefaceUtils.getRobotoCondensedRegular(this);

        TextView mContentType = (TextView) findViewById(R.id.contentType);
		mContentType.setTypeface(mTypeface);

		mContent = (RadioGroup) findViewById(R.id.content_type);
		mFilesource = (RadioGroup) findViewById(R.id.filesource_type);

        TextView mContentLocation = (TextView) findViewById(R.id.contentLocation);
		mContentLocation.setTypeface(mTypeface);

        Button mContinue = (Button) findViewById(R.id.continue_button);
		mContinue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFilesource.getCheckedRadioButtonId() == R.id.source_device) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivityForResult(intent, SELECT_FOLDER_REQUEST_CODE);
                } else {
                    Intent i = new Intent();
                    i.putExtra(TYPE, mContent.getCheckedRadioButtonId() == R.id.content_movie ? MOVIE : TV_SHOW);
                    if (mFilesource.getCheckedRadioButtonId() == R.id.source_smb) {
                        i.setClass(AddFileSource.this, AddNetworkFilesourceDialog.class);
                    } else if (mFilesource.getCheckedRadioButtonId() == R.id.source_upnp) {
                        i.setClass(AddFileSource.this, AddUpnpFilesourceDialog.class);
                    } else {
                        // This case handles fallback, though Device is handled above
                        i.setClass(AddFileSource.this, FileSourceBrowser.class);
                        i.putExtra(FILESOURCE, FileSource.UPNP);
                    }
                    startActivity(i);
                    finish();
                }
            }
        });
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FOLDER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (SecurityException e) {
                    // Fallback or ignore if already have permission
                }

                String path = getPathFromTreeUri(treeUri);
                if (path != null) {
                    String contentType = mContent.getCheckedRadioButtonId() == R.id.content_movie ?
                            DbAdapterSources.KEY_TYPE_MOVIE : DbAdapterSources.KEY_TYPE_SHOW;

                    MizuuApplication.getSourcesAdapter().createSource(path, contentType, FileSource.FILE, "", "", "");

                    LocalBroadcastManager.getInstance(this).sendBroadcast(IntentUtils.getFileSourceChangeListener());
                    finish();
                } else {
                    // Fallback to storing the Uri string if path conversion fails
                    String contentType = mContent.getCheckedRadioButtonId() == R.id.content_movie ?
                            DbAdapterSources.KEY_TYPE_MOVIE : DbAdapterSources.KEY_TYPE_SHOW;
                    MizuuApplication.getSourcesAdapter().createSource(treeUri.toString(), contentType, FileSource.FILE, "", "", "");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(IntentUtils.getFileSourceChangeListener());
                    finish();
                }
            }
        }
    }

    private String getPathFromTreeUri(Uri treeUri) {
        if (treeUri == null) return null;
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            String[] split = docId.split(":");
            String type = split[0];
            String relativePath = split.length > 1 ? split[1] : "";

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + relativePath;
            } else {
                // Attempt to resolve physical SD card paths
                File storageDir = new File("/storage");
                if (storageDir.exists() && storageDir.isDirectory()) {
                    File[] files = storageDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().equalsIgnoreCase(type)) {
                                return file.getAbsolutePath() + "/" + relativePath;
                            }
                        }
                    }
                }
                return "/storage/" + type + "/" + relativePath;
            }
        } catch (Exception e) {
            return null;
        }
    }
	
	@Override
	protected int getLayoutResource() {
		return R.layout.add_file_source;
	}

	@Override
	public void onStart() {
		super.onStart();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
