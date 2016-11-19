package com.example.me.wifidirect;

/**
 * Created by ME on 19-Nov-16.
 */



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

import static android.net.wifi.p2p.WifiP2pManager.*;


public class WifiP2pBroadcastReceiver extends BroadcastReceiver {

    private final WifiP2pManager manager;
    private final Channel channel;
    private final MainActivity mainActivity;

    public WifiP2pBroadcastReceiver(WifiP2pManager manager, Channel channel, MainActivity mainActivity){
        super();
        this.manager = manager;
        this.channel = channel;
        this.mainActivity = mainActivity;

    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String intent_name = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intent_name)){
            int state = intent.getIntExtra(EXTRA_WIFI_STATE, -1);
            if (state == WIFI_P2P_STATE_ENABLED){
                mainActivity.isWifiP2pEnabled(true);
            }
            else {
                mainActivity.isWifiP2pEnabled(false);
                mainActivity.resetDataUI();
            }
            Log.d(MainActivity.TAG, "Wi-Fi Direct Status: " + state);
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intent_name)){
            Log.d(MainActivity.TAG,"Peer changed");
            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) mainActivity.getFragmentManager().findFragmentById(R.id.frag_list));
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intent_name)){
            if (manager == null){
                return;
            }
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()){
                PeerDetailsFragment details = (PeerDetailsFragment) mainActivity.getFragmentManager().findFragmentById(R.id.frag_details);
                manager.requestConnectionInfo(channel,details);
            }
            else {
                mainActivity.resetDataUI();
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(intent_name)){
            PeerListFragment list = (PeerListFragment) mainActivity.getFragmentManager().findFragmentById(R.id.frag_list);
            list.updateInfo((WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }

    }
}

