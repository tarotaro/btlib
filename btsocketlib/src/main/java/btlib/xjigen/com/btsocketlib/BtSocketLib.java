package btlib.xjigen.com.btsocketlib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
                device.put(dev.getName(), dev.getAddress());
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


    private static BtSocketLib _library = new BtSocketLib();

    private String searchCallBackGameObject;
    private String searchDelegateMethod;

    private String pairingCallbackGameObject;
    private String pairingDelegateMethod;

    private BtSocketLib(){
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
        _library.searchCallBackGameObject = gameObjectName;
        _library.searchDelegateMethod = delegateMethod;
        _library.onSearchDevice();
    }
}