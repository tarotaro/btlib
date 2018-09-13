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
import android.util.Log;
import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class BtSocketLib {
//SingletonBtSocketLib
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    protected BluetoothSocket mSocket;
    public static final int REQUEST_DISCOVERABLE_BT = 5000;
    public static final int WAIT_TIME = 120;
    private static ArrayList<BluetoothDevice> mCandidateServers;

    private final UUID mUuid = UUID.fromString("5726CA0C-A6F6-4B05-A178-8070D54A91C0");
    private ServerThread mServerThread;    //サーバー用のスレッド
    private ClientThread mClientThread;    //クライアント用のスレッド
    private final Activity activity = UnityPlayer.currentActivity;

    private void onActiveBluetooth() {
        if (mBluetoothAdapter == null) {
            // Bluetoothはサポートされていない
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            //ブルートゥースをONにする
            mBluetoothAdapter.enable();
        }
    }

    /**
     * ペアリング待ちを行う
     */
    private void onPairingBluetooth() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.setClassName(activity, "btlib.xjigen.com.btsocketlib.PairingNotifyActivity");
        intent.putExtra("gameObjectName",pairingCallbackGameObject);
        intent.putExtra("delegateMethod",pairingDelegateMethod);
        activity.startActivity(intent);
    }


    /**
     * デバイスをSearchさせる
     */
    private void onSearchDevice() {
        mCandidateServers.clear();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }

    private void callBackEndSearch(BluetoothDevice ret[]){
        JSONObject object = new JSONObject();
        JSONArray deviceArray = new JSONArray();

        try {
            for (BluetoothDevice dev : ret) {
                JSONObject device = new JSONObject();
                String devName = dev.getName();
                if(devName == null){
                    devName = "NoName";
                }
                device.put("device",devName);
                device.put("address", dev.getAddress());
                deviceArray.put(device);
            }
            object.put("devices",deviceArray);
        }catch (JSONException exp){
        }

        UnityPlayer.UnitySendMessage(searchCallBackGameObject,searchDelegateMethod,object.toString());
    }

    /**
     * デバイスの検索
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // deviceをリストに格納
                mCandidateServers.add(device);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                context.unregisterReceiver(mReceiver);
                //デバイス検索の終了
                BluetoothDevice[] ret = null;
                ret = mCandidateServers.toArray(new BluetoothDevice[mCandidateServers.size()]);
                callBackEndSearch(ret);
            }
        }
    };

    private void onConnect(String address) {
        // クライアント用のスレッドを生成
        mClientThread = new ClientThread(address);
        mClientThread.start();
    }


    private void onStartServer() {
        if (mServerThread != null) {
            mServerThread.cancel();
        }
        mServerThread = new ServerThread();
        mServerThread.start();
    }

    private void onCancel() {
        if (mServerThread != null) {
            mServerThread.cancel();
            mServerThread = null;
        }
        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }
    }

    private class ClientThread extends ReceiverThread  {
        private final BluetoothDevice mServer;

        private ClientThread(String address) {
            mServer = mBluetoothAdapter.getRemoteDevice(address);
            try {
                mSocket = mServer.createRfcommSocketToServiceRecord(mUuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            // connect() の前にデバイス検出をやめる必要がある
            mBluetoothAdapter.cancelDiscovery();
            try {
                // サーバに接続する
                mSocket.connect();
                callbackConnect();
                loop();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cancel();
            }

        }

        private void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //============================
    //サーバー側の処理
    //============================
    /**
     * サーバーのスレッド
     *
     */
    private class ServerThread extends ReceiverThread  {
        private BluetoothServerSocket mServerSocket;

        private ServerThread() {
            try {
                mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        activity.getPackageName(), mUuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                mSocket = mServerSocket.accept();
                callbackConnect();
                loop();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                cancel();
            }
        }

        private void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void callbackConnect(){
        UnityPlayer.UnitySendMessage(connectCallbackGameObject,connectDelegateMethod,"");
    }


    private static BtSocketLib _library = new BtSocketLib();

    private String searchCallBackGameObject;
    private String searchDelegateMethod;

    private String pairingCallbackGameObject;
    private String pairingDelegateMethod;

    private String connectCallbackGameObject;
    private String connectDelegateMethod;

    private BtSocketLib(){
        mCandidateServers = new ArrayList();
    }

    public static void startServer(String gameObjectName,String delegateMethod)
    {
        _library.connectCallbackGameObject = gameObjectName;
        _library.connectDelegateMethod = delegateMethod;
        _library.onStartServer();
    }

    public static void activeBluetooth() {
        _library.onActiveBluetooth();
    }

    public static void pairingBluetooth(String gameObjectName,String delegateMethod) {
        _library.pairingCallbackGameObject = gameObjectName;
        _library.pairingDelegateMethod = delegateMethod;
        _library.onPairingBluetooth();
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
        _library.onConnect(address);
    }

    public static void sendData(String sendedData){
        if (_library.mServerThread != null) {
            try {
                _library.mServerThread.sendData(sendedData.getBytes());
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
        if (_library.mClientThread != null) {
            try {
                _library.mClientThread.sendData(sendedData.getBytes());
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }

    public static void cancel(){
        _library.onCancel();
    }

}