package com.emanuelef.remote_capture.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileConnection {
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
}