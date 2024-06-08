package com.emanuelef.remote_capture.fragments;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.adapters.FileConnectionsAdapter;
import com.emanuelef.remote_capture.model.AppInfo;
import com.emanuelef.remote_capture.model.FileConnection;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileConnectionsFragment extends Fragment {

    String folderPath = "";
    private RecyclerView recyclerView;
    private TextView errorTextView;

    public FileConnectionsFragment(String folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_connections, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        errorTextView = view.findViewById(R.id.error_text_view);



        String csvPath = folderPath + "/csvFile.csv";
        File csvFile = new File(csvPath);

        if (!csvFile.exists()) {
            errorTextView.setText("capture not exist");
            errorTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            List<FileConnection> connections = readCsv(csvFile);
            FileConnectionsAdapter adapter = new FileConnectionsAdapter(requireContext(), connections);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            errorTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private List<FileConnection> readCsv(File csvFile) {
        List<FileConnection> connections = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            List<String[]> records = reader.readAll();
            for (String[] record : records) {
                // Assuming the first row contains headers, skip it
                if (records.indexOf(record) == 0) continue;
                connections.add(new FileConnection(
                        record[0],  // IPProto
                        record[1],  // SrcIP
                        record[2],  // SrcPort
                        record[3],  // DstIp
                        record[4],  // DstPort
                        record[5],  // UID
                        record[6],  // App
                        record[7],  // Proto
                        record[8],  // Status
                        record[9],  // Info
                        Long.parseLong(record[10]), // BytesSent
                        Long.parseLong(record[11]), // BytesRcvd
                        Long.parseLong(record[12]), // PktsSent
                        Long.parseLong(record[13]), // PktsRcvd
                        record[14], // FirstSeen
                        record[15]  // LastSeen
                ));
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return connections;
    }
}
