package com.example.me.wifidirect;

/**
 * Created by ME on 19-Nov-16.
 */

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;

public class PeerDetailsFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View contentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    public static int port  = 8988;

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView (final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        contentView = inflater.inflate(R.layout.device_details,null);
        contentView.findViewById(R.id.connect_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if(progressDialog != null && progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press  back to cancel", "Connecting to: " + device.deviceName, true, true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((DeviceActionListner) getActivity()).cancleDisconnect();
                    }
                });
                ((DeviceActionListner) getActivity()).connect(config);
            }
        });

        contentView.findViewById(R.id.disconnect_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DeviceActionListner) getActivity()).disconnect();
            }
        });

        contentView.findViewById(R.id.start_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.d(MainActivity.TAG,"File Selected");
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                Intent intent = new Intent(getActivity(),FileBrowser.class);
                //intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                //Log.d(MainActivity.TAG,"File Selected");
            }
        });
        return contentView;

    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data){
        getActivity();
        if (resultCode == Activity.RESULT_OK){
            Log.d(MainActivity.TAG,"File Selected");
        }
        File targetDir = (File) data.getExtras().get("file");
        if (targetDir.exists()) {
            if (targetDir.canRead()) {
                Log.d(MainActivity.TAG, "its file");
            }
        }
        if (targetDir.isFile()){
            Log.d(MainActivity.TAG, "its file .target");
        }
        //Uri uri = data.getData();
        String selectedFilePath;
        selectedFilePath = targetDir.getPath();
        Log.d(MainActivity.TAG,selectedFilePath);
        Log.d(MainActivity.TAG,"File Selected");
        TextView statusText = (TextView) contentView.findViewById(R.id.status_text);
        statusText.setText("Sending : " + selectedFilePath);
        Log.d(MainActivity.TAG,"Intent ...." + selectedFilePath);
        Intent serviceIntent = new Intent(getActivity(), FileTransfer.class);
        serviceIntent.setAction(FileTransfer.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransfer.EXTRA_FILE_PATH, selectedFilePath);
        serviceIntent.putExtra(FileTransfer.EXTRA_GROUP_OWNER_ADDRESS,info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransfer.EXTRA_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }



    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) contentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text) + ((info.isGroupOwner  == true) ? getResources().getString(R.string.yes) : getResources().getString(R.string.no)));
        view = (TextView) contentView.findViewById(R.id.device_info);
        view.setText("Group owner IP is: " + info.groupOwnerAddress.getHostAddress());

        if(info.groupFormed && info.isGroupOwner){
            new FileServerAsyncTask(getActivity(), contentView.findViewById(R.id.status_text)).execute();
        }
        else if (info.groupFormed){
            contentView.findViewById(R.id.start_button).setVisibility(View.VISIBLE);
            ((TextView) contentView.findViewById(R.id.status_text)).setText(getResources().getString(R.string.client_text));
        }
        contentView.findViewById(R.id.connect_button).setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device){
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) contentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view =(TextView) contentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }
    public void resetViews(){
        contentView.findViewById(R.id.connect_button).setVisibility(View.VISIBLE);
        TextView view = (TextView) contentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) contentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) contentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) contentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        contentView.findViewById(R.id.start_button).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }


    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String>{

        private Context context;
        private TextView statusText;

        public FileServerAsyncTask (Context context, View statusText){
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                ServerSocket serverSocket = new ServerSocket(port);
                Log.d (MainActivity.TAG, "Server: Socket opened,");
                Socket client = serverSocket.accept();
                Log.d(MainActivity.TAG,"Server: connection done." + client.getInetAddress());
                final File file = new File(Environment.getExternalStorageDirectory() + "/" + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis() + ".jpg");

                File directory = new File(file.getParent());
                Log.d(MainActivity.TAG, file.getPath());
                if (!directory.exists()){
                    directory.mkdirs();
                }
                file.createNewFile();

                Log.d(MainActivity.TAG, "Server: copying files " + file.toString());
                InputStream inputStream = client.getInputStream();
                copyFile(inputStream, new FileOutputStream(file));
                serverSocket.close();
                return file.getAbsolutePath();

            } catch (IOException e) {
                Log.d(MainActivity.TAG, e.getMessage() + "No file.");
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){
            if (result != null){
                Log.d(MainActivity.TAG,"Done.");
                statusText.setText("File copied: " + result);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result),"image/*");
                //context.startActivity(intent);
            }
        }
        @Override
        protected void onPreExecute (){
            statusText.setText("Opening server socket");
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream outputStream){
        byte buffer [] = new byte[4096];
        int length;
        long startTime = System.currentTimeMillis();

        try {
            if ((length = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            long endTime = System.currentTimeMillis() - startTime;
            Log.v("","Time taken to transfer is: "+ startTime + " " + System.currentTimeMillis()+ " " + endTime);

        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
