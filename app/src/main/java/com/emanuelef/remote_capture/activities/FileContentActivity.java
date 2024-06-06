package com.emanuelef.remote_capture.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.fragments.FileConnectionsFragment;
import com.emanuelef.remote_capture.fragments.FileVideoFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileContentActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    String folderPath = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_content);
        setTitle("File Content");
        displayBackAction();

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);


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
}
