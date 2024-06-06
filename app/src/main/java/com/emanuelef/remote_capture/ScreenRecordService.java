package com.emanuelef.remote_capture;

import static com.emanuelef.remote_capture.Utils.getPrimaryLocale;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordService extends Service {

    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIFICATION_ID = 1;
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private int mScreenDensity;
    private int screenWidth;
    private int screenHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        // 设置前台服务类型
        Notification notification = createForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
    }
    private Notification createForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Screen recording in progress")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Record Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Screen recording in progress")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_OK);
        Intent data = intent.getParcelableExtra("data");

        screenWidth = intent.getIntExtra("screenWidth", 0);
        screenHeight = intent.getIntExtra("screenHeight", 0);
        mScreenDensity = intent.getIntExtra("screenDensity", 0);

        Log.v("@@@@@@@@@@@@@@","@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        Log.v("resultCode","" + resultCode);
        Log.v("data","" + data);
        Log.v("@@@@@@@@@@@@@@","@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

        mMediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.i("ScreenRecordService", "MediaProjection Stopped");
                if (mMediaRecorder != null) {
//                    mMediaRecorder.stop();
//                    mMediaRecorder.reset();
                }
                if (mVirtualDisplay != null) {
//                    mVirtualDisplay.release();
                }
                mMediaProjection = null;
                stopSelf();
            }
        }, null);

        initRecorder();
        createVirtualDisplay();
        mMediaRecorder.start();
        startForegroundService();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true); // 停止前台服务
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mVirtualDisplay.release();
        mMediaProjection.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initRecorder() {

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/PacketRecorder/" + CaptureService.folderName +
                        "/recordedVideo.mp4");
        mMediaRecorder.setVideoSize(screenWidth, screenHeight);
        // 一定要偶數
//        mMediaRecorder.setVideoSize(1080, 2200);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.v("initRecorder ERROR", "E：" + e);
            e.printStackTrace();
        }
    }

    private void createVirtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenRecordService",
                screenWidth, screenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);
    }
}

