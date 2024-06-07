package com.emanuelef.remote_capture.adapters;

import static com.emanuelef.remote_capture.activities.FileContentActivity.deviceAppInfoList;
import static com.emanuelef.remote_capture.model.AppInfo.getIconByUid;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.activities.FileConnectionDetailActivity;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.FileConnection;

public class FileConnectionsAdapter extends RecyclerView.Adapter<FileConnectionsAdapter.ViewHolder> {

    private List<FileConnection> connections;
    private Context context;

    public FileConnectionsAdapter(Context context, List<FileConnection> connections) {
        this.context = context;
        this.connections = connections;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_connection_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        FileConnection connection = connections.get(position);
        holder.icon.setImageDrawable(getIconByUid(Integer.parseInt(connection.getUID())));
        holder.appName.setText(connection.getApp());
        holder.proto.setText(connection.getProto() + ", " + connection.getDstPort());
        holder.dstIp.setText(connection.getDstIp());
        holder.lastSeen.setText(connection.getFormattedLastSeen());
        holder.totalBytes.setText(String.valueOf(connection.getTotalBytes()) + "B");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FileConnectionDetailActivity.class);
            intent.putExtra("fileConnection", connection);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return connections.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView appName;
        public TextView proto;
        public TextView dstIp;
        public TextView lastSeen;
        public TextView totalBytes;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            appName = itemView.findViewById(R.id.app_name);
            proto = itemView.findViewById(R.id.l7proto);
            dstIp = itemView.findViewById(R.id.remote);
            lastSeen = itemView.findViewById(R.id.last_seen);
            totalBytes = itemView.findViewById(R.id.traffic);
        }
    }
}