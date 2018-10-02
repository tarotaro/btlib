package btlib.xjigen.com.btsocketlib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class BLEClient extends BluetoothGattCallback {
    private BluetoothGatt connectedGatt;
    public ConnectInterface connectInterface;
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private BluetoothGattCharacteristic inputCharacteristic;
    private BluetoothGattCharacteristic outputCharacteristic;
    private Thread readThread;
    private Thread writeThread;
    private Lock rlock;
    private Lock wlock;
    private boolean isConnect = false;
    private boolean isReadReturn = true;
    private boolean isWriteReturn = true;

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

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d("bluetooth", "onCharacteristicWrite: " + status);
        String ch = characteristic.getUuid().toString();
        if(status==BluetoothGatt.GATT_SUCCESS) {
            if (BtSocketLib.CHAR_WRITE_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
                isWriteReturn = true;
            }
        }

    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        String ch = characteristic.getUuid().toString();
        if(status==BluetoothGatt.GATT_SUCCESS){
                if(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
                    byte[] rd = characteristic.getValue();
                    Log.w("bluetooth_cl", "onCharacteristicRead: " + status);
                    if (rd != null && rd.length != 0) {
                        rlock.lock();
                        try {
                            for (int i = 0; i < rd.length; i++) {
                                _readQueue.add(rd[i]);
                            }
                        } finally {
                            rlock.unlock();
                            isReadReturn = true;
                        }
                    }
                }
        }
    }

    private void initialWriteAndRead(){
        //characteristic を取得しておく
        _readQueue = new LinkedList<Byte>();
        _writeQueue = new LinkedList<Byte>();
        rlock = new ReentrantLock();
        wlock = new ReentrantLock();
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
                    if(isReadReturn) {
                        connectedGatt.readCharacteristic(outputCharacteristic);
                        isReadReturn = false;
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
                    if(isWriteReturn) {
                        if (_writeQueue != null && _writeQueue.size() != 0) {
                            int size = _writeQueue.size() > 128 ? 128 : _writeQueue.size();
                            byte[] wroteData = new byte[size];
                            wlock.lock();
                            try {
                                for (int i = 0; i < size && i < 128; i++) {
                                    wroteData[i] = _writeQueue.remove();
                                }
                            } finally {
                                wlock.unlock();
                            }

                            inputCharacteristic.setValue(wroteData);
                            connectedGatt.writeCharacteristic(inputCharacteristic);
                            isWriteReturn = false;
                        }
                    }
                    if(isConnect != true){
                        break;
                    }
                }
            }
        });
        writeThread.start();
    }




    public  Queue<Byte> getReadQueueLock() {
        rlock.lock();
        return _readQueue;
    }

    public void readQueueUnlock(){
        Log.w("calced","added:readQueSize:"+ _readQueue.size());
        rlock.unlock();
    }

    public void addWriteQueue(Queue<Byte> writeQueue)
    {
        wlock.lock();
        try {
            this._writeQueue.addAll(writeQueue);
        }finally {
            wlock.unlock();
        }
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
