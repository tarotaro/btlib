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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_CONNECTING;
import static android.media.session.PlaybackState.STATE_NONE;

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
    private ReadWriteModel mReadWriteThread;
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
                mReadWriteThread = new ReadWriteModel(mSocket);
                mReadWriteThread.start();
                callbackConnect();
                Log.w("***ClientConnect***","***connect***");
            } catch (IOException e) {
                e.printStackTrace();
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
            while (true) {
                try {
                    mSocket = mServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(mSocket != null) {
                        mReadWriteThread = new ReadWriteModel(mSocket);
                        mReadWriteThread.start();
                        Log.w("***SearverConnect***","***connect***");
                        callbackConnect();
                        cancel();
                        break;
                    }
                }
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

    public class ReadWriteModel extends Thread {
        //ソケットに対するI/O処理
        private InputStream _in;
        private OutputStream _out;
        private Queue<Byte> _readQueue;
        private Queue<Byte> _writeQueue;

        //コンストラクタの定義
        public ReadWriteModel(BluetoothSocket socket){
            try {
                //接続済みソケットからI/Oストリームをそれぞれ取得
                _in = socket.getInputStream();
                _out = socket.getOutputStream();
                _readQueue = new LinkedList<Byte>();
                _writeQueue = new LinkedList<Byte>();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        }

        public void write(byte[] buf){
            for(int i = 0;i<buf.length;i++){
                _writeQueue.add(buf[i]);
            }
        }

        public Byte[] read(int length){
            ArrayList<Byte> arrayList = new ArrayList<Byte>();
            for(int i = 0;i<length&&!_readQueue.isEmpty();i++){
                arrayList.add(_readQueue.remove());
            }
            Byte[] ret = new Byte[arrayList.size()];
            arrayList.toArray(ret);
            return ret;
        }

        public void run() {
            byte[] buf = new byte[1024];
            String rcvNum = null;
            int tmpBuf = 0;

            while(true){
                try {
                    tmpBuf = _in.read(buf);
                    if(!_writeQueue.isEmpty()){
                        ArrayList<Byte> wo = new ArrayList<Byte>();
                        for(int i = 0;!_writeQueue.isEmpty();i++){
                            byte b = _writeQueue.remove();
                            _out.write(b);
                        }
                        _out.flush();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    break;
                }
                if(tmpBuf!=0){
                    for(int i=0;i<buf.length;i++){
                        _readQueue.add(buf[i]);
                    }
                }
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
        _library.mReadWriteThread.write(sendedData.getBytes());
    }

    public static int IsConnected(){
        if(_library.mSocket.isConnected()){
            return 1;
        }else{
            return 0;
        }
    }

    public static void cancel(){
        _library.onCancel();
    }

}