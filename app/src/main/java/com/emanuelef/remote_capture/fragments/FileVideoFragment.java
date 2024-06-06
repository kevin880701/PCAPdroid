package com.emanuelef.remote_capture.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.emanuelef.remote_capture.R;

public class FileVideoFragment extends Fragment {

    String filePath = "EEEEEEEEEEE";

    public FileVideoFragment(String filePath) {
        this.filePath = filePath;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_video, container, false);
        TextView textView = view.findViewById(R.id.text);
        textView.setText(filePath);

//        if (getArguments() != null) {
////            filePath = getArguments().getString(filePath);
//            textView.setText(filePath);
//        }

        return view;
    }
}
