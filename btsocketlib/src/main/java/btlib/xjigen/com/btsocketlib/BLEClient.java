package btlib.xjigen.com.btsocketlib;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

public class BLEClient extends BluetoothGattCallback {
    private BluetoothGatt connectedGatt;
    public ConnectInterface connectInterface;
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            // ペリフェラルとの接続に成功した時点でサービスを検索する
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // ペリフェラルとの接続が切れた時点でオブジェクトを空にする
            if (connectedGatt != null) {
                connectedGatt.close();
                connectedGatt = null;
                if(connectInterface != null) {
                    connectInterface.disConnect();
                }
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            connectedGatt = gatt;
            if(connectInterface != null) {
                connectInterface.onConnect();
            }
        }
    }
}
