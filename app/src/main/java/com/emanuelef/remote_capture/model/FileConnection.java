package com.emanuelef.remote_capture.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileConnection implements Parcelable {
    private String IPProto;
    private String SrcIP;
    private String SrcPort;
    private String DstIp;
    private String DstPort;
    private String UID;
    private String App;
    private String Proto;
    private String Status;
    private String Info;
    private long BytesSent;
    private long BytesRcvd;
    private long PktsSent;
    private long PktsRcvd;
    private String FirstSeen;
    private String LastSeen;

    private static final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.TAIWAN);
    private static final SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.TAIWAN);
    private static final SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.TAIWAN);

    // Constructor
    public FileConnection(String IPProto, String SrcIP, String SrcPort, String DstIp, String DstPort, String UID,
                          String App, String Proto, String Status, String Info, long BytesSent, long BytesRcvd,
                          long PktsSent, long PktsRcvd, String FirstSeen, String LastSeen) {
        this.IPProto = IPProto;
        this.SrcIP = SrcIP;
        this.SrcPort = SrcPort;
        this.DstIp = DstIp;
        this.DstPort = DstPort;
        this.UID = UID;
        this.App = App;
        this.Proto = Proto;
        this.Status = Status;
        this.Info = Info;
        this.BytesSent = BytesSent;
        this.BytesRcvd = BytesRcvd;
        this.PktsSent = PktsSent;
        this.PktsRcvd = PktsRcvd;
        this.FirstSeen = FirstSeen;
        this.LastSeen = LastSeen;
    }

    // Getters
    public String getIPProto() { return IPProto; }
    public String getSrcIP() { return SrcIP; }
    public String getSrcPort() { return SrcPort; }
    public String getDstIp() { return DstIp; }
    public String getDstPort() { return DstPort; }
    public String getUID() { return UID; }
    public String getApp() { return App; }
    public String getProto() { return Proto; }
    public String getStatus() { return Status; }
    public String getInfo() { return Info; }
    public long getBytesSent() { return BytesSent; }
    public long getBytesRcvd() { return BytesRcvd; }
    public long getPktsSent() { return PktsSent; }
    public long getPktsRcvd() { return PktsRcvd; }
    public String getFirstSeen() { return FirstSeen; }
    public String getLastSeen() { return LastSeen; }
    public long getTotalBytes() { return BytesSent + BytesRcvd; }

    public String getFormattedLastSeen() {
        try {
            Date date = inputFormat.parse(LastSeen);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return LastSeen;
        }
    }


    public long getDuration() {
        try {
            Date firstSeenDate = inputFormat.parse(FirstSeen);
            Date lastSeenDate = inputFormat.parse(LastSeen);
            return lastSeenDate.getTime() - firstSeenDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected FileConnection(Parcel in) {
        IPProto = in.readString();
        SrcIP = in.readString();
        SrcPort = in.readString();
        DstIp = in.readString();
        DstPort = in.readString();
        UID = in.readString();
        App = in.readString();
        Proto = in.readString();
        Status = in.readString();
        Info = in.readString();
        BytesSent = in.readLong();
        BytesRcvd = in.readLong();
        PktsSent = in.readLong();
        PktsRcvd = in.readLong();
        FirstSeen = in.readString();
        LastSeen = in.readString();
    }

    public static final Creator<FileConnection> CREATOR = new Creator<FileConnection>() {
        @Override
        public FileConnection createFromParcel(Parcel in) {
            return new FileConnection(in);
        }

        @Override
        public FileConnection[] newArray(int size) {
            return new FileConnection[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(IPProto);
        dest.writeString(SrcIP);
        dest.writeString(SrcPort);
        dest.writeString(DstIp);
        dest.writeString(DstPort);
        dest.writeString(UID);
        dest.writeString(App);
        dest.writeString(Proto);
        dest.writeString(Status);
        dest.writeString(Info);
        dest.writeLong(BytesSent);
        dest.writeLong(BytesRcvd);
        dest.writeLong(PktsSent);
        dest.writeLong(PktsRcvd);
        dest.writeString(FirstSeen);
        dest.writeString(LastSeen);
    }
}
