package com.emanuelef.remote_capture.fragments;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import com.emanuelef.remote_capture.AlwaysVisibleMediaController;
import com.emanuelef.remote_capture.R;

import java.io.File;

public class FileVideoFragment extends Fragment {

    String folderPath = "";

    public FileVideoFragment(String folderPath) {
        this.folderPath = folderPath;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_video, container, false);

        VideoView videoView = view.findViewById(R.id.video_view);
        TextView textView = view.findViewById(R.id.message_text_view);


        String videoPath = folderPath + "/recordedVideo.mp4";
        File videoFile = new File(videoPath);

        if (videoFile.exists()) {
            Uri videoUri = Uri.fromFile(videoFile);
            videoView.setVideoURI(videoUri);

            AlwaysVisibleMediaController mediaController = new AlwaysVisibleMediaController(getContext());
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            videoView.start();
        } else {
            videoView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            textView.setText(R.string.not_found_video);
        }

        return view;
    }
}
