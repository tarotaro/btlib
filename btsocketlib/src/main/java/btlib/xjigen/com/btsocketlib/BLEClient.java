package btlib.xjigen.com.btsocketlib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
    private boolean isMTUExtend = false;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            // ペリフェラルとの接続に成功した時点でサービスを検索する
            gatt.discoverServices();
            if(gatt.requestMtu(512)){
                isMTUExtend = true;
            }else{
                isMTUExtend = false;
            }
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            // ペリフェラルとの接続が切れた時点でオブジェクトを空にする
            if (connectedGatt != null) {
                connectedGatt.disconnect();
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
        super.onCharacteristicWrite(gatt,characteristic,status);
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
        super.onCharacteristicRead(gatt,characteristic,status);
        String ch = characteristic.getUuid().toString();
        if(status==BluetoothGatt.GATT_SUCCESS){
                if(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
                    byte[] rd = characteristic.getValue();
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

    /*@Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt,characteristic);
        String ch = characteristic.getUuid().toString();
        if(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
            outputCharacteristic = characteristic;
        }else if(BtSocketLib.CHAR_WRITE_UUID_YOU_CAN_CHANGE.equalsIgnoreCase(ch)) {
            inputCharacteristic = characteristic;

        }
    }*/


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

        /*BluetoothGattDescriptor inputdescriptor = inputCharacteristic.getDescriptor(
                UUID.fromString(BtSocketLib.CHAR_DESKW_CONFIG_UUID_YOU_CAN_CHANGE));

        //connectedGatt.setCharacteristicNotification(inputCharacteristic,true);
        inputdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        connectedGatt.writeDescriptor(inputdescriptor);*/

        outputCharacteristic = connectedGatt.
                getService(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE))
                .getCharacteristic(UUID.fromString(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE));

        /*BluetoothGattDescriptor outputdescriptor = outputCharacteristic.getDescriptor(
                UUID.fromString(BtSocketLib.CHAR_DESKR_CONFIG_UUID_YOU_CAN_CHANGE));

        connectedGatt.setCharacteristicNotification(outputCharacteristic,true);
        outputdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        connectedGatt.writeDescriptor(outputdescriptor);*/

        readThread  = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(isReadReturn) {

                        if(connectedGatt.readCharacteristic(outputCharacteristic)) {
                            isReadReturn = false;
                        }else{
                            isWriteReturn = true;
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
                    if(isWriteReturn) {
                        if (_writeQueue != null && _writeQueue.size() != 0) {
                            int size = _writeQueue.size() > 16 ? 16 : _writeQueue.size();
                            byte[] wroteData = new byte[size];
                            wlock.lock();
                            try {
                                for (int i = 0; i < size ; i++) {
                                    wroteData[i] = _writeQueue.peek();
                                }
                            } finally {
                                wlock.unlock();
                            }


                            inputCharacteristic.setValue(wroteData);
                            if(connectedGatt.writeCharacteristic(inputCharacteristic)) {
                                isWriteReturn = false;
                                wlock.lock();
                                try {
                                    for (int i = 0; i < size; i++) {
                                        _writeQueue.poll();
                                    }
                                } finally {
                                    wlock.unlock();
                                }

                            }else{
                                isWriteReturn = true;
                            }
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
