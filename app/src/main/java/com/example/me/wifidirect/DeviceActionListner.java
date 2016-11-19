package com.example.me.wifidirect;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by ME on 19-Nov-16.
 */

public interface DeviceActionListner {
    void showDetails (WifiP2pDevice device);
    void cancleDisconnect();
    void connect(WifiP2pConfig config);
    void disconnect();
}

