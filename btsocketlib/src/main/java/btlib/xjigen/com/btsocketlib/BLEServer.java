package btlib.xjigen.com.btsocketlib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import java.util.EventListener;
import java.util.LinkedList;
import java.util.Queue;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEServer extends BluetoothGattServerCallback {
    private Queue<Byte> _readQueue;
    private Queue<Byte> _writeQueue;
    private boolean _isQueueReaded;
    public ConnectInterface connectInterface;
    //BLE
    private BluetoothGattServer bluetoothGattServer;
    public BLEServer(BluetoothGattServer gattServer) {
        this.bluetoothGattServer = gattServer;
        _readQueue = new LinkedList<Byte>();
        _writeQueue = new LinkedList<Byte>();
        _isQueueReaded = false;
    }

    public  Queue<Byte> getReadQueue() {
        return this._readQueue;
    }

    public void addWriteQueue(Queue<Byte> writeQueue)
    {
        synchronized (_writeQueue) {
            this._writeQueue.addAll(writeQueue);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        if(connectInterface == null) {
            return;
        }
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            connectInterface.onConnect();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            connectInterface.disConnect();
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
                synchronized (_writeQueue) {
                    while (!_writeQueue.isEmpty()) {
                        byte[] wa = new byte[128];
                        for (int i = 0; i < 128 && !_writeQueue.isEmpty(); i++) {
                            wa[i] = _writeQueue.remove();
                        }
                        ch.setValue(wa);
                        bluetoothGattServer.sendResponse(dev, reqId, BluetoothGatt.GATT_SUCCESS, offs,
                                ch.getValue());
                    }
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
