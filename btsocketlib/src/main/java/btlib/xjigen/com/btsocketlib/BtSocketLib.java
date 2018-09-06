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
    private static final int REQUEST_DISCOVERABLE_BT = 5000;
    private static final int WAIT_TIME = 120;
    private static ArrayList<BluetoothDevice> mCandidateServers;

    private final UUID mUuid = UUID.fromString("5726CA0C-A6F6-4B05-A178-8070D54A91C0");
    private ServerThread mServerThread;    //サーバー用のスレッド
    private ClientThread mClientThread;    //クライアント用のスレッド


    public void onActiveBluetooth() {
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
    public void onPairingBluetooth(Activity activity) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, WAIT_TIME);
        activity.startActivityForResult(intent, REQUEST_DISCOVERABLE_BT);
    }


    /**
     * デバイスをSearchさせる
     */
    public void onSearchDevice(Activity activity) {
        mCandidateServers.clear();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
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
                String[] ret = null;
                ret = mCandidateServers.toArray(new String[mCandidateServers.size()]);
                //callBackEndSearch(ret);
            }
        }
    };


    private static BtSocketLib _library = new BtSocketLib();

    private BtSocketLib(){
    }

    public static void activeBluetooth() {
        _library.onActiveBluetooth();
    }

    public static void pairingBluetooth(Activity activity) {
        _library.onPairingBluetooth(activity);
    }

    public static void searchDevice(Activity activity) {
        _library.onSearchDevice(activity);
    }
}