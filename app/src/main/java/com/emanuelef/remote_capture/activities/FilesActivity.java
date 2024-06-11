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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.adapters.FilesAdapter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private FilesAdapter adapter;
    private List<File> foldersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        setTitle("Files");
        displayBackAction();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        Drawable customDivider = ContextCompat.getDrawable(this, R.drawable.custom_divider);
        if (customDivider != null) {
            dividerItemDecoration.setDrawable(customDivider);
        }
        recyclerView.addItemDecoration(dividerItemDecoration);

        foldersList = new ArrayList<>();

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pcapDroidDir = new File(downloadsDir, "PacketRecorder");
        if (pcapDroidDir.exists() && pcapDroidDir.isDirectory()) {
            File[] files = pcapDroidDir.listFiles();
            if (files != null) {
                String pattern = "\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}";
                for (File file : files) {
                    if (file.isDirectory() && file.getName().matches(pattern)) {
                        File csvFile = new File(file, "csvFile.csv");
                        File videoFile = new File(file, "recordedVideo.mp4");
                        File pcapFile = new File(file, "pcapFile.pcap");

                        if (csvFile.exists() && videoFile.exists() && pcapFile.exists()) {
                            foldersList.add(file);
                        }
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
        foldersList = new ArrayList<>();

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File pcapDroidDir = new File(downloadsDir, "PacketRecorder");
        if (pcapDroidDir.exists() && pcapDroidDir.isDirectory()) {
            File[] files = pcapDroidDir.listFiles();
            if (files != null) {
                String pattern = "\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}";
                for (File file : files) {
                    if (file.isDirectory() && file.getName().matches(pattern)) {
                        File csvFile = new File(file, "csvFile.csv");
                        File videoFile = new File(file, "recordedVideo.mp4");
                        File pcapFile = new File(file, "pcapFile.pcap");

                        if (csvFile.exists() && videoFile.exists() && pcapFile.exists()) {
                            foldersList.add(file);
                        }
                    }
                }
            }
        }

        adapter = new FilesAdapter(this, foldersList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    private boolean isValidFolder(File folder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
        try {
            Date date = sdf.parse(folder.getName());
        } catch (ParseException e) {
            return false;
        }

        // 检查文件夹内容
        File csvFile = new File(folder, "csvFile.csv");
        File recordedVideo = new File(folder, "recordedVideo");
        File pcapFile = new File(folder, "pcapFile.pcap");

        return csvFile.exists() && recordedVideo.exists() && pcapFile.exists();
    }
}
