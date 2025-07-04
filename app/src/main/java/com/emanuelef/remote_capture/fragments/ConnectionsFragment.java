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

package com.emanuelef.remote_capture.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.emanuelef.remote_capture.AppsResolver;
import com.emanuelef.remote_capture.Billing;
import com.emanuelef.remote_capture.CaptureService;
import com.emanuelef.remote_capture.ConnectionsRegister;
import com.emanuelef.remote_capture.Log;
import com.emanuelef.remote_capture.MitmReceiver;
import com.emanuelef.remote_capture.PCAPdroid;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;
import com.emanuelef.remote_capture.activities.AppDetailsActivity;
import com.emanuelef.remote_capture.activities.MainActivity;
import com.emanuelef.remote_capture.interfaces.AppStateListener;
import com.emanuelef.remote_capture.model.AppDescriptor;
import com.emanuelef.remote_capture.model.AppState;
import com.emanuelef.remote_capture.model.Blocklist;
import com.emanuelef.remote_capture.model.ConnectionDescriptor;
import com.emanuelef.remote_capture.activities.ConnectionDetailsActivity;
import com.emanuelef.remote_capture.adapters.ConnectionsAdapter;
import com.emanuelef.remote_capture.model.FilterDescriptor;
import com.emanuelef.remote_capture.model.MatchList;
import com.emanuelef.remote_capture.model.MatchList.RuleType;
import com.emanuelef.remote_capture.model.Prefs;
import com.emanuelef.remote_capture.views.EmptyRecyclerView;
import com.emanuelef.remote_capture.interfaces.ConnectionsListener;
import com.emanuelef.remote_capture.activities.EditFilterActivity;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class ConnectionsFragment extends Fragment implements ConnectionsListener, MenuProvider,
        SearchView.OnQueryTextListener, AppStateListener {
    private static final String TAG = "ConnectionsFragment";
    public static final String FILTER_EXTRA = "filter";
    public static final String QUERY_EXTRA = "query";
    private Handler mHandler;
    private ConnectionsAdapter mAdapter;
    private FloatingActionButton mFabDown;
    private EmptyRecyclerView mRecyclerView;
    private TextView mEmptyText;
    private TextView mOldConnectionsText;
    private boolean autoScroll;
    private boolean listenerSet;
    private ChipGroup mActiveFilter;
    private Menu mMenu;
    private MenuItem mStartBtn;
    private MainActivity mActivity;
    private MenuItem mStopBtn;
    private MenuItem mFilesBtn;
    private MenuItem mMenuFilter;
    private MenuItem mMenuItemSearch;
    private MenuItem mSave;
    private MenuItem mMenuApps;
    private Uri mCsvFname;
    private AppsResolver mApps;
    private Set<String> mAppFilter;
    private SearchView mSearchView;
    private String mQueryToApply;

    private final ActivityResultLauncher<Intent> csvFileLauncher =
            registerForActivityResult(new StartActivityForResult(), this::csvFileResult);
    private final ActivityResultLauncher<Intent> filterLauncher =
            registerForActivityResult(new StartActivityForResult(), this::filterResult);


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (MainActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity.setAppStateListener(null);
        mActivity = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshEmptyText();

        registerConnsListener();
        mRecyclerView.setEmptyView(mEmptyText); // after registerConnsListener, when the adapter is populated

        refreshMenuIcons();
        refreshStatus();
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterConnsListener();
        mRecyclerView.setEmptyView(null);

        if(mSearchView != null)
            mQueryToApply = mSearchView.getQuery().toString();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if(mSearchView != null)
            outState.putString("search", mSearchView.getQuery().toString());
        if(mAdapter != null)
            outState.putSerializable("filter_desc", mAdapter.mFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        requireActivity().addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        return inflater.inflate(R.layout.connections, container, false);
    }

    private void refreshEmptyText() {
        if((CaptureService.getConnsRegister() != null) || CaptureService.isServiceActive())
            mEmptyText.setText(mAdapter.hasFilter() ? R.string.no_matches_found : R.string.no_connections);
        else
            mEmptyText.setText(R.string.capture_not_running_status);
    }

    private void registerConnsListener() {
        if (!listenerSet) {
            ConnectionsRegister reg = CaptureService.getConnsRegister();

            if (reg != null) {
                reg.addListener(this);
                listenerSet = true;
            }
        }
    }

    private void unregisterConnsListener() {
        if(listenerSet) {
            ConnectionsRegister reg = CaptureService.getConnsRegister();
            if (reg != null)
                reg.removeListener(this);

            listenerSet = false;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mHandler = new Handler(Looper.getMainLooper());
        mFabDown = view.findViewById(R.id.fabDown);
        mRecyclerView = view.findViewById(R.id.connections_view);
        mOldConnectionsText = view.findViewById(R.id.old_connections_notice);
        EmptyRecyclerView.MyLinearLayoutManager layoutMan = new EmptyRecyclerView.MyLinearLayoutManager(requireContext());
        mRecyclerView.setLayoutManager(layoutMan);
        mApps = new AppsResolver(requireContext());

        mEmptyText = view.findViewById(R.id.no_connections);
        mActiveFilter = view.findViewById(R.id.active_filter);
        mActiveFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if(mAdapter != null) {
                for(int checkedId: checkedIds)
                    mAdapter.mFilter.clear(checkedId);
                refreshFilteredConnections();
            }
        });

        mAdapter = new ConnectionsAdapter(requireContext(), mApps);
        mRecyclerView.setAdapter(mAdapter);
        listenerSet = false;
        registerForContextMenu(mRecyclerView);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                layoutMan.getOrientation());
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        mAdapter.setClickListener(v -> {
            int pos = mRecyclerView.getChildLayoutPosition(v);
            ConnectionDescriptor item = mAdapter.getItem(pos);

            if(item != null) {
                Intent intent = new Intent(requireContext(), ConnectionDetailsActivity.class);
                intent.putExtra(ConnectionDetailsActivity.CONN_ID_KEY, item.incr_id);
                startActivity(intent);
            }
        });

        autoScroll = true;
        showFabDown(false);

        mFabDown.setOnClickListener(v -> scrollToBottom());

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            //public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int state) {
                recheckScroll();
            }
        });

        refreshMenuIcons();

        String search = "";
        boolean fromIntent = false;
        Intent intent = requireActivity().getIntent();

        if(intent != null) {
            FilterDescriptor filter = Utils.getSerializableExtra(intent, FILTER_EXTRA, FilterDescriptor.class);
            if(filter != null) {
                mAdapter.mFilter = filter;
                fromIntent = true;
            }

            search = intent.getStringExtra(QUERY_EXTRA);
            if((search != null) && !search.isEmpty()) {
                // Avoid hiding the interesting items
                mAdapter.mFilter.showMasked = true;
                fromIntent = true;
            }
        }

        if(savedInstanceState != null) {
            if((search == null) || search.isEmpty())
                search = savedInstanceState.getString("search");

            if(!fromIntent && savedInstanceState.containsKey("filter_desc"))
                mAdapter.mFilter = Utils.getSerializable(savedInstanceState, "filter_desc", FilterDescriptor.class);
        }
        refreshActiveFilter();

        if((search != null) && !search.isEmpty())
            mQueryToApply = search;

        // Register for service status
        CaptureService.observeStatus(this, serviceStatus -> {
            if(serviceStatus == CaptureService.ServiceStatus.STARTED) {
                // register the new connection register
                if(listenerSet) {
                    unregisterConnsListener();
                    registerConnsListener();
                }

                autoScroll = true;
                showFabDown(false);
                mOldConnectionsText.setVisibility(View.GONE);
                mEmptyText.setText(R.string.no_connections);
                mApps.clear();
            }

            refreshMenuIcons();
        });

        // Register for updates
//        MitmReceiver.observeStatus(this, status -> refreshDecryptionStatus());
//        CaptureService.observeStats(this, this::onStatsUpdate);

        /* Important: call this after all the fields have been initialized */
        mActivity.setAppStateListener(this);
        refreshStatus();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Context ctx = requireContext();
        ConnectionDescriptor conn = mAdapter.getSelectedItem();
        MatchList whitelist = PCAPdroid.getInstance().getMalwareWhitelist();
        MatchList fwWhitelist = PCAPdroid.getInstance().getFirewallWhitelist();
        MatchList decryptionList = PCAPdroid.getInstance().getDecryptionList();
        Blocklist blocklist = PCAPdroid.getInstance().getBlocklist();
        boolean firewallPurchased = Billing.newInstance(ctx).isPurchased(Billing.FIREWALL_SKU);
        boolean mask_changed = false;
        boolean whitelist_changed = false;
        boolean blocklist_changed = false;
        boolean firewall_wl_changed = false;
        boolean decryption_list_changed = false;

        if(conn == null)
            return super.onContextItemSelected(item);

        int id = item.getItemId();

        if(id == R.id.hide_app) {
            mAdapter.mMask.addApp(conn.uid);
            mask_changed = true;
        } else if(id == R.id.hide_host) {
            mAdapter.mMask.addHost(conn.info);
            mask_changed = true;
        } else if(id == R.id.hide_ip) {
            mAdapter.mMask.addIp(conn.dst_ip);
            mask_changed = true;
        } else if(id == R.id.hide_proto) {
            mAdapter.mMask.addProto(conn.l7proto);
            mask_changed = true;
        } else if(id == R.id.hide_domain) {
            mAdapter.mMask.addHost(Utils.getSecondLevelDomain(conn.info));
            mask_changed = true;
        } else if(id == R.id.hide_country) {
            mAdapter.mMask.addCountry(conn.country);
            mask_changed = true;
        } else if(id == R.id.search_app) {
            AppDescriptor app = mApps.getAppByUid(conn.uid, 0);
            if(app != null)
                setQuery(app.getPackageName());
            else
                return super.onContextItemSelected(item);
        } else if(id == R.id.search_host)
            setQuery(conn.info);
        else if(id == R.id.search_ip)
            setQuery(conn.dst_ip);
        else if(id == R.id.search_proto)
            setQuery(conn.l7proto);
        else if(id == R.id.mw_whitelist_app)  {
            whitelist.addApp(conn.uid);
            whitelist_changed = true;
        } else if(id == R.id.mw_whitelist_ip)  {
            whitelist.addIp(conn.dst_ip);
            whitelist_changed = true;
        } else if(id == R.id.mw_whitelist_host) {
            whitelist.addHost(conn.info);
            whitelist_changed = true;
        } else if(id == R.id.dec_add_app)  {
            decryptionList.addApp(conn.uid);
            decryption_list_changed = true;
        } else if(id == R.id.dec_add_ip)  {
            decryptionList.addIp(conn.dst_ip);
            decryption_list_changed = true;
        } else if(id == R.id.dec_add_host)  {
            decryptionList.addHost(conn.info);
            decryption_list_changed = true;
        } else if(id == R.id.dec_rem_app)  {
            decryptionList.removeApp(conn.uid);
            decryption_list_changed = true;
        } else if(id == R.id.dec_rem_ip)  {
            decryptionList.removeIp(conn.dst_ip);
            decryption_list_changed = true;
        } else if(id == R.id.dec_rem_host)  {
            decryptionList.removeHost(conn.info);
            decryption_list_changed = true;
        } else if(id == R.id.block_app) {
            if(firewallPurchased) {
                blocklist.addApp(conn.uid);
                blocklist_changed = true;
            } else
                showFirewallPurchaseDialog();
        } else if(id == R.id.block_ip) {
            if(firewallPurchased) {
                blocklist.addIp(conn.dst_ip);
                blocklist_changed = true;
            } else
                showFirewallPurchaseDialog();
        } else if(id == R.id.block_host) {
            if(firewallPurchased) {
                blocklist.addHost(conn.info);
                blocklist_changed = true;
            } else
                showFirewallPurchaseDialog();
        } else if(id == R.id.block_domain) {
            if(firewallPurchased) {
                blocklist.addHost(Utils.getSecondLevelDomain(conn.info));
                blocklist_changed = true;
            } else
                showFirewallPurchaseDialog();
        } else if(id == R.id.unblock_app_permanently) {
            blocklist.removeApp(conn.uid);
            blocklist_changed = true;
        } else if(id == R.id.unblock_app_10m) {
            blocklist_changed = blocklist.unblockAppForMinutes(conn.uid, 10);
        } else if(id == R.id.unblock_app_1h) {
            blocklist_changed = blocklist.unblockAppForMinutes(conn.uid, 60);
        } else if(id == R.id.unblock_app_8h) {
            blocklist_changed = blocklist.unblockAppForMinutes(conn.uid, 480);
        } else if(id == R.id.unblock_ip) {
            blocklist.removeIp(conn.dst_ip);
            blocklist_changed = true;
        } else if(id == R.id.unblock_host) {
            blocklist.removeHost(conn.info);
            blocklist_changed = true;
        } else if(id == R.id.unblock_domain) {
            blocklist.removeHost(Utils.getSecondLevelDomain(conn.info));
            blocklist_changed = true;
        } else if(id == R.id.add_to_fw_whitelist) {
            fwWhitelist.addApp(conn.uid);
            firewall_wl_changed = true;
        } else if(id == R.id.remove_from_fw_whitelist) {
            fwWhitelist.removeApp(conn.uid);
            firewall_wl_changed = true;
        } else if(id == R.id.open_app_details) {
            Intent intent = new Intent(requireContext(), AppDetailsActivity.class);
            intent.putExtra(AppDetailsActivity.APP_UID_EXTRA, conn.uid);
            startActivity(intent);
        } else if(id == R.id.copy_ip)
            Utils.copyToClipboard(ctx, conn.dst_ip);
        else if(id == R.id.copy_host)
            Utils.copyToClipboard(ctx, conn.info);
        else if(id == R.id.copy_url)
            Utils.copyToClipboard(ctx, conn.url);
        else if(id == R.id.copy_http_request)
            Utils.copyToClipboard(ctx, conn.getHttpRequest());
        else if(id == R.id.copy_http_response)
            Utils.copyToClipboard(ctx, conn.getHttpResponse());
        else
            return super.onContextItemSelected(item);

        if(mask_changed) {
            mAdapter.mMask.save();
            mAdapter.mFilter.showMasked = false;
            refreshFilteredConnections();
        } else if(whitelist_changed) {
            whitelist.save();
            CaptureService.reloadMalwareWhitelist();
        } else if(firewall_wl_changed) {
            fwWhitelist.save();
            if(CaptureService.isServiceActive())
                CaptureService.requireInstance().reloadFirewallWhitelist();
        } else if(decryption_list_changed) {
            decryptionList.save();
            CaptureService.reloadDecryptionList();
        } else if(blocklist_changed)
            blocklist.saveAndReload();

        return true;
    }

    private void showFirewallPurchaseDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.paid_feature)
                .setMessage(Utils.getText(requireContext(), R.string.firewall_purchase_msg, getString(R.string.no_root_firewall)))
                .setPositiveButton(R.string.show_me, (dialogInterface, i) -> {
                    // Billing code here
                })
                .setNegativeButton(R.string.cancel_action, (dialogInterface, i) -> {})
                .show();
    }

    private void setQuery(String query) {
        Utils.setSearchQuery(mSearchView, mMenuItemSearch, query);
    }

    private void recheckScroll() {
        final EmptyRecyclerView.MyLinearLayoutManager layoutMan = (EmptyRecyclerView.MyLinearLayoutManager) mRecyclerView.getLayoutManager();
        assert layoutMan != null;
        int first_visibile_pos = layoutMan.findFirstCompletelyVisibleItemPosition();
        int last_visible_pos = layoutMan.findLastCompletelyVisibleItemPosition();
        int last_pos = mAdapter.getItemCount() - 1;
        boolean reached_bottom = (last_visible_pos >= last_pos);
        boolean is_scrolling = (first_visibile_pos != 0) || (!reached_bottom);

        if(is_scrolling) {
            if(reached_bottom) {
                autoScroll = true;
                showFabDown(false);
            } else {
                autoScroll = false;
                showFabDown(true);
            }
        } else
            showFabDown(false);
    }

    private void showFabDown(boolean visible) {
        // compared to setVisibility, .show/.hide provide animations and also properly clear the AnchorId
        if(visible)
            mFabDown.show();
        else
            mFabDown.hide();
    }

    private void scrollToBottom() {
        int last_pos = mAdapter.getItemCount() - 1;
        mRecyclerView.scrollToPosition(last_pos);

        showFabDown(false);
    }

    private void refreshActiveFilter() {
        if(mAdapter == null)
            return;

        mActiveFilter.removeAllViews();
        mAdapter.mFilter.toChips(getLayoutInflater(), mActiveFilter);
    }

    // This performs an unoptimized adapter refresh
    private void refreshFilteredConnections() {
        mAdapter.refreshFilteredConnections();
        refreshMenuIcons();
        refreshActiveFilter();
        recheckScroll();
    }

    private void recheckUntrackedConnections() {
        ConnectionsRegister reg = CaptureService.requireConnsRegister();
        if(reg.getUntrackedConnCount() > 0) {
            String info = String.format(getString(R.string.older_connections_notice), reg.getUntrackedConnCount());
            mOldConnectionsText.setText(info);
            mOldConnectionsText.setVisibility(View.VISIBLE);
        } else
            mOldConnectionsText.setVisibility(View.GONE);
    }

    @Override
    public void connectionsChanges(int num_connections) {
        // Important: must use the provided num_connections rather than accessing the register
        // in order to avoid desyncs

        // using runOnUi to populate the adapter as soon as registerConnsListener is called
        Utils.runOnUi(() -> {
            Log.d(TAG, "New connections size: " + num_connections);

            mAdapter.connectionsChanges(num_connections);

            recheckScroll();
            if(autoScroll)
                scrollToBottom();
            recheckUntrackedConnections();
        }, mHandler);
    }

    @Override
    public void connectionsAdded(int start, ConnectionDescriptor []conns) {
        mHandler.post(() -> {
            Log.d(TAG, "Add " + conns.length + " connections at " + start);

            mAdapter.connectionsAdded(start, conns);

            if(autoScroll)
                scrollToBottom();
            recheckUntrackedConnections();
        });
    }

    @Override
    public void connectionsRemoved(int start, ConnectionDescriptor []conns) {
        mHandler.post(() -> {
            Log.d(TAG, "Remove " + conns.length + " connections at " + start);
            mAdapter.connectionsRemoved(start, conns);
        });
    }

    @Override
    public void connectionsUpdated(int[] positions) {
        mHandler.post(() -> mAdapter.connectionsUpdated(positions));
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.connections_menu, menu);
        mMenu = menu;
//        mSave = menu.findItem(R.id.save);
//        mMenuFilter = menu.findItem(R.id.edit_filter);
        mMenuItemSearch = menu.findItem(R.id.search);

        mStartBtn = menu.findItem(R.id.action_start);
        mStopBtn = menu.findItem(R.id.action_stop);
        mFilesBtn = menu.findItem(R.id.action_files);
        mMenuApps = menu.findItem(R.id.action_apps);

//        mSearchView = (SearchView) mMenuItemSearch.getActionView();
//        mSearchView.setOnQueryTextListener(this);

        if((mQueryToApply != null) && (!mQueryToApply.isEmpty())) {
            String query = mQueryToApply;
            mQueryToApply = null;
            setQuery(query);
        }

        refreshMenuIcons();
        refreshStatus();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

//        if(id == R.id.save) {
//            openFileSelector();
//            return true;
//        } else if(id == R.id.edit_filter) {
//            Intent intent = new Intent(requireContext(), EditFilterActivity.class);
//            intent.putExtra(EditFilterActivity.FILTER_DESCRIPTOR, mAdapter.mFilter);
//            filterLauncher.launch(intent);
//            return true;
//        }

        return false;
    }

    private void refreshMenuIcons() {
        if(mSave == null)
            return;

        boolean is_enabled = (CaptureService.getConnsRegister() != null);

        mMenuItemSearch.setVisible(is_enabled); // NOTE: setEnabled does not work for this
        //mMenuFilter.setEnabled(is_enabled);
        mSave.setEnabled(is_enabled);
    }

    public void dumpCsv() {
//        String dump = mAdapter.dumpConnectionsCsv();
        StringBuilder builder = new StringBuilder();
        AppsResolver resolver = new AppsResolver(requireContext());
        boolean malwareDetection = Prefs.isMalwareDetectionEnabled(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));

        String header = getString(R.string.connections_csv_fields);
        builder.append(header);
        if(malwareDetection)
            builder.append(",Malicious");
        builder.append("\n");

        // Contents
        for(int i=0; i< mAdapter.getItemCount(); i++) {
            ConnectionDescriptor conn = mAdapter.getItem(i);

            if(conn != null) {
                AppDescriptor app = resolver.getAppByUid(conn.uid, 0);

                builder.append(conn.ipproto);                               builder.append(",");
                builder.append(conn.src_ip);                                builder.append(",");
                builder.append(conn.src_port);                              builder.append(",");
                builder.append(conn.dst_ip);                                builder.append(",");
                builder.append(conn.dst_port);                              builder.append(",");
                builder.append(conn.uid);                                   builder.append(",");
                builder.append((app != null) ? app.getName() : "");         builder.append(",");
                builder.append(conn.l7proto);                               builder.append(",");
                builder.append(conn.getStatusLabel(requireContext()));              builder.append(",");
                builder.append((conn.info != null) ? conn.info : "");       builder.append(",");
                builder.append(conn.sent_bytes);                            builder.append(",");
                builder.append(conn.rcvd_bytes);                            builder.append(",");
                builder.append(conn.sent_pkts);                             builder.append(",");
                builder.append(conn.rcvd_pkts);                             builder.append(",");
                builder.append(Utils.formatMillisIso8601(requireContext(), conn.first_seen));                            builder.append(",");
                builder.append(Utils.formatMillisIso8601(requireContext(), conn.last_seen));

                if(malwareDetection) {
                    builder.append(",");

                    if(conn.isBlacklisted())
                        builder.append("yes");
                }

                builder.append("\n");
            }
        }
        String dump = builder.toString();
        mCsvFname = Utils.getDownloadsUri(requireContext(), "csvFile.csv", CaptureService.folderName);

        if(mCsvFname != null) {
            Log.d(TAG, "Writing CSV file: " + mCsvFname);
            boolean error = true;

            try {
                OutputStream stream = requireActivity().getContentResolver().openOutputStream(mCsvFname, "rwt");

                if(stream != null) {
                    stream.write(dump.getBytes());
                    stream.close();
                }

                Utils.UriStat stat = Utils.getUriStat(requireContext(), mCsvFname);

                if(stat != null) {
                    String msg = String.format(getString(R.string.file_saved_with_name), stat.name);
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                } else
                    Utils.showToast(requireContext(), R.string.save_ok);

                error = false;
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(error)
                Utils.showToast(requireContext(), R.string.cannot_write_file);
        }

        mCsvFname = null;
    }

    public void openFileSelector() {
        boolean noFileDialog = false;
        String fname = Utils.getUniqueFileName(requireContext(), "csv");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fname);

        if(Utils.supportsFileDialog(requireContext(), intent)) {
            try {
                csvFileLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                noFileDialog = true;
            }
        } else
            noFileDialog = true;

        if(noFileDialog) {
            Log.w(TAG, "No app found to handle file selection");

            // Pick default path
            Uri uri = Utils.getDownloadsUri(requireContext(), fname, CaptureService.folderName);

            if(uri != null) {
                mCsvFname = uri;
                dumpCsv();
            } else
                Utils.showToastLong(requireContext(), R.string.no_activity_file_selection);
        }
    }

    private void csvFileResult(final ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            mCsvFname = result.getData().getData();
            dumpCsv();
        } else {
            mCsvFname = null;
        }
    }

    private void filterResult(final ActivityResult result) {
        if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            FilterDescriptor descriptor = Utils.getSerializableExtra(result.getData(), EditFilterActivity.FILTER_DESCRIPTOR, FilterDescriptor.class);
            if(descriptor != null) {
                mAdapter.mFilter = descriptor;
                mAdapter.refreshFilteredConnections();
                refreshActiveFilter();
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) { return true; }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.setSearch(newText);
        recheckScroll();
        refreshEmptyText();
        return true;
    }

    // NOTE: dispatched from activity, returns true if handled
    public boolean onBackPressed() {
        return Utils.backHandleSearchview(mSearchView);
    }


    @Override
    public void appStateChanged(AppState state) {
        Context context = getContext();
        Log.d("###########","" + (mMenu != null));
        if(context == null)
            return;

        if(mMenu != null) {
            if((state == AppState.running) || (state == AppState.stopping)) {
                mStartBtn.setVisible(false);
                mStopBtn.setEnabled(true);

                Drawable filesIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_files);
                if (filesIcon != null) {
                    filesIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.disable_grey), PorterDuff.Mode.SRC_IN);
                    mFilesBtn.setIcon(filesIcon);
                }
                mFilesBtn.setEnabled(false);

                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.apps);
                if (icon != null) {
                    icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.disable_grey), PorterDuff.Mode.SRC_IN);
                    mMenuApps.setIcon(icon);
                }
                mMenuApps.setEnabled(false);

                mStopBtn.setVisible(!CaptureService.isAlwaysOnVPN());
            } else { // ready || starting
                mStopBtn.setVisible(false);
                mStartBtn.setEnabled(true);
                mStartBtn.setVisible(!CaptureService.isAlwaysOnVPN());


                Drawable filesIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_files);
                if (filesIcon != null) {
                    filesIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white), PorterDuff.Mode.SRC_IN);
                    mFilesBtn.setIcon(filesIcon);
                }
                mFilesBtn.setEnabled(true);

                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.apps);
                if (icon != null) {
                    icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white), PorterDuff.Mode.SRC_IN);
                    mMenuApps.setIcon(icon);
                }
                mMenuApps.setEnabled(true);
            }
        }

        switch(state) {
            case starting:
                if(mMenu != null)
                    mStartBtn.setEnabled(false);
                break;
            case stopping:
                if(mMenu != null)
                    mStopBtn.setEnabled(false);
                break;
            default:
                break;
        }
    }

    private void refreshStatus() {
        if(mActivity != null)
            appStateChanged(mActivity.getState());
//        recheckFilterWarning();
    }
}
