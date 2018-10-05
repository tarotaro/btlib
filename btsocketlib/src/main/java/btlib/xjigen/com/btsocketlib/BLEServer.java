package btlib.xjigen.com.btsocketlib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEServer extends BluetoothGattServerCallback {
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private boolean _isQueueReaded;
    //private Lock rlock;
    //private Lock wlock;
    private BluetoothDevice _connectedDevice;
    public ConnectInterface connectInterface;
    //BLE
    private BluetoothGattServer bluetoothGattServer;
    public BLEServer(ConnectInterface _connectInterface) {
        connectInterface = _connectInterface;

        _readQueue = new LinkedList<Byte>();
        _writeQueue = new LinkedList<Byte>();
        //rlock = new ReentrantLock();
        //wlock = new ReentrantLock();
        _isQueueReaded = false;
    }

    public BluetoothDevice getConnectedDevice() {
        return _connectedDevice;
    }

    public void setGattServer(BluetoothGattServer _gattServer){
        bluetoothGattServer = _gattServer;
    }

    public  Queue<Byte> getReadQueueLock() {
        //rlock.lock();
        return this._readQueue;
    }

    public void readQueueUnlock(){
        //rlock.unlock();
    }

    public void addWriteQueue(Queue<Byte> writeQueue)
    {
        //wlock.lock();
        try {
            this._writeQueue.addAll(writeQueue);
        }finally {
            //wlock.unlock();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device,status,newState);
        if(connectInterface == null) {
            return;
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            _connectedDevice = device;
            boolean isConnect = bluetoothGattServer.connect(device,false);
            connectInterface.onConnect();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            connectInterface.onDisConnect();
        }

    }


    //セントラル（クライアント）からReadRequestが来ると呼ばれる
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                            int offset, final BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device,requestId,offset,characteristic);

        if(_writeQueue == null || _writeQueue.isEmpty()) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            return;
        }

        String ch = characteristic.getUuid().toString();
        if(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {

            int size = _writeQueue.size() > BtSocketLib.SEND_DATA_SIZE_MAX ? BtSocketLib.SEND_DATA_SIZE_MAX : _writeQueue.size();
            byte[] wa = new byte[size];
            //wlock.lock();
            try {
                for (int i = 0; i < size; i++) {
                    wa[i] = _writeQueue.remove();
                }
            } finally {
                //wlock.unlock();
            }
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    wa);
        }else {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    null);
        }

    }

    //セントラル（クライアント）からWriteRequestが来ると呼ばれる
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device,requestId,characteristic,preparedWrite,responseNeeded,offset,value);

        String ch = characteristic.getUuid().toString();
        if(BtSocketLib.CHAR_WRITE_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
            //rlock.lock();
            try {
                for (int i = 0; i < value.length; i++) {
                    _readQueue.add(value[i]);
                }
            } finally {
                //rlock.unlock();
            }

        }
        if(responseNeeded) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId
            , BluetoothGattDescriptor descriptor, boolean preparedWrite
            , boolean responseNeeded, int offset, byte[] value) {
        // ここに処理を記述. 接続後1回しか呼ばれない.
        super.onDescriptorWriteRequest(device,requestId,descriptor,preparedWrite,responseNeeded,offset,value);

        if(responseNeeded){
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
        }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
            int offset, BluetoothGattDescriptor descriptor)
            {
        // ここに処理を記述. 接続後1回しか呼ばれない.
        super.onDescriptorReadRequest(device,requestId,offset,descriptor);
    }

}
