package btlib.xjigen.com.btsocketlib;

import android.app.Activity;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import com.unity3d.player.UnityPlayer;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;



public class BtSocketLib implements ConnectInterface {
//SingletonBtSocketLib

    public static final String SERVICE_UUID_YOU_CAN_CHANGE = "0000CA0C-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_WRITE_UUID_YOU_CAN_CHANGE = "0000F9EF-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_READ_UUID_YOU_CAN_CHANGE = "0000F9EE-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_DESK_CONFIG_UUID_YOU_CAN_CHANGE = "0009FA9-0000-1000-8000-00805f9b34fb";

    private Advertise mAdvertise;
    private Scan mScan;
    private final Activity activity = UnityPlayer.currentActivity;
    private ConnectState connectState = ConnectState.DisConnect;
    private ReadWriteModel mReadWriteModel;
    private ArrayList<BluetoothDevice> devices;
    private ConnectMode connectMode = ConnectMode.ServerMode;

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

    /**
     * デバイスをSearchさせる
     */
    private void onSearchDevice() {
        mScan = new Scan();
        mScan.getBLEClient().connectInterface = this;
        mScan.startScan(activity.getApplicationContext());
    }

    @Override
    public void callBackSearch(ArrayList<BluetoothDevice> ret){
            devices = ret;
    }

    private void onStartServer() {
        mAdvertise = new Advertise();
        mAdvertise.startAdvertise(activity.getApplicationContext(),this);
    }

    private void onCancel() {
        if(mAdvertise != null){
            mAdvertise.stopAdvertise();
        }
    }

    @Override
    public void onConnect() {
        mReadWriteModel = new ReadWriteModel();
        connectState = ConnectState.Connected;
    }

    @Override
    public void onDisConnect() {
        connectState = ConnectState.DisConnect;
        connectMode = ConnectMode.ServerMode;
    }


    public class ReadWriteModel {
        private Queue<Byte> _readQueue;
        private Queue<Byte> _writeQueue;
        public ReadWriteModel(){
        }

        public void send(byte[] buf,int len){
            if(mAdvertise != null && connectState == ConnectState.Connected
                    && connectMode == ConnectMode.ServerMode) {
                Log.w("xjigen","********send client data*******");
                _writeQueue = new LinkedList<Byte>();
                for (int i = 0; i < len; i++) {
                    _writeQueue.add(buf[i]);
                }

                mAdvertise.getBLEServer().addWriteQueue(_writeQueue);
                Log.w("xjigen","********sended client data*******");
            }else if(mScan != null && mScan.getBLEClient() != null && connectState == ConnectState.Connected
                    && connectMode == ConnectMode.ClientMode){
                Log.w("xjigen","********send client data*******");
                _writeQueue = new LinkedList<Byte>();
                for (int i = 0; i < len; i++) {
                    _writeQueue.add(buf[i]);
                }
                mScan.getBLEClient().addWriteQueue(_writeQueue);
                Log.w("xjigen","********sended client data*******");
            }
        }

        public boolean recv(byte[] buf, int length){
            if(mAdvertise != null && connectState == ConnectState.Connected
                    && connectMode == ConnectMode.ServerMode) {
                _readQueue = mAdvertise.getBLEServer().getReadQueueLock();
                if(_readQueue.size() < length){
                    return false;
                }
                Log.w("xjigen","********recv server data*******");
                try {
                    for (int i = 0; i < length && !_readQueue.isEmpty(); i++) {
                        buf[i] = (_readQueue.remove());
                    }
                }finally {
                    mAdvertise.getBLEServer().readQueueUnlock();
                }
                Log.w("xjigen","********recved server data*******");
                return true;
            }else if(mScan != null && mScan.getBLEClient() != null && connectState == ConnectState.Connected
                    && connectMode == ConnectMode.ClientMode){
                Log.w("xjigen","********recv client data*******");
                _readQueue = mScan.getBLEClient().getReadQueueLock();
                try {
                    if (_readQueue.size() < length) {
                        return false;
                    }
                    for (int i = 0; (i < length && _readQueue.isEmpty() != true); i++) {
                        buf[i] = (_readQueue.remove());
                    }
                    Log.w("calced","recv:readQueSize:"+ _readQueue.size());
                }finally {
                    mScan.getBLEClient().readQueueUnlock();
                }
                Log.w("xjigen","********recved client data*******");
                return true;
            }
            return false;

        }
    }

    private static BtSocketLib _library = new BtSocketLib();

    private BtSocketLib(){
    }

    public static void startServer()
    {
        _library.onStartServer();
    }


    public static void searchDevice() {
        _library.onSearchDevice();
    }

    public static String GetBluetoothIDList(){
        JSONObject object = new JSONObject();
        JSONArray deviceArray = new JSONArray();
        ArrayList<BluetoothDevice> devices =  new ArrayList<BluetoothDevice>();
        if(_library.devices == null || _library.devices.size() == 0){
            try {
                object.put("devices", deviceArray);
                return object.toString();
            }catch (JSONException ex){
                return "";
            }
        }
        devices.addAll(_library.devices);
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
            }
            object.put("devices", deviceArray);
        } catch (JSONException exp) {
        }


        return object.toString();
    }

    //接続
    public static void connectById(String address)
    {
        _library.connectState = ConnectState.Connecting;
        if ( _library.devices == null){
            _library.connectState = ConnectState.Failed;
            return;
        }
        boolean isFound = false;
        int index = 0;
        for (BluetoothDevice dev : _library.devices){
            if (dev.getAddress().equals(address)){
                isFound = true;
                _library.mScan.connect(index);
                break;
            }
            index++;
        }
        if(!isFound){
            _library.connectState = ConnectState.Failed;
        }else{
            _library.connectMode = ConnectMode.ClientMode;
        }
    }

    public static void connectByListIndex(int index) {
        if(_library.devices.size()<index){
            _library.connectState = ConnectState.Failed;
        }
        _library.connectMode = ConnectMode.ClientMode;
        _library.mScan.connect(index);
    }

    public static void send(byte [] data,int len ){
        _library.mReadWriteModel.send(data,len);
    }

    public static boolean recv(byte [] data,int len ){
        return _library.mReadWriteModel.recv(data,len);
    }

    public static int getConnectState(){
        return _library.connectState.getState();
    }

    public static void disConnect(){
        if(_library.mScan != null){
            _library.mScan.getBLEClient().disConnect();
        }
        if(_library.mAdvertise != null){
            _library.mAdvertise.stopAdvertise();
        }
    }



}

interface ConnectInterface extends EventListener {
    public void onConnect();
    public void onDisConnect();
    public void callBackSearch(ArrayList<BluetoothDevice> devices);
}