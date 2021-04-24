package com.example.androidcv2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static com.example.androidcv2.MainActivity.fatalError;
import static com.example.androidcv2.MainActivity.setIsResult;
import static com.example.androidcv2.MainActivity.setIsSend;

public class BluetoothService {

    private final BluetoothAdapter adapter;
    Context mContext;
    BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;
    static Boolean isConnected = false;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public BluetoothService(Context context, BluetoothAdapter adapter) {
        this.adapter = adapter;
        this.mContext = context;
        start();
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket socket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            bluetoothDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket = tmp;
            adapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            connected(socket, deviceUUID);
        }

        public void cancel(){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void start(){
        // Cancel any thread attempting to make a connection
        if(connectThread != null){
            connectThread.cancel();
            connectThread = null;
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){
        Toast.makeText(mContext, "Connecting, please wait", Toast.LENGTH_LONG).show();
        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    private class ConnectedThread extends Thread{

        private final BluetoothSocket bSocket;
        private final InputStream is;
        private final OutputStream os;

        public ConnectedThread(BluetoothSocket socket){
            bSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = bSocket.getInputStream();
                tmpOut = bSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            is = tmpIn;
            os = tmpOut;
        }

        public void run() {
            Boolean canGo = true;
            byte[] buffer = new byte[1]; // buffer store for the stream
            int bytes; // amount of bytes returned from read()
            while(canGo)
            try {
                bytes = is.read(buffer);
                String incomingMessage = new String(buffer, 0, bytes);
                System.out.println("inside " + incomingMessage);
                if (incomingMessage.equals("1")) {
                    setIsSend(true);
                }
                if (incomingMessage.equals("2")) {
                    setIsResult(true);
                    canGo = false;
                }
                if (incomingMessage.equals("0")) {
                    setIsSend(false);
                }
                if(incomingMessage.equals("3")){
                    fatalError = true;
                }

            } catch (IOException e) {
                e.printStackTrace();
                break; // end receiving message
            }
        }

        // call from MainActivity
        public void write(byte[] bytes){
            try {
                os.write(bytes);
            } catch (IOException e) {
                setIsSend(false);
                e.printStackTrace();
            }
        }

        //call from MainActivity
        public int read(byte[] buffer){
            int receivedBytes = 0;
                try {
                    receivedBytes = is.read(buffer);
                    System.out.println("buffer " + buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            return receivedBytes;
        }

        public void cancel(){
            try {
                bSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void connected(BluetoothSocket socket, UUID deviceUUID) {
        // start the thread to perform transmision
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
        isConnected = true;

    }

    public static Boolean getIsConnected(){
        return isConnected;
    }

    // asynchronous write
    public void write(byte[] out){
    connectedThread.write(out);
    }

    public int read(byte[] buffer){
        int receivedBytes = connectedThread.read(buffer);
        return receivedBytes;
    }

}
