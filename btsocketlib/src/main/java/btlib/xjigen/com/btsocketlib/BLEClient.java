package btlib.xjigen.com.btsocketlib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;


public class BLEClient extends BluetoothGattCallback {
    private BluetoothGatt connectedGatt;
    public ConnectInterface connectInterface;
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private BluetoothGattCharacteristic inputCharacteristic;
    private BluetoothGattCharacteristic outputCharacteristic;
    private Thread readThread;
    private Thread writeThread;
    private boolean isConnect = false;

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
        isConnect = true;
        inputCharacteristic = connectedGatt.
              getService(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE))
              .getCharacteristic(UUID.fromString(BtSocketLib.CHAR_WRITE_UUID_YOU_CAN_CHANGE));

        outputCharacteristic = connectedGatt.
                getService(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE))
                .getCharacteristic(UUID.fromString(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE));

        readThread  = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    connectedGatt.readCharacteristic(outputCharacteristic);
                    byte[] rd = outputCharacteristic.getValue();
                    if (rd != null && rd.length != 0){
                        for(int i = 0 ;i < rd.length;i++){
                            _readQueue.add(rd[i]);
                        }
                    }
                    if(isConnect != true){
                        break;
                    }
                }
            }
        });
        readThread.start();

        writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                        if (_writeQueue != null && _writeQueue.size() != 0) {
                            int size = _writeQueue.size() > 128 ? 128 : _writeQueue.size();
                            byte[] wroteData = new byte[size];
                            for (int i = 0; i < size && i < 128; i++) {
                                wroteData[i] = _writeQueue.remove();
                            }
                            inputCharacteristic.setValue(wroteData);
                            connectedGatt.writeCharacteristic(inputCharacteristic);
                        }
                    if(isConnect != true){
                        break;
                    }
                }
            }
        });
        writeThread.start();
    }




    public  Queue<Byte> getReadQueue() {
        return _readQueue;
    }

    public void addWriteQueue(Queue<Byte> writeQueue)
    {
        this._writeQueue.addAll(writeQueue);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt,status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            connectedGatt = gatt;
            initialWriteAndRead();
            if(connectInterface != null) {
                isConnect = true;
                connectInterface.onConnect();
            }
        }
    }



    public void disConnect(){
        if(connectedGatt != null) {
            connectedGatt.disconnect();
        }
        isConnect = false;
    }



}
