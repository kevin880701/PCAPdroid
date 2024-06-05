/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import android.os.Bundle;
import android.os.Environment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.adapters.FilesAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private FilesAdapter adapter;
    private List<File> foldersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        setTitle("PCAPdroid");

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        foldersList = new ArrayList<>();

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pcapDroidDir = new File(downloadsDir, "PCAPdroid");
        if (pcapDroidDir.exists() && pcapDroidDir.isDirectory()) {
            File[] files = pcapDroidDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) { // 只添加資料夾
                        foldersList.add(file);
                    }
                }
            }
        }

        adapter = new FilesAdapter(this, foldersList);
        recyclerView.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
