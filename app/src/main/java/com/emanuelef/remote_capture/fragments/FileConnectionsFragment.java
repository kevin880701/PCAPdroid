package com.emanuelef.remote_capture.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.emanuelef.remote_capture.AlwaysVisibleMediaController;
import com.emanuelef.remote_capture.CaptureHelper;
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.model.AppState;
import com.emanuelef.remote_capture.model.CaptureSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileConnectionsFragment extends Fragment {

    String folderPath = "";

    String pcapPath = "";
    File pcapFile;
    private static final String TAG = "FileConnectionsFragment";
    private AlertDialog mPcapLoadDialog;
    private SharedPreferences mPrefs;
    private CaptureHelper mCapHelper;



    public FileConnectionsFragment(String folderPath) {
        this.folderPath = folderPath;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在 onCreate 中初始化
        mCapHelper = new CaptureHelper(requireActivity(), true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_video, container, false);

        VideoView videoView = view.findViewById(R.id.video_view);
        TextView textView = view.findViewById(R.id.message_text_view);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

//        mCapHelper = new CaptureHelper(requireActivity(), true);
        mCapHelper.setListener(success -> {
            if(!success) {
                Log.w(TAG, "Capture start failed");
//                appStateReady();
            }
        });

        pcapPath = folderPath + "/pcapFile.pcap";
        pcapFile = new File(pcapPath);
        pcapFileOpenResult();

        return view;
    }
    private void pcapFileOpenResult() {
            Uri uri = Uri.fromFile(pcapFile);
            if(uri == null)
                return;

            Log.d(TAG, "pcapFileOpenResult: " + uri);
            ExecutorService executor = Executors.newSingleThreadExecutor();

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.loading);
            builder.setMessage(R.string.pcap_load_in_progress);

            mPcapLoadDialog = builder.create();
            mPcapLoadDialog.setCanceledOnTouchOutside(false);
            mPcapLoadDialog.show();

            mPcapLoadDialog.setOnCancelListener(dialogInterface -> {
                Log.i(TAG, "Abort PCAP loading");
                executor.shutdownNow();

                if (CaptureService.isServiceActive())
                    CaptureService.stopService();

                Utils.showToastLong(requireContext(), R.string.pcap_file_load_aborted);
            });
            mPcapLoadDialog.setOnDismissListener(dialog -> mPcapLoadDialog = null);

            String path = Utils.uriToFilePath(requireContext(), uri);
            if((path == null) || !Utils.isReadable(path)) {
                // Unable to get a direct file path (e.g. for files in Downloads). Copy file to the
                // cache directory
                File out = getTmpPcapPath();
                out.deleteOnExit();
                String abs_path = out.getAbsolutePath();

                // PCAP file can be big, copy in a different thread
                executor.execute(() -> {
                    try (InputStream in_stream = requireContext().getContentResolver().openInputStream(uri)) {
                        Utils.copy(in_stream, out);
                    } catch (IOException | SecurityException e) {
                        e.printStackTrace();

                        requireActivity().runOnUiThread(() -> {
                            Utils.showToastLong(requireContext(), R.string.copy_error);
                            if(mPcapLoadDialog != null) {
                                mPcapLoadDialog.dismiss();
                                mPcapLoadDialog = null;
                            }
                        });
                        return;
                    }

                    requireActivity().runOnUiThread(() -> doStartCaptureService(abs_path));
                });
            } else {
                Log.d(TAG, "pcapFileOpenResult: path: " + path);
                doStartCaptureService(path);
            }
    }

    private void doStartCaptureService(String input_pcap_path) {
//        appStateStarting();

        CaptureSettings settings = new CaptureSettings(requireContext(), mPrefs);
        settings.input_pcap_path = input_pcap_path;
        mCapHelper.startCapture(settings);
    }

    private File getTmpPcapPath() {
        return new File(requireContext().getCacheDir() + "/tmp.pcap");
    }
}
