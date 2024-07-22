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
 * Copyright 2020-21 - Emanuele Faranda
 */

package com.emanuelef.remote_capture.activities;

import static com.emanuelef.remote_capture.Utils.getPrimaryLocale;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.emanuelef.remote_capture.AppsResolver;
import com.emanuelef.remote_capture.Billing;
import com.emanuelef.remote_capture.BuildConfig;
import com.emanuelef.remote_capture.CaptureHelper;
import com.emanuelef.remote_capture.ConnectionsRegister;
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.MitmReceiver;
import com.emanuelef.remote_capture.PCAPdroid;
import com.emanuelef.remote_capture.ScreenRecordService;
import com.emanuelef.remote_capture.VpnReconnectService;
import com.emanuelef.remote_capture.activities.prefs.SettingsActivity;
import com.emanuelef.remote_capture.fragments.ConnectionsFragment;
import com.emanuelef.remote_capture.interfaces.AppStateListener;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.AppState;
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.model.CaptureSettings;
import com.emanuelef.remote_capture.MitmAddon;
import com.emanuelef.remote_capture.model.CaptureStats;
import com.emanuelef.remote_capture.model.ListInfo;
import com.emanuelef.remote_capture.model.Prefs;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Billing mIab;
    private ViewPager2 mPager;
    private AppState mState;
    private AppStateListener mListener;
    private File mKeylogFile;
    private DrawerLayout mDrawer;
    private SharedPreferences mPrefs;
    private NavigationView mNavView;
    private CaptureHelper mCapHelper;
    private AlertDialog mPcapLoadDialog;
    private DisplayMetrics displayMetrics;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    // helps detecting duplicate state reporting of STOPPED in MutableLiveData
    private boolean mWasStarted = false;
    private boolean mStartPressed = false;
    private boolean mDecEmptyRulesNoticeShown = false;
    private boolean mTrailerNoticeShown = false;
    public static ConnectionsFragment connectionsFragment = new ConnectionsFragment();
    private MediaProjectionManager mProjectionManager;
    private static final String TAG = "Main";

    private static final int POS_STATUS = 0;
    private static final int POS_CONNECTIONS = 1;
    private static final int TOTAL_COUNT = 1;

    public static final String TELEGRAM_GROUP_NAME = "PCAPdroid";
    public static final String GITHUB_PROJECT_URL = "https://github.com/emanuele-f/PCAPdroid";

    private final ActivityResultLauncher<Intent> sslkeyfileExportLauncher =
            registerForActivityResult(new StartActivityForResult(), this::sslkeyfileExportResult);
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted ->
                    Log.d(TAG, "Write permission " + (isGranted ? "granted" : "denied"))
            );

    private final ActivityResultLauncher<String[]> requestMultiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean notificationsGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
                Boolean audioGranted = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);
                Boolean writeGranted = result.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);

                Log.d(TAG, "Notification permission " + (notificationsGranted ? "granted" : "denied"));
                Log.d(TAG, "Record audio permission " + (audioGranted ? "granted" : "denied"));
                Log.d(TAG, "writeGranted permission " + (writeGranted ? "granted" : "denied"));
                if (!audioGranted) {
//                    if (!audioGranted || !notificationsGranted) {
                    Log.d(TAG, "One or more permissions denied, finishing activity.");
                    finish();
                } else {
                    Log.d(TAG, "All permissions granted");
                }
            });
    private final ActivityResultLauncher<Intent> peerInfoLauncher =
            registerForActivityResult(new StartActivityForResult(), this::peerInfoResult);
    private final ActivityResultLauncher<Intent> pcapFileOpenLauncher =
            registerForActivityResult(new StartActivityForResult(), this::pcapFileOpenResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setTitle("PacketRecorder");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // 取得螢幕尺寸
        
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        //displayMetrics = new DisplayMetrics();
        //getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // 初始化 screenCaptureLauncher
        screenCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    // 啟動時創建當前時間資料夾用於保存檔案
                    Locale locale = getPrimaryLocale(this);
                    final DateFormat fmt = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", locale);
                    CaptureService.folderName = fmt.format(new Date());

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File targetDir = new File(downloadsDir, "PacketRecorder/" + CaptureService.folderName);

                    if (!targetDir.exists()) {
                        if (targetDir.mkdirs()) {
                            System.out.println("Directory created: " + targetDir.getAbsolutePath());
                        }
                    }

                    if (result.getResultCode() != Activity.RESULT_OK) {
                        Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent data = result.getData();
                    Intent serviceIntent = new Intent(this, ScreenRecordService.class);
                    serviceIntent.putExtra("resultCode", result.getResultCode());
                    serviceIntent.putExtra("data", data);
                    serviceIntent.putExtra("screenWidth", displayMetrics.widthPixels);
                    serviceIntent.putExtra("screenHeight", displayMetrics.heightPixels);
                    serviceIntent.putExtra("screenDensity", displayMetrics.densityDpi);
                    ContextCompat.startForegroundService(this, serviceIntent);
                    startCapture();
                }
        );

        mIab = Billing.newInstance(this);
        mIab.setLicense(mIab.getLicense());

        initPeerAppInfo();
        initAppState();
        checkPermissions();

        mCapHelper = new CaptureHelper(this, true);
        mCapHelper.setListener(success -> {
            if (!success) {
                Log.w(TAG, "Capture start failed");
                appStateReady();
            }
        });

        mPager = findViewById(R.id.pager);
        setupTabs();

        /* Register for service status */
        CaptureService.observeStatus(this, serviceStatus -> {
            Log.d(TAG, "Service status: " + serviceStatus.name());

            if (serviceStatus == CaptureService.ServiceStatus.STARTED) {
                appStateRunning();
                mWasStarted = true;
            } else if (mWasStarted) { /* STARTED -> STOPPED */
                // The service may still be active (on premature native termination)
                if (CaptureService.isServiceActive())
                    CaptureService.stopService();

                mKeylogFile = MitmReceiver.getKeylogFilePath(MainActivity.this);
                if (!mKeylogFile.exists() || !CaptureService.isDecryptingTLS())
                    mKeylogFile = null;

                Log.d(TAG, "sslkeylog? " + (mKeylogFile != null));

                if ((Prefs.getDumpMode(mPrefs) == Prefs.DumpMode.PCAP_FILE)) {
                    showPcapActionDialog();
//                    Toast.makeText(this,"已儲存紀錄",Toast.LENGTH_SHORT);
                    // will export the keylogfile after saving/sharing pcap
                } else if (mKeylogFile != null)
                    startExportSslkeylogfile();

                appStateReady();
                mWasStarted = false;
                mStartPressed = false;
            } else /* STOPPED -> STOPPED */
                appStateReady();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!CaptureService.isServiceActive()) {
            boolean ignored = getTmpPcapPath().delete();
        }

        mCapHelper = null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavigationDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNavView != null) {
            Menu navMenu = mNavView.getMenu();
            navMenu.findItem(R.id.tls_decryption).setVisible(Prefs.getTlsDecryptionEnabled(mPrefs) && !Prefs.isRootCaptureEnabled(mPrefs));
        }

        checkPaidDrawerEntries();
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    // keep this in a separate function, used by play billing code
    private void checkPaidDrawerEntries() {
        if (mNavView == null)
            return;
        Menu navMenu = mNavView.getMenu();
        navMenu.findItem(R.id.malware_detection).setVisible(Prefs.isMalwareDetectionEnabled(this, mPrefs));
        navMenu.findItem(R.id.firewall).setVisible(mIab.isFirewallVisible());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START))
            mDrawer.closeDrawer(GravityCompat.START, true);
        else {
            if (mPager.getCurrentItem() == POS_CONNECTIONS) {
                Fragment fragment = getFragment(ConnectionsFragment.class);

                if ((fragment != null) && ((ConnectionsFragment) fragment).onBackPressed())
                    return;
            }

            super.onBackPressed();
        }
    }

    private void checkPermissions() {
//                // Needed to write PCAP files
//                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                    try {
//                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                    } catch (ActivityNotFoundException e) {
//                        Utils.showToastLong(this, R.string.no_intent_handler_found);
//                    }
//                }

        // 要求通知和影音權限
        boolean requestWritePermission =
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        boolean requestNotificationPermission = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
            boolean requestAudioPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;

            if (requestNotificationPermission || requestAudioPermission || requestWritePermission) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) || shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.notifications_notice + "\n" + getString(R.string.audio_permission_notice))
                            .setPositiveButton(R.string.ok, (dialog, whichButton) -> requestPermissions())
                            .show()
                            .setCanceledOnTouchOutside(false);
                } else {
                    requestPermissions();
                }
            }
    }


    private void requestPermissions() {
        try {
            requestMultiplePermissionsLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.RECORD_AUDIO});
        } catch (ActivityNotFoundException e) {
            Utils.showToastLong(this, R.string.no_intent_handler_found);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        try {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } catch (ActivityNotFoundException e) {
            Utils.showToastLong(this, R.string.no_intent_handler_found);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestRecordPermission() {
        try {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } catch (ActivityNotFoundException e) {
            Utils.showToastLong(this, R.string.no_intent_handler_found);
        }
    }

    // On debug builds, if the user also has the non-debug app installed (peer app), unlock the
    // already-purchased features also on this beta app
    private void initPeerAppInfo() {
        if (!BuildConfig.APPLICATION_ID.equals("com.emanuelef.remote_capture.debug"))
            return;

        final String peerAppPackage = "com.emanuelef.remote_capture";

        AppDescriptor peer = AppsResolver.resolveInstalledApp(getPackageManager(), peerAppPackage, 0);
        if (peer == null) {
            Log.d(TAG, "Peer app not found");
            mIab.clearPeerSkus();
            return;
        }

        PackageInfo pInfo = peer.getPackageInfo();
        if ((pInfo == null) || (PackageInfoCompat.getLongVersionCode(pInfo) < 56)) {
            Log.d(TAG, "Unsupported peer app version found");
            mIab.clearPeerSkus();
            return;
        }

        // Verify that the peer signature
        Utils.BuildType buildType = Utils.getVerifiedBuild(this, peerAppPackage);
        if ((buildType != Utils.BuildType.FDROID) && (buildType != Utils.BuildType.PLAYSTORE) && (buildType != Utils.BuildType.GITHUB)) {
            Log.d(TAG, "Unsupported peer app build: " + buildType.name());
            mIab.clearPeerSkus();
            return;
        }

        Log.d(TAG, "Valid peer app found (" + pInfo.versionName + " - " + PackageInfoCompat.getLongVersionCode(pInfo) + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(peerAppPackage, "com.emanuelef.remote_capture.activities.CaptureCtrl");
        intent.putExtra("action", "get_peer_info");

        try {
            peerInfoLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "Peer app launch failed");
            mIab.clearPeerSkus();
        }
    }

    private void peerInfoResult(final ActivityResult result) {
        if ((result.getResultCode() == RESULT_OK) && (result.getData() != null)) {
            Intent data = result.getData();

            try {
                @SuppressWarnings("unchecked")
                HashSet<String> skus = Utils.getSerializableExtra(data, "skus", HashSet.class);

                if (skus != null) {
                    Log.d(TAG, "Found peer app info");

                    mIab.handlePeerSkus(skus);

                    // success
                    return;
                }
            } catch (ClassCastException ignored) {
            }
        }

        // fail
        Log.d(TAG, "Invalid peer app result");
        mIab.clearPeerSkus();
    }

    private static class MainStateAdapter extends FragmentStateAdapter {
        MainStateAdapter(final FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "createFragment");

            switch (position) {
                default: // Deliberate fall-through to status tab
//                case POS_STATUS:
//                    return new StatusFragment();
                case POS_CONNECTIONS:
                    return connectionsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return TOTAL_COUNT;
        }

        public int getPageTitle(final int position) {
            switch (position) {
                default: // Deliberate fall-through to status tab
//                case POS_STATUS:
//                    return R.string.status;
                case POS_CONNECTIONS:
                    return R.string.connections_view;
            }
        }
    }

    private void setupTabs() {
        final MainStateAdapter stateAdapter = new MainStateAdapter(this);
        mPager.setAdapter(stateAdapter);

        new TabLayoutMediator(findViewById(R.id.tablayout), mPager, (tab, position) ->
                tab.setText(getString(stateAdapter.getPageTitle(position)))
        ).attach();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // This is required to properly handle the DPAD down press on Android TV, to properly
        // focus the tab content
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            View view = getCurrentFocus();

            Log.d(TAG, "onKeyDown focus " + view.getClass().getName());

            if (view instanceof TabLayout.TabView) {
                int pos = mPager.getCurrentItem();
                View focusOverride = null;

                Log.d(TAG, "TabLayout.TabView focus pos " + pos);

                if (pos == POS_STATUS)
                    focusOverride = findViewById(R.id.main_screen);
                else if (pos == POS_CONNECTIONS)
                    focusOverride = findViewById(R.id.connections_view);

                if (focusOverride != null) {
                    focusOverride.requestFocus();
                    return true;
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // Clicking "right" from the connections view goes to the fab down item
            if (mPager.getCurrentItem() == POS_CONNECTIONS) {
                RecyclerView rview = findViewById(R.id.connections_view);

                if (rview.getFocusedChild() != null) {
                    Log.d(TAG, "onKeyDown (right) focus " + rview.getFocusedChild());

                    View fab = findViewById(R.id.fabDown);

                    if (fab != null) {
                        fab.requestFocus();
                        return true;
                    }
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.item_apps) {
            if (CaptureService.getConnsRegister() != null) {
                Intent intent = new Intent(MainActivity.this, AppsActivity.class);
                startActivity(intent);
            } else
                Utils.showToast(this, R.string.start_capture_first);
        } else if (id == R.id.malware_detection) {
            Intent intent = new Intent(MainActivity.this, MalwareDetection.class);
            startActivity(intent);
        } else if (id == R.id.tls_decryption) {
            Intent intent = new Intent(MainActivity.this, EditListActivity.class);
            intent.putExtra(EditListActivity.LIST_TYPE_EXTRA, ListInfo.Type.DECRYPTION_LIST);
            startActivity(intent);
        } else if (id == R.id.firewall) {
            Intent intent = new Intent(MainActivity.this, FirewallActivity.class);
            startActivity(intent);
        } else if (id == R.id.open_log) {
            Intent intent = new Intent(MainActivity.this, LogviewActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_stats) {
            if (mState == AppState.running) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            } else
                Utils.showToast(this, R.string.start_capture_first);
        }

        return false;
    }

    public void setAppStateListener(AppStateListener listener) {
        mListener = listener;
    }

    private void notifyAppState() {
        if (mListener != null)
            mListener.appStateChanged(mState);
    }

    public void appStateReady() {
        mState = AppState.ready;
        notifyAppState();

        if (mPcapLoadDialog != null)
            checkLoadedPcap();
    }

    public void appStateStarting() {
        mState = AppState.starting;
        notifyAppState();
    }

    public void appStateRunning() {
        mState = AppState.running;
        notifyAppState();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            checkVpnLockdownNotice();
        else if (mStartPressed) { // STOPPED -> STARTED
            if (CaptureService.isDecryptingTLS() && !CaptureService.isCapturingAsRoot())
                checkDecryptionRulesNotice();
        }
    }

    public void appStateStopping() {
        mState = AppState.stopping;
        notifyAppState();
    }

    private void checkDecryptionRulesNotice() {
        if (!mDecEmptyRulesNoticeShown && PCAPdroid.getInstance().getDecryptionList().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.tls_decryption_no_rules_notice)
                    .setPositiveButton(R.string.yes, (d, whichButton) -> {
                        Intent intent = new Intent(MainActivity.this, EditListActivity.class);
                        intent.putExtra(EditListActivity.LIST_TYPE_EXTRA, ListInfo.Type.DECRYPTION_LIST);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.no, (d, whichButton) -> {
                    })
                    .show();
            mDecEmptyRulesNoticeShown = true;
        }
    }

    private void checkLoadedPcap() {
        if (mPcapLoadDialog != null) {
            mPcapLoadDialog.dismiss();
            mPcapLoadDialog = null;
        }

        if (!CaptureService.hasError()) {
            // pcap file loaded successfully
            ConnectionsRegister reg = CaptureService.getConnsRegister();

            if ((reg != null) && (reg.getConnCount() > 0)
                    && !CaptureService.hasSeenPcapdroidTrailer()
                    && !mTrailerNoticeShown
            ) {
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.pcapdroid_trailer_notice,
                                getString(R.string.unknown_app), getString(R.string.pcapdroid_trailer)))
                        .setPositiveButton(R.string.ok, (d, whichButton) -> mTrailerNoticeShown = true)
                        .show();
            } else
                Utils.showToastLong(this, R.string.pcap_load_success);

            mPager.setCurrentItem(POS_CONNECTIONS);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void checkVpnLockdownNotice() {
        if (!Prefs.lockdownVpnNoticeShown(mPrefs) && Prefs.isFirewallEnabled(this, mPrefs) && !CaptureService.isLockdownVPN()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.vpn_lockdown_notice)
                    .setPositiveButton(R.string.yes, (d, whichButton) -> Utils.startActivity(this, new Intent("android.net.vpn.SETTINGS")))
                    .setNegativeButton(R.string.no, (d, whichButton) -> {
                    })
                    .show();
            dialog.setCanceledOnTouchOutside(false);

            Prefs.setLockdownVpnNoticeShown(mPrefs);
        }
    }

    /*private void rateApp() {
        try {
            // If playstore is installed
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + this.getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            // If playstore is not available
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }*/

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_start) {
            mStartPressed = true;
            startRecording();
            return true;
        } else if (id == R.id.action_stop) {
            stopRecording();
            stopCapture();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_files) {
            Intent intent = new Intent(MainActivity.this, FilesActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_apps) {
            Intent intent = new Intent(MainActivity.this, AppFilterActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initAppState() {
        boolean is_active = CaptureService.isServiceActive();

        if (!is_active) {
            appStateReady();

            // PCAPdroid could have been closed unexpectedly (e.g. due to low memory), try to export
            // the keylog file if exists
            mKeylogFile = MitmReceiver.getKeylogFilePath(MainActivity.this);
            if (mKeylogFile.exists())
                startExportSslkeylogfile();
        } else
            appStateRunning();
    }

    private void doStartCaptureService(String input_pcap_path) {

        appStateStarting();

        CaptureSettings settings = new CaptureSettings(this, mPrefs);
        settings.input_pcap_path = input_pcap_path;
        mCapHelper.startCapture(settings);
    }

    public void startCapture() {
        if (VpnReconnectService.isAvailable())
            VpnReconnectService.stopService();

        if (showRemoteServerAlert())
            return;

        if (Prefs.getTlsDecryptionEnabled(mPrefs) && MitmAddon.needsSetup(this)) {
            Intent intent = new Intent(this, MitmSetupWizard.class);
            startActivity(intent);
            return;
        }

        if (!Prefs.isRootCaptureEnabled(mPrefs) && (Utils.getRunningVpn(this) != null)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.active_vpn_detected)
                    .setMessage(R.string.disconnect_vpn_confirm)
                    .setPositiveButton(R.string.ok, (dialog, whichButton) -> doStartCaptureService(null))
                    .setNegativeButton(R.string.cancel_action, (dialog, whichButton) -> {
                    })
                    .show();
        } else {
            doStartCaptureService(null);
        }
    }

    public void stopCapture() {
        appStateStopping();
        CaptureService.stopService();
    }

    // see also CaptureCtrl.checkRemoteServerNotAllowed
    private boolean showRemoteServerAlert() {
        if (mPrefs.getBoolean(Prefs.PREF_REMOTE_COLLECTOR_ACK, false))
            return false; // already acknowledged

        return false;
    }

    public void showPcapActionDialog() {
        Log.d(TAG, "showPcapActionDialog called");

        if (CaptureService.isUserDefinedPcapUri())
            return;

        Uri pcapUri = CaptureService.getPcapUri();
        if (pcapUri == null)
            return;

        CaptureStats stats = CaptureService.getStats();
        connectionsFragment.dumpCsv();
        if (stats.pcap_dump_size <= 0) {
            deletePcapFile(pcapUri); // empty file, delete
            return;
        }

        String pcapName = CaptureService.getPcapFname();
        if (pcapName == null)
            pcapName = "unknown";

        String message = String.format(getResources().getString(R.string.pcap_file_action), pcapName, Utils.formatBytes(stats.pcap_dump_size));
        message = "已儲存紀錄";
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message);

//        builder.setPositiveButton(R.string.share, (dialog, which) -> {
//            Intent sendIntent = new Intent(Intent.ACTION_SEND);
//            sendIntent.setType("application/cap");
//            sendIntent.putExtra(Intent.EXTRA_STREAM, pcapUri);
//            sendIntent.setClipData(ClipData.newRawUri("", pcapUri));
//            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            Utils.startActivity(this, Intent.createChooser(sendIntent, getResources().getString(R.string.share)));
//        });
//        builder.setNegativeButton(R.string.delete, (dialog, which) -> deletePcapFile(pcapUri));
        builder.setNeutralButton(R.string.ok, (dialog, which) -> {
        });
        builder.setOnDismissListener(dialogInterface -> {
            // also export the keylog
            if (mKeylogFile != null)
                startExportSslkeylogfile();
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void deletePcapFile(Uri pcapUri) {
        boolean deleted = false;

        // The getContentResolver().delete in some Android versions does not work, try to delete
        // using file path first
        String fpath = Utils.uriToFilePath(this, pcapUri);
        if (fpath != null) {
            Log.d(TAG, "deletePcapFile: path=" + fpath);

            try {
                deleted = new File(fpath).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "deletePcapFile: uri=" + pcapUri);

            try {
                deleted = (getContentResolver().delete(pcapUri, null, null) == 1);
            } catch (UnsupportedOperationException | SecurityException e) {
                e.printStackTrace();
            }
        }

        if (!deleted)
            Utils.showToast(MainActivity.this, R.string.delete_error);
    }

    public AppState getState() {
        return (mState);
    }

    private void startExportSslkeylogfile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, "sslkeylogfile.txt");

        Log.d(TAG, "startExportSslkeylogfile: launching dialog");
        Utils.launchFileDialog(this, intent, sslkeyfileExportLauncher);
    }

    private void sslkeyfileExportResult(final ActivityResult result) {
        if ((result.getResultCode() == RESULT_OK) && (result.getData() != null) && (mKeylogFile != null)) {
            try (OutputStream out = getContentResolver().openOutputStream(result.getData().getData(), "rwt")) {
                Utils.copy(mKeylogFile, out);
                Utils.showToast(this, R.string.save_ok);
            } catch (IOException e) {
                e.printStackTrace();
                Utils.showToastLong(this, R.string.export_failed);
            }
        }

        if (mKeylogFile != null) {
            // upon closing the dialog, delete the keylog

            //noinspection ResultOfMethodCallIgnored
            mKeylogFile.delete();
            mKeylogFile = null;
        }
    }

    private void startOpenPcapFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        Log.d(TAG, "startOpenPcapFile: launching dialog");
        Utils.launchFileDialog(this, intent, pcapFileOpenLauncher);
    }

    private void pcapFileOpenResult(final ActivityResult result) {
        if ((result.getResultCode() == RESULT_OK) && (result.getData() != null)) {
            Uri uri = result.getData().getData();
            if (uri == null)
                return;

            Log.d(TAG, "pcapFileOpenResult: " + uri);
            ExecutorService executor = Executors.newSingleThreadExecutor();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

                Utils.showToastLong(this, R.string.pcap_file_load_aborted);
            });
            mPcapLoadDialog.setOnDismissListener(dialog -> mPcapLoadDialog = null);

            String path = Utils.uriToFilePath(this, uri);
            if ((path == null) || !Utils.isReadable(path)) {
                // Unable to get a direct file path (e.g. for files in Downloads). Copy file to the
                // cache directory
                File out = getTmpPcapPath();
                out.deleteOnExit();
                String abs_path = out.getAbsolutePath();

                // PCAP file can be big, copy in a different thread
                executor.execute(() -> {
                    try (InputStream in_stream = getContentResolver().openInputStream(uri)) {
                        Utils.copy(in_stream, out);
                    } catch (IOException | SecurityException e) {
                        e.printStackTrace();

                        runOnUiThread(() -> {
                            Utils.showToastLong(this, R.string.copy_error);
                            if (mPcapLoadDialog != null) {
                                mPcapLoadDialog.dismiss();
                                mPcapLoadDialog = null;
                            }
                        });
                        return;
                    }

                    runOnUiThread(() -> doStartCaptureService(abs_path));
                });
            } else {
                Log.d(TAG, "pcapFileOpenResult: path: " + path);
                doStartCaptureService(path);
            }
        }
    }

    private void startRecording() {
        if (mProjectionManager != null) {
            Intent screenCaptureIntent = mProjectionManager.createScreenCaptureIntent();
            screenCaptureLauncher.launch(screenCaptureIntent);
        }
    }

    private void stopRecording() {
        Intent serviceIntent = new Intent(this, ScreenRecordService.class);
        stopService(serviceIntent);
    }

    private File getTmpPcapPath() {
        return new File(getCacheDir() + "/tmp.pcap");
    }
}
