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
 * Copyright 2020-22 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.emanuelef.remote_capture.AppsResolver;
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.ConnectionsRegister;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.ConnectionDescriptor;
import com.emanuelef.remote_capture.model.FileConnection;
import com.haipq.android.flagkit.FlagImageView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FileConnectionDetailActivity extends BaseActivity {

    private TextView detailApp;
    private TextView detailProtocol;
    private TextView detailSource;
    private TextView detailDestination;
    private TextView detailBytes;
    private TextView detailPackets;
    private TextView detailFirstSeen;
    private TextView detailLastSeen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_connection_detail);

        detailApp = findViewById(R.id.detail_app);
        detailProtocol = findViewById(R.id.detail_protocol);
        detailSource = findViewById(R.id.detail_source);
        detailDestination = findViewById(R.id.detail_destination);
        detailBytes = findViewById(R.id.detail_bytes);
        detailPackets = findViewById(R.id.detail_packets);
        detailFirstSeen = findViewById(R.id.first_seen);
        detailLastSeen = findViewById(R.id.last_seen);

        FileConnection connection = getIntent().getParcelableExtra("fileConnection");

        if (connection != null) {
            detailApp.setText(connection.getApp());
            detailProtocol.setText(connection.getProto());
            detailSource.setText(connection.getSrcIP() + ":" + connection.getSrcPort());
            detailDestination.setText(connection.getDstIp() + ":" + connection.getDstPort());
            detailBytes.setText(connection.getBytesRcvd() + "B received － " + connection.getBytesSent() + "B sent");
            detailPackets.setText(connection.getPktsRcvd() + " received － " + connection.getPktsSent() + " sent");
            detailFirstSeen.setText(connection.getFirstSeen());
            detailLastSeen.setText(connection.getLastSeen());
        }
    }
}