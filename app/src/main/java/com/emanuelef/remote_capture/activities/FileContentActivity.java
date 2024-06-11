package com.emanuelef.remote_capture.activities;

import static com.emanuelef.remote_capture.pcap_dump.FileDumper.TAG;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.fragments.FileConnectionsFragment;
import com.emanuelef.remote_capture.fragments.FileVideoFragment;
import com.emanuelef.remote_capture.model.AppInfo;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileContentActivity extends BaseActivity implements MenuProvider {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    String folderPath = "";

    private Menu mMenu;
    private MenuItem mShareBtn;
    private MenuItem mDeleteBtn;

    public static ArrayList<AppInfo> deviceAppInfoList = new ArrayList<AppInfo>();
    private ActivityResultLauncher<Intent> shareActivityResultLauncher;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_content);
        setTitle("File Content");
        displayBackAction();
        addMenuProvider(this);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        getInstalledApps();


        folderPath = getIntent().getStringExtra("filePath");
        if (folderPath != null) {
            File file = new File(folderPath);
            if (file.exists() && file.isDirectory()) {
                String folderName = file.getName();
                setTitle(folderName);
            } else if (file.exists() && file.isFile()) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    setTitle(content);
                } catch (IOException e) {
                    e.printStackTrace();
                    setTitle("error_reading_file");
                }
            } else {
                setTitle("file_or_folder_not_found");
            }
        } else {
            setTitle("no_file_specified");
        }

        shareActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 分享完成后删除 ZIP 文件
                    File zipFile = new File(getCacheDir(), new File(folderPath).getName() + ".zip");
                    if (zipFile.exists()) {
                        zipFile.delete();
                    }
                }
        );

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        switch (position) {
                            case 0:
                                tab.setText("Connections");
                                break;
                            case 1:
                                tab.setText("Video");
                                break;
                        }
                    }
                }).attach();
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        MenuProvider.super.onPrepareMenu(menu);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.file_content_menu, menu);

        mMenu = menu;

        mShareBtn = menu.findItem(R.id.share);
        mDeleteBtn = menu.findItem(R.id.delete);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.delete) {
            Log.d(TAG, "Delete menu item clicked");
            new AlertDialog.Builder(this)
                    .setTitle("Delete Folder")
                    .setMessage("Are you sure you want to delete this folder?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        File folder = new File(folderPath);
                        if (deleteFolder(folder)) {
                            Log.d(TAG, "Folder deleted successfully");
                            finish();
                        } else {
                            Log.d(TAG, "Failed to delete folder");
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        } else if(id == R.id.share) {
            Log.d(TAG, "Share menu item clicked");

            try {
                File folder = new File(folderPath);
                File zipFile = new File(getCacheDir(), folder.getName() + ".zip");
                zipFolder(folder, zipFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/zip");
                Uri zipUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", zipFile);
                shareIntent.putExtra(Intent.EXTRA_STREAM, zipUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                shareActivityResultLauncher.launch(Intent.createChooser(shareIntent, "Share ZIP"));

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Failed to zip folder");
            }

            return true;
        }

        return false;
    }

    @Override
    public void onMenuClosed(@NonNull Menu menu) {
        MenuProvider.super.onMenuClosed(menu);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new FileConnectionsFragment(folderPath);
                case 1:
                    return new FileVideoFragment(folderPath);
                default:
                    return new FileConnectionsFragment(folderPath);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }



    public void getInstalledApps() {
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppInfo> appList = new ArrayList<>();

        for (ApplicationInfo appInfo : packages) {
            String packageName = appInfo.packageName;
            String appName = packageManager.getApplicationLabel(appInfo).toString();
            Drawable appIcon = packageManager.getApplicationIcon(appInfo);
            int appUid = appInfo.uid;
            appList.add(new AppInfo(appName, packageName, appIcon, appUid));
        }
        deviceAppInfoList = (ArrayList<AppInfo>) appList;
    }


    private boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteFolder(file)) {
                        return false;
                    }
                }
            }
        }
        return folder.delete();
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
}
