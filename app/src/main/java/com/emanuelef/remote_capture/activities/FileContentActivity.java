package com.emanuelef.remote_capture.activities;

import static com.emanuelef.remote_capture.pcap_dump.FileDumper.TAG;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileContentActivity extends BaseActivity implements MenuProvider {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    String folderPath = "";

    private Menu mMenu;
    private MenuItem mShareBtn;
    private MenuItem mDeleteBtn;

    public static ArrayList<AppInfo> deviceAppInfoList = new ArrayList<AppInfo>();

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
            Log.d("#################################","EEEEEEEEEEEEEEEE");
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
}
