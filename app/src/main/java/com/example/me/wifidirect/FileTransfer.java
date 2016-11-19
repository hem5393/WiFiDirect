package com.example.me.wifidirect;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by ME on 19-Nov-16.
 */

public class FileTransfer extends IntentService {
    public static final String ACTION_SEND_FILE = "com.android.hem.wifi_direct.SEND_FILE";
    public static final String EXTRA_FILE_PATH = "file_uri";
    public static final String EXTRA_GROUP_OWNER_ADDRESS = "go_host" ;
    public static final String EXTRA_GROUP_OWNER_PORT = "go_port";
    private static final int SOCKET_TIMEOUT = 500;


    public FileTransfer(String name){
        super(name);
    }

    public FileTransfer(){
        super("FileTransfer");
    }

    @Override
    protected void onHandleIntent (Intent intent){
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)){
            String fileUri = intent.getExtras().getString(EXTRA_FILE_PATH);
            Log.d(MainActivity.TAG, "hello"  + fileUri);
            String host = intent.getExtras().getString(EXTRA_GROUP_OWNER_ADDRESS);
            Log.d(MainActivity.TAG, "hello"  + host);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRA_GROUP_OWNER_PORT);
            Log.d(MainActivity.TAG, "hello"  + port);

            try {
                Log.d(MainActivity.TAG,"Opening client socket- ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host,port)),SOCKET_TIMEOUT);
                Log.d(MainActivity.TAG, "Client socket- " + socket.isConnected());
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = null;
                ContentResolver resolver = context.getContentResolver();
                try {
                    inputStream = resolver.openInputStream(Uri.parse(fileUri));
                }
                catch (FileNotFoundException e){
                    Log.d(MainActivity.TAG, e.toString());
                }
                PeerDetailsFragment.copyFile(inputStream,outputStream);
                Log.d(MainActivity.TAG, "Client: Data Written");

            } catch (IOException e) {
                Log.d(MainActivity.TAG, e.toString());
            }
            finally {
                if (socket != null){
                    try {
                        socket.close();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
