package com.emanuelef.remote_capture.model;

import static com.emanuelef.remote_capture.activities.FileContentActivity.deviceAppInfoList;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String appName;
    private String packageName;
    private Drawable appIcon;
    private int appUid;

    public AppInfo(String appName, String packageName, Drawable appIcon, int appUid) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
        this.appUid = appUid;
    }

    // Getters and setters (or public fields, depending on your style)
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public int getAppUid() {
        return appUid;
    }

    public void setAppUid(int appUid) {
        this.appUid = appUid;
    }


    public static Drawable getIconByUid(int uid) {
        for (AppInfo appInfo : deviceAppInfoList) {
            if (appInfo.getAppUid() == uid) {
                return appInfo.getAppIcon();
            }
        }
        return null; // UID not found
    }
}