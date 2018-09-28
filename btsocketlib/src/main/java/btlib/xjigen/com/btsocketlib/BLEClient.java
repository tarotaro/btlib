package btlib.xjigen.com.btsocketlib;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class BLEClient extends BluetoothGattCallback {
    private BluetoothGatt connectedGatt;
    public ConnectInterface connectInterface;
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private BluetoothGattCharacteristic inputOutputCharacteristic;
    private Thread readThread;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            // ペリフェラルとの接続に成功した時点でサービスを検索する
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // ペリフェラルとの接続が切れた時点でオブジェクトを空にする
            if (connectedGatt != null) {
                connectedGatt.close();
                connectedGatt = null;
            }
            if(connectInterface != null) {
                connectInterface.onDisConnect();
            }
        }
    }

    private void initialWriteAndRead(){
        //characteristic を取得しておく
        _readQueue = new LinkedList<Byte>();
        _writeQueue = new LinkedList<Byte>();
        inputOutputCharacteristic = connectedGatt.
              getService(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE))
              .getCharacteristic(UUID.fromString(BtSocketLib.CHAR_UUID_YOU_CAN_CHANGE));

        readThread  = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] rd = inputOutputCharacteristic.getValue();
                    if (rd == null && rd.length != 0){
                        for(int i = 0 ;i < rd.length;i++){
                            _readQueue.add(rd[i]);
                        }
                    }
                }
            }
        });
        readThread.start();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt,status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            connectedGatt = gatt;
            initialWriteAndRead();
            if(connectInterface != null) {
                connectInterface.onConnect();
            }
        }
    }



    public void disConnect(){
        if(connectedGatt != null) {
            connectedGatt.disconnect();
        }
    }



}
