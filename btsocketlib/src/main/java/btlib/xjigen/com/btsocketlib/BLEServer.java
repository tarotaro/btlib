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

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEServer extends BluetoothGattServerCallback {
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private boolean _isQueueReaded;
    private BluetoothDevice _connectedDevice;
    public ConnectInterface connectInterface;
    //BLE
    private BluetoothGattServer bluetoothGattServer;
    public BLEServer(ConnectInterface _connectInterface) {
        connectInterface = _connectInterface;

        _readQueue = new LinkedList<Byte>();
        _writeQueue = new LinkedList<Byte>();
        _isQueueReaded = false;
    }

    public BluetoothDevice getConnectedDevice() {
        return _connectedDevice;
    }

    public void setGattServer(BluetoothGattServer _gattServer){
        bluetoothGattServer = _gattServer;
    }

    public  Queue<Byte> getReadQueue() {
        Log.w("xjigen","********get data*******");
        return this._readQueue;
    }

    public void addWriteQueue(Queue<Byte> writeQueue)
    {
        this._writeQueue.addAll(writeQueue);
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
                                            int offset, BluetoothGattCharacteristic characteristic) {

        if(_writeQueue == null || _writeQueue.isEmpty()) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            return;
        }

        final BluetoothGattCharacteristic ch = characteristic;
        final android.bluetooth.BluetoothDevice dev  = device;
        final int offs = offset;
        final int reqId = requestId;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                    while (!_writeQueue.isEmpty()) {
                        byte[] wa = new byte[128];
                        for (int i = 0; i < 128 && !_writeQueue.isEmpty(); i++) {
                            wa[i] = _writeQueue.remove();
                        }
                        Log.w("xjigen","********reading data*******");
                        ch.setValue(wa);
                        bluetoothGattServer.sendResponse(dev, reqId, BluetoothGatt.GATT_SUCCESS, offs,
                                ch.getValue());
                        Log.w("xjigen","********read data*******");
                        break;
                    }
            }
        });
        thread.start();

    }

    //セントラル（クライアント）からWriteRequestが来ると呼ばれる
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {

        for(int i = 0;i<value.length;i++){
            _readQueue.add(value[i]);
        }
        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
    }

}
