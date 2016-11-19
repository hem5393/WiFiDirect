package com.example.me.wifidirect;

/**
 * Created by ME on 19-Nov-16.
 */


import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.content.DialogInterface.*;


public class PeerListFragment extends ListFragment implements PeerListListener {
    private WifiP2pDevice device;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    ProgressDialog progressDialog = null;
    View contentView = null;

    private class PeerListAdapter extends ArrayAdapter<WifiP2pDevice>{
        private List<WifiP2pDevice> items;

        public PeerListAdapter(Context context, int resource,List<WifiP2pDevice> peerDevices) {
            super(context, resource,peerDevices);
            items = peerDevices;
        }

        @Override
        public View getView(int position, View convetView, ViewGroup parent){
            View v = convetView;
            if(v == null){
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices,null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null){
                TextView top = (TextView) v.findViewById(R.id.peer_name);
                TextView bottom = (TextView) v.findViewById(R.id.peer_details);
                if(top != null){
                    top.setText(device.deviceName);
                }
                if (bottom != null){
                    bottom.setText(getDeviceStatus(device.status));
                }
            }
            return v;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new PeerListAdapter(getActivity(),R.layout.row_devices,peers));
    }
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup content,Bundle savedInstanceState){
        contentView = inflater.inflate(R.layout.peer_list,null);
        return contentView;
    }
    private static String getDeviceStatus(int deviceStatus){
        Log.d(MainActivity.TAG,"Status" + deviceStatus);
        switch (deviceStatus){
            case WifiP2pDevice.AVAILABLE:
                return "Device is Available.";
            case WifiP2pDevice.CONNECTED:
                return "Device is Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.INVITED:
                return "Device is Invited.";
            case WifiP2pDevice.UNAVAILABLE:
                return "Device is Unavailable.";
            default:
                return "Something is Wrong.";
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id){
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        ((MainActivity) getActivity()).showDetails(device);

    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peersList.getDeviceList());
        ((PeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if(peers.size() == 0){
            Log.d(MainActivity.TAG, "No devices found. Make sure other devices are visible.");
        }
    }

    public void onInitiateDiscovery(){
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        progressDialog  = ProgressDialog.show(getActivity(), "Press back to cancle", "Finding new peers", true, true, new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {}
        });

    }

    public WifiP2pDevice getDevice(){
        return device;
    }

    public void clearPeerList(){
        peers.clear();
        ((PeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    public void updateInfo(WifiP2pDevice device){
        this.device = device;
        TextView textView = (TextView) contentView.findViewById(R.id.device_name);
        textView.setText(device.deviceName);
        textView = (TextView) contentView.findViewById(R.id.device_status);
        textView.setText(getDeviceStatus(device.status));
    }

}

