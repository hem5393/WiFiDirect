package com.example.me.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import static android.net.wifi.p2p.WifiP2pManager.*;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;

public class MainActivity extends AppCompatActivity implements DeviceActionListner, ChannelListener {

    public static final String TAG = "MainActivity";
    private WifiP2pManager manager;
    private boolean isWifiEnabled = false;
    private boolean tryAgainChannel = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver broadcastReceiver = null;

    public void isWifiP2pEnabled (boolean isWifiEnabled){
        this.isWifiEnabled = isWifiEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentFilter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this,getMainLooper(),null);
    }
    @Override
    public void onResume(){
        super.onResume();
        broadcastReceiver = new WifiP2pBroadcastReceiver(manager,channel,this);
        registerReceiver(broadcastReceiver,intentFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    public void resetDataUI(){
        PeerListFragment list = (PeerListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        PeerDetailsFragment details = (PeerDetailsFragment) getFragmentManager().findFragmentById(R.id.frag_details);
        if (list != null){
            list.clearPeerList();
        }
        if (details != null){
            details.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.action_items, menu);
        return true;

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.wifi_direct_enable:
                if(manager != null && channel != null){
                    startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS));
                }
                else {
                    Log.e(TAG, "Channel and/or Manager is Null.");
                }
                return true;
            case R.id.wifi_direct_discover_peers:
                if(! isWifiEnabled){
                    Toast.makeText(MainActivity.this,"Enable the Wi-Fi Direct",Toast.LENGTH_SHORT).show();
                    return true;
                }
                final PeerListFragment peerList = (PeerListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                peerList.onInitiateDiscovery();
                manager.discoverPeers(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this,"Discovery Started",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this,"Discovery Failed",Toast.LENGTH_SHORT).show();

                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        PeerDetailsFragment details = (PeerDetailsFragment) getFragmentManager().findFragmentById(R.id.frag_details);
        details.showDetails(device);
    }

    @Override
    public void cancleDisconnect() {
        if (manager != null) {
            final PeerListFragment list = (PeerListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
            if (list.getDevice() == null || list.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            }
            else if (list.getDevice().status == WifiP2pDevice.AVAILABLE || list.getDevice().status == WifiP2pDevice.INVITED) {
                manager.cancelConnect(channel, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Aborting connection",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Connect abort request failed. Reason Code: " + reason, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this,"Connection successful." ,Toast.LENGTH_SHORT).show();
                // Do nothing.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this,"Connection Failed" + reason,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final PeerDetailsFragment details = (PeerDetailsFragment) getFragmentManager().findFragmentById(R.id.frag_details);
        details.resetViews();
        Log.d(TAG,"Disconnect successful.");
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                details.getView().setVisibility(View.GONE);
                Log.d(TAG,"Disconnect successful.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG,"Disconnect Failed" + reason);
                Toast.makeText(MainActivity.this, "Disconnect Failed" + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        if(manager != null && !tryAgainChannel){
            Toast.makeText(MainActivity.this,"Channel is lost or Manager is null",Toast.LENGTH_SHORT).show();
            resetDataUI();
            tryAgainChannel = true;
            manager.initialize(MainActivity.this,getMainLooper(),this);
        }
        else {
            Toast.makeText(MainActivity.this," Channel is lost premanently. Try Disable/Re-Enable P2P.", Toast.LENGTH_SHORT).show();
        }
    }
}

