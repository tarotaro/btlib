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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;

import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

@SuppressLint("NewApi")
public class BtSerialLib implements BluetoothSPP.BluetoothStateListener , BluetoothSPP.BluetoothConnectionListener, BluetoothSPP.OnDataReceivedListener {
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayList<BluetoothDevice> mDevices;
    private BluetoothSPP mBluetoothSPP;
    private BroadcastReceiver mReceiver;
    private final Activity activity = UnityPlayer.currentActivity;
    private ConnectState mState;
    private ConnectMode  mConnectMode;
    private Queue<Byte> mReadQueue;
    private String uuidForName = null;
    private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";

    private static BtSerialLib _library = new BtSerialLib();

    @Override
    public void onServiceStateChanged(int state) {
        switch(state){
            case BluetoothState.STATE_CONNECTING:
                mState = ConnectState.Connecting;
                break;
            case BluetoothState.STATE_CONNECTED:
                mState = ConnectState.Connected;
                if (mBtAdapter != null) {
                    mBtAdapter.cancelDiscovery();
                }

                // Unregister broadcast listeners
                activity.unregisterReceiver(mReceiver);
                break;
        }
    }

    @Override
    public void onDeviceConnected(String name, String address) {
        mState = ConnectState.Connected;
    }

    @Override
    public void onDeviceDisconnected() {
        mState = ConnectState.DisConnect;
    }

    @Override
    public void onDeviceConnectionFailed() {
        mState = ConnectState.Failed;
    }

    @Override
    public void onDataReceived(byte[] data) {
        for(int i= 0;i<data.length;i++) {
            mReadQueue.add(data[i]);
        }
    }

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
            bluetoothMacAddress = Settings.Secure.getString(activity.getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
        } else {
            bluetoothMacAddress = bluetoothAdapter.getAddress();
        }
        return bluetoothMacAddress;
    }

    private BtSerialLib(){
        mReadQueue = new LinkedList<Byte>();
        mBluetoothSPP = new BluetoothSPP(activity);
    }

    public static void startServer() {
        _library.uuidForName = UUID.randomUUID().toString().substring(0,4);
        _library.mConnectMode = ConnectMode.ServerMode;
        _library.mBluetoothSPP.setupService();
    }

    public static void searchDevice() {
        _library.mDevices.clear();
        _library.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // If it's already paired, skip it, because it's been listed already
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        _library.mDevices.add(device);
                    }

                    // When discovery is finished, change the Activity title
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        _library.activity.registerReceiver(_library.mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        _library.activity.registerReceiver(_library.mReceiver, filter);

        // Get the local Bluetooth adapter
        BluetoothAdapter BtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = BtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                _library.mDevices.add(device);
            }
        } else {

            _library.mBluetoothSPP.startDiscovery();
        }
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
                _library.mBluetoothSPP.connect(address);
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
        _library.mBluetoothSPP.disconnect();
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
        if(_library.mDevices == null || _library.mDevices.size() == 0){
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
        _library.mBluetoothSPP.send(data,false);
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


