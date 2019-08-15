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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.provider.Settings;

import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;
import com.github.ivbaranov.rxbluetooth.events.AclEvent;
import com.github.ivbaranov.rxbluetooth.predicates.BtPredicate;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("NewApi")
public class BtSerialLib {
    // Member fields
    private static final UUID UUID_ANDROID_DEVICE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
    private Looper mByteStreamLooper;
    private Lock rlock;


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
        /*BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothMacAddress = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)*
            bluetoothMacAddress = Settings.Secure.getString(getCurrentContext().getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        } else {
            bluetoothMacAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        }*/
        return uuidForName;
    }

    private Activity getCurrentContext(){
        return UnityPlayer.currentActivity;
    }

    private BtSerialLib(){
        mReadQueue = new LinkedList<Byte>();
        mRxBluetooth = new RxBluetooth(getCurrentContext());
        mDevices = new ArrayList<BluetoothDevice>();

        mRxBluetooth.observeAclEvent() //
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<AclEvent>() {
                    @Override public void accept(AclEvent aclEvent) throws Exception {
                        switch (aclEvent.getAction()) {
                            case BluetoothDevice.ACTION_ACL_CONNECTED:
                                break;
                            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                                break;
                            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                                mState = ConnectState.DisConnect;
                                break;
                        }
                    }
                });

    }

    public static boolean startServer() {
        _library.uuidForName = UUID.randomUUID().toString().substring(0,6);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName(_library.uuidForName);
        _library.mConnectMode = ConnectMode.ServerMode;
        _library.mRxBluetooth.enableDiscoverability(_library.getCurrentContext(),1);

        if (ContextCompat.checkSelfPermission(
                _library.getCurrentContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(_library.getCurrentContext(), AccessPermissionActivity.class);
            _library.getCurrentContext().startActivity(intent);
            return false;
        }

        _library.mRxBluetooth.connectAsServer("servername", UUID_ANDROID_DEVICE)
                .observeOn(AndroidSchedulers.from(Looper.myLooper(),true))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                new Consumer<BluetoothSocket>() {
                    @Override public void accept(BluetoothSocket bluetoothSocket) throws Exception {
                        // Client connected, do anything with the socket
                        if(_library.mState != ConnectState.Connected) {
                            _library.mServerSocket = bluetoothSocket;
                            _library.mServerBluetoothConnection = new BluetoothConnection(bluetoothSocket);
                            _library.mState = ConnectState.Connected;
                            _library.mReadQueue.clear();
                            _library.mByteStreamLooper = Looper.myLooper();
                            _library.mServerBluetoothConnection.observeByteStream()
                                    .observeOn(AndroidSchedulers.from(_library.mByteStreamLooper, true))
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(new Consumer<Byte>() {
                                        @Override
                                        public void accept(Byte aByte) throws Exception {
                                            _library.mReadQueue.add(aByte);
                                        }
                                    }, new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) throws Exception {
                                            // Error occured
                                        }
                                    });
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override public void accept(Throwable throwable) throws Exception {
                        // On error
                    }
                });
        return true;
    }

    public static boolean searchDevice() {
        if (ContextCompat.checkSelfPermission(
                _library.getCurrentContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(_library.getCurrentContext(), AccessPermissionActivity.class);
            _library.getCurrentContext().startActivity(intent);
            return false;
        }

        _library.mRxBluetooth.observeDiscovery()
                .observeOn(AndroidSchedulers.from(Looper.myLooper(),true))
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.ACTION_DISCOVERY_STARTED, BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                .subscribe(new Consumer<String>() {
                    @Override public void accept(String action) throws Exception {
                        Log.d("bluetooth",action);
                    }
                });

        _library.mRxBluetooth.observeDevices()
                .observeOn(AndroidSchedulers.from(Looper.myLooper(),true))
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<BluetoothDevice>() {
                    @Override public void accept(@NonNull BluetoothDevice bluetoothDevice) throws Exception {
                        Log.d("bluetooth","deviceFound");
                        if(!_library.mDevices.contains(bluetoothDevice)) {
                            _library.mDevices.add(bluetoothDevice);
                        }
                    }
                });

        _library.mRxBluetooth.observeScanMode()
                .observeOn(AndroidSchedulers.from(Looper.myLooper(),true))
                .subscribeOn(Schedulers.computation())
                .filter(BtPredicate.in(BluetoothAdapter.SCAN_MODE_NONE,BluetoothAdapter.SCAN_MODE_CONNECTABLE,BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
                .subscribe(new Consumer<Integer>() {
                    @Override public void accept(Integer integer) throws Exception {
                        Log.d("bluetooth","setCcanMode");
                    }
                });

        _library.mRxBluetooth.startDiscovery();

        return true;

    }

    public static void connectByUuid(String address)
    {

        if ( _library.mDevices == null){
            _library.mState = ConnectState.Failed;
            return;
        }
        boolean isFound = false;
        for (BluetoothDevice dev : _library.mDevices){
            if(dev.getName()==null){
                continue;
            }
            if (dev.getName().equals(address)){
                isFound = true;

                _library.mState = ConnectState.Connecting;
                _library.mRxBluetooth.connectAsClient(dev, UUID_ANDROID_DEVICE)
                        .observeOn(AndroidSchedulers.from(Looper.myLooper(),true))
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                        new Consumer<BluetoothSocket>() {
                            @Override public void accept(BluetoothSocket bluetoothSocket) throws Exception {
                                // Connected to bluetooth device, do anything with the socket
                                if(_library.mState != ConnectState.Connected) {
                                    _library.mClientSocket = bluetoothSocket;
                                    _library.mClientBluetoothConnection = new BluetoothConnection(bluetoothSocket);
                                    _library.mReadQueue.clear();
                                    _library.mByteStreamLooper = Looper.myLooper();
                                    _library.mState = ConnectState.Connected;
                                    _library.mClientBluetoothConnection.observeByteStream()
                                            .observeOn(AndroidSchedulers.from(_library.mByteStreamLooper, true))
                                            .subscribeOn(Schedulers.io())
                                            .subscribe(new Consumer<Byte>() {
                                                @Override
                                                public void accept(Byte aByte) throws Exception {
                                                    _library.mReadQueue.add(aByte);
                                                }
                                            }, new Consumer<Throwable>() {
                                                @Override
                                                public void accept(Throwable throwable) throws Exception {
                                                    // Error occured
                                                }
                                            });
                                }
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

    public static String getUuidForName(){
        return _library.getDeviceAddress();
    }

    public static String GetBluetoothList(){
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
                if (devName == null || devName.length() != 6 ||!canParseInt(devName,16)) {
                    continue;
                }
                device.put("device", devName);
                device.put("address", dev.getAddress());
                device.put("uuid", devName);
                deviceArray.put(device);
                devCnt++;
            }
            object.put("devices", deviceArray);
        } catch (JSONException exp) {
        }


        return object.toString();
    }

    public static boolean canParseInt(String s, int radix) {
        try {
            int Val = Integer.parseInt(s, radix);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String getBluetoothDeviceAddress(){
        String add = _library.getDeviceAddress();
        return add;
    }

    public static void send(byte [] data,int len ){
        if(_library.mState == ConnectState.DisConnect) {
            return;
        }
        byte[] sendData = new byte[len];
        for(int j = 0;j< len;j++){
            sendData[j] = data[j];
        }
        if(_library.mConnectMode == ConnectMode.ServerMode && _library.mServerBluetoothConnection != null){
            _library.mServerBluetoothConnection.send(sendData);
        }else if(_library.mClientBluetoothConnection != null){
            _library.mClientBluetoothConnection.send(sendData);
        }
    }

    public static byte[] recv(int len ) {
        if (_library.mReadQueue.size() < len) {
            return null;
        }else {
            int length = _library.mReadQueue.size();
            byte buf[] = new  byte[len];
            for (int i = 0; i<len; i++) {
                buf[i] = (_library.mReadQueue.remove());
            }
            return buf;
        }
    }

}


