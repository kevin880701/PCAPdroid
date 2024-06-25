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

import static com.emanuelef.remote_capture.pcap_dump.FileDumper.TAG;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.adapters.FilesAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FilesActivity extends BaseActivity implements MenuProvider {

    private RecyclerView recyclerView;
    private FilesAdapter adapter;
    private List<File> foldersList;

    private Menu mMenu;
    private MenuItem mImportBtn;
    private MenuItem mExportBtn;

    private ActivityResultLauncher<Intent> exportActivityResultLauncher;
    private ActivityResultLauncher<Intent> importFileActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files);
        setTitle("Files");
        displayBackAction();
        addMenuProvider(this);

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

                        if (csvFile.exists() && videoFile.exists()) {
                            foldersList.add(file);
                        }
                    }
                }
            }
        }

        adapter = new FilesAdapter(this, foldersList);
        recyclerView.setAdapter(adapter);

        exportActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    File zipFile = new File(getCacheDir(), "PacketRecorder.zip");
                    if (zipFile.exists()) {
                        zipFile.delete();
                    }
                }
        );
        importFileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            importZipFile(selectedFileUri);
                        }
                    }
                }
        );
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
        refreshFoldersList();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.file_menu, menu);

        mMenu = menu;

        mExportBtn = menu.findItem(R.id.ic_export);
        mImportBtn = menu.findItem(R.id.ic_import);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.ic_export) {
            Log.d(TAG, "Share menu item clicked");

            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pcapDroidDir = new File(downloadsDir, "PacketRecorder");
                File zipFile = new File(getCacheDir(), "PacketRecorder.zip");
                zipFolder(pcapDroidDir, zipFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                Uri zipUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", zipFile);
                shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                exportActivityResultLauncher.launch(Intent.createChooser(shareIntent, "Share ZIP"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Failed to zip folder");
            }
            return true;
        } else if(id == R.id.ic_import) {
            Log.d(TAG, "Import menu item clicked");

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/zip");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            importFileActivityResultLauncher.launch(intent);

            return true;
        }

        return false;
    }

    private void zipFolder(File folder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFile(folder, folder.getName(), zos);
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    private void importZipFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pcapDroidDir = new File(downloadsDir, "PacketRecorder");

                unzip(inputStream, pcapDroidDir);
                // 更新列表
                refreshFoldersList();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to import and unzip file");
        }
    }

    private void unzip(InputStream inputStream, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String fileName = zipEntry.getName();

                // 去掉根目录名（PacketRecorder）
                if (fileName.startsWith("PacketRecorder/")) {
                    fileName = fileName.substring("PacketRecorder/".length());
                }

                File newFile = new File(destDir, fileName);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // Fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // Write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void refreshFoldersList() {
        foldersList.clear();
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

                        if (csvFile.exists() && videoFile.exists()) {
                            foldersList.add(file);
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
