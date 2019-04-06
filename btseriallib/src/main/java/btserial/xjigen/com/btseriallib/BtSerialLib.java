/*
 * Copyright 2014 Akexorcist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package btserial.xjigen.com.btseriallib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.ivbaranov.rxbluetooth.events.ConnectionStateEvent;
import com.github.ivbaranov.rxbluetooth.predicates.BtPredicate;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("NewApi")
public class BtSerialLib {
    // Member fields
    private static final UUID UUID_ANDROID_DEVICE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";


    private ArrayList<BluetoothDevice> mDevices;
    private Queue<Byte> mReadQueue;
    private ConnectState mState = ConnectState.DisConnect;
    private ConnectMode  mConnectMode;
    private String uuidForName = null;
    private RxBluetooth mRxBluetooth;
    private BluetoothSocket mServerSocket;
    private BluetoothSocket mClientSocket;
    private BluetoothConnection mServerBluetoothConnection;
    private BluetoothConnection mClientBluetoothConnection;


    private static BtSerialLib _library = new BtSerialLib();

    public enum ConnectState {
        DisConnect(0),
        Connected(1),
        Connecting(2),
        Failed(3)
        ;

        private final int state;
        private ConnectState(final int _state){
            this.state = _state;
        }

        public int getState(){
            return this.state;
        }
    }

    public enum ConnectMode {
        ServerMode,
        ClientMode
    }

    private String getDeviceAddress(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            bluetoothMacAddress = Settings.Secure.getString(getCurrentContext().getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        } else {
            bluetoothMacAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        }
        return bluetoothMacAddress;
    }

    private Context getCurrentContext(){
        return UnityPlayer.currentActivity;
    }

    private BtSerialLib(){
        mReadQueue = new LinkedList<Byte>();
        mRxBluetooth = new RxBluetooth(getCurrentContext());
        mDevices = new ArrayList<BluetoothDevice>();

        mRxBluetooth.observeConnectionState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<ConnectionStateEvent>() {
                    @Override public void accept(ConnectionStateEvent event) throws Exception {
                        switch (event.getState()) {
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                mState = ConnectState.DisConnect;
                                break;
                            case BluetoothAdapter.STATE_CONNECTING:
                                mState = ConnectState.Connecting;
                                break;
                            case BluetoothAdapter.STATE_CONNECTED:
                                mState = ConnectState.Connected;
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTING:
                                break;
                        }
                    }
                });

    }

    public static void startServer() {
        _library.uuidForName = UUID.randomUUID().toString().substring(0,4);
        _library.mConnectMode = ConnectMode.ServerMode;

        _library.mRxBluetooth.connectAsServer("servername", UUID_ANDROID_DEVICE).subscribe(
                new Consumer<BluetoothSocket>() {
                    @Override public void accept(BluetoothSocket bluetoothSocket) throws Exception {
                        // Client connected, do anything with the socket
                        _library.mServerSocket = bluetoothSocket;
                        _library.mServerBluetoothConnection = new BluetoothConnection(bluetoothSocket);
                        _library.mServerBluetoothConnection.observeByteStream()
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribe(new Consumer<Byte>() {
                                    @Override public void accept(Byte aByte) throws Exception {
                                        _library.mReadQueue.add(aByte);
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override public void accept(Throwable throwable) throws Exception {
                                        // Error occured
                                    }
                                });
                    }
                }, new Consumer<Throwable>() {
                    @Override public void accept(Throwable throwable) throws Exception {
                        // On error
                    }
                });


    }

    public static void searchDevice() {
        _library.mRxBluetooth.observeDiscovery()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                .subscribe(new Consumer<String>() {
                    @Override public void accept(String action) throws Exception {
                        Log.d("bluetooth",action);
                    }
                });
        _library.mRxBluetooth.observeScanMode()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
                .subscribe(new Consumer<Integer>() {
                    @Override public void accept(Integer integer) throws Exception {
                        Log.d("bluetooth","setCcanMode");
                    }
                });

        _library.mRxBluetooth.observeDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<BluetoothDevice>() {
                    @Override public void accept(@NonNull BluetoothDevice bluetoothDevice) throws Exception {
                        Log.d("bluetooth","deviceFound");
                        if(!_library.mDevices.contains(bluetoothDevice)) {
                            _library.mDevices.add(bluetoothDevice);
                        }
                    }
                });
        _library.mRxBluetooth.startDiscovery();
    }

    public static void connectById(String address)
    {

        if ( _library.mDevices == null){
            _library.mState = ConnectState.Failed;
            return;
        }
        boolean isFound = false;
        for (BluetoothDevice dev : _library.mDevices){
            if (dev.getAddress().equals(address)){
                isFound = true;
                _library.mRxBluetooth.connectAsClient(dev, UUID_ANDROID_DEVICE).subscribe(
                        new Consumer<BluetoothSocket>() {
                            @Override public void accept(BluetoothSocket bluetoothSocket) throws Exception {
                                // Connected to bluetooth device, do anything with the socket
                                _library.mClientSocket = bluetoothSocket;
                                _library.mClientBluetoothConnection = new BluetoothConnection(bluetoothSocket);
                                _library.mClientBluetoothConnection.observeByteStream()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeOn(Schedulers.io())
                                        .subscribe(new Consumer<Byte>() {
                                            @Override public void accept(Byte aByte) throws Exception {
                                                _library.mReadQueue.add(aByte);
                                            }
                                        }, new Consumer<Throwable>() {
                                            @Override public void accept(Throwable throwable) throws Exception {
                                                // Error occured
                                            }
                                        });
                            }
                        }, new Consumer<Throwable>() {
                            @Override public void accept(Throwable throwable) throws Exception {
                                // On error
                            }
                        });
                break;
            }
        }
        if(!isFound){
            _library.mState = ConnectState.Failed;
        }else{
            _library.mConnectMode = ConnectMode.ClientMode;
        }

    }

    public static void disConnect() {
        if(_library.mConnectMode == ConnectMode.ClientMode) {
            _library.mClientBluetoothConnection.closeConnection();
        }else{
            _library.mServerBluetoothConnection.closeConnection();
        }
    }

    public static int getConnectState(){
        return _library.mState.getState();
    }

    public static String getUUIDForName(){
        return _library.uuidForName;
    }

    public static String GetBluetoothIDList(){
        JSONObject object = new JSONObject();
        JSONArray deviceArray = new JSONArray();
        ArrayList<BluetoothDevice> devices =  new ArrayList<BluetoothDevice>();
        if(_library.mDevices == null ||  _library.mDevices.size() == 0){
            try {
                object.put("devices", deviceArray);
                return object.toString();
            }catch (JSONException ex){
                return "";
            }
        }
        devices.addAll(_library.mDevices);
        int devCnt = 0;
        try {
            for (BluetoothDevice dev : devices) {
                JSONObject device = new JSONObject();
                String devName = dev.getName();
                if (devName == null) {
                    devName = "NoName";
                }
                device.put("device", devName);
                device.put("address", dev.getAddress());
                deviceArray.put(device);
                devCnt++;
            }
            object.put("devices", deviceArray);
        } catch (JSONException exp) {
        }


        return object.toString();
    }

    public static String getBluetoothDeviceAddress(){
        String add = _library.getDeviceAddress();
        return add;
    }

    public static void send(byte [] data,int len ){
        if(_library.mConnectMode == ConnectMode.ServerMode){
            _library.mServerBluetoothConnection.send(data);
        }else{
            _library.mClientBluetoothConnection.send(data);
        }
    }

    public static byte[] recv(int len ) {
        if (_library.mReadQueue.size() < len) {
            return null;
        }else {
            int length = _library.mReadQueue.size();
            byte buf[] = new  byte[length];
            for (int i = 0; !_library.mReadQueue.isEmpty(); i++) {
                buf[i] = (_library.mReadQueue.remove());
            }
            return buf;
        }
    }

}


