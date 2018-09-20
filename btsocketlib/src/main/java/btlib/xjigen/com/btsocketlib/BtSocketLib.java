package btlib.xjigen.com.btsocketlib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.Queue;



public class BtSocketLib implements ConnectInterface {
//SingletonBtSocketLib

    public static final String SERVICE_UUID_YOU_CAN_CHANGE = "0000CA0C-0000-1000-8000-00805f9b34fb";
    public static final String CHAR_UUID_YOU_CAN_CHANGE = "0000F9EF-0000-1000-8000-00805f9b34fb";

    private Advertise mAdvertise;
    private Scan mScan;
    private final Activity activity = UnityPlayer.currentActivity;
    private boolean isConnect = false;
    private ReadWriteModel mReadWriteModel;


    /**
     * デバイスをSearchさせる
     */
    private void onSearchDevice() {
        mScan = new Scan();
        mScan.getBLEClient().connectInterface = this;
        mScan.startScan(activity.getApplicationContext());

    }

    private void callBackEndSearch(BluetoothDevice ret[]){
        JSONObject object = new JSONObject();
        JSONArray deviceArray = new JSONArray();

        try {
            for (BluetoothDevice dev : ret) {
                JSONObject device = new JSONObject();
                /*String devName = dev.getName();
                if(devName == null){
                    devName = "NoName";
                }
                device.put("device",devName);
                device.put("address", dev.getAddress());
                deviceArray.put(device);*/
            }
            object.put("devices",deviceArray);
        }catch (JSONException exp){
        }

        UnityPlayer.UnitySendMessage(searchCallBackGameObject,searchDelegateMethod,object.toString());
    }



    private void onStartServer() {
        mReadWriteModel = new ReadWriteModel();
        mAdvertise = new Advertise();
        mAdvertise.startAdvertise(activity.getApplicationContext());
        mAdvertise.getBLEServer().connectInterface = this;
    }

    private void onCancel() {
        if(mAdvertise != null){
            mAdvertise.stopAdvertise();
        }
    }

    @Override
    public void onConnect() {
        UnityPlayer.UnitySendMessage(connectCallbackGameObject,connectDelegateMethod,"connect");
    }

    @Override
    public void disConnect() {
        UnityPlayer.UnitySendMessage(connectCallbackGameObject,connectDelegateMethod,"disconnect");
    }


    public class ReadWriteModel {
        private Queue<Byte> _readQueue;
        private Queue<Byte> _writeQueue;
        public ReadWriteModel(){
        }

        public void write(byte[] buf){
            if(mAdvertise != null) {
                _writeQueue = new LinkedList<Byte>();
                for (int i = 0; i < buf.length; i++) {
                    _writeQueue.add(buf[i]);
                }
                mAdvertise.getBLEServer().addWriteQueue(_writeQueue);
            }
        }

        public Byte[] read(int length){
            if(mAdvertise != null) {
                _readQueue = mAdvertise.getBLEServer().getReadQueue();

                ArrayList<Byte> arrayList = new ArrayList<Byte>();
                for (int i = 0; i < length && !_readQueue.isEmpty(); i++) {
                    arrayList.add(_readQueue.remove());
                }
                Byte[] ret = new Byte[arrayList.size()];
                arrayList.toArray(ret);
                return ret;
            }
            Byte [] data = new Byte[128];
            return data;
        }
    }

    private static BtSocketLib _library = new BtSocketLib();

    private String searchCallBackGameObject;
    private String searchDelegateMethod;

    private String connectCallbackGameObject;
    private String connectDelegateMethod;

    private BtSocketLib(){
    }

    public static void startServer(String gameObjectName,String delegateMethod)
    {
        _library.connectCallbackGameObject = gameObjectName;
        _library.connectDelegateMethod = delegateMethod;
        _library.onStartServer();
    }


    public static void searchDevice(String gameObjectName,String delegateMethod) {
        Log.w("searchDevice","****search****");
        _library.searchCallBackGameObject = gameObjectName;
        _library.searchDelegateMethod = delegateMethod;
        _library.onSearchDevice();
    }


    //接続
    public static void connect(String address,String gameObjectName,String delegateMethod)
    {
        _library.connectCallbackGameObject = gameObjectName;
        _library.connectDelegateMethod = delegateMethod;
    }

    public static void sendData(String sendedData){
        _library.mReadWriteModel.write(sendedData.getBytes());
    }

    public static int IsConnected(){
        if(_library.isConnect){
            return 1;
        }else{
            return 0;
        }
    }

    public static void cancel(){
        _library.onCancel();
    }

}

interface ConnectInterface extends EventListener {
    public void onConnect();
    public void disConnect();
}