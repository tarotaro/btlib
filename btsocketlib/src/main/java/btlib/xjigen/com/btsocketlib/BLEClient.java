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
    private Queue<Byte> _requestMode;
    private BluetoothGattCharacteristic inputCharacteristic;
    private BluetoothGattCharacteristic outputCharacteristic;
    private Thread readThread;
    private Thread writeThread;
    //private Lock rlock;
    //private Lock wlock;
    private boolean isConnect = false;
    private boolean isReadReturn = true;
    private boolean isWriteReturn = true;
    private int isReadMTUExtend = 0;
    private int isWriteMTUExtend = 0;
    private int valueMTU = 512;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            // ペリフェラルとの接続に成功した時点でサービスを検索する
            gatt.discoverServices();
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
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status){
        if (status == BluetoothGatt.GATT_SUCCESS){
           Byte mode = _requestMode.remove();
           if(mode == 1){
               valueMTU = mtu;
               isReadMTUExtend = 2;
           }else if(mode == 2) {
               valueMTU = mtu;
               isWriteMTUExtend = 2;
           }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt,characteristic,status);
        //Log.d("bluetooth", "onCharacteristicWrite: " + status);
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
                        //rlock.lock();
                        try {
                            for (int i = 0; i < rd.length; i++) {
                                _readQueue.add(rd[i]);
                            }
                        } finally {
                            //rlock.unlock();
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
        _requestMode = new LinkedList<Byte>();
        //rlock = new ReentrantLock();
        //wlock = new ReentrantLock();
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
                    try {
                        Thread.sleep(40);
                    }catch (Exception e){

                    }
                    if(isReadReturn) {
                        if(isReadMTUExtend == 0) {
                            isReadMTUExtend = 1;
                            _requestMode.add((byte)1);
                            connectedGatt.requestMtu(valueMTU);
                        }
                        if(isReadMTUExtend == 2) {
                            if (connectedGatt.readCharacteristic(outputCharacteristic)) {
                                isReadReturn = false;
                                isReadMTUExtend = 0;
                            } else {
                                isReadReturn = true;
                                isReadMTUExtend = 2;
                            }
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
                    try {
                        Thread.sleep(40);
                    }catch (Exception e){

                    }
                    if(isWriteReturn) {
                        if(isWriteMTUExtend == 0) {
                            _requestMode.add((byte)2);
                            connectedGatt.requestMtu(valueMTU);
                            isWriteMTUExtend = 1;
                        }
                        if(isWriteMTUExtend == 2) {
                            if (_writeQueue != null && _writeQueue.size() != 0) {
                                int size = _writeQueue.size() > valueMTU/2 ? valueMTU/2 : _writeQueue.size();
                                byte[] wroteData = new byte[size];
                                //wlock.lock();
                                try {
                                    for (int i = 0; i < size; i++) {
                                        wroteData[i] = _writeQueue.peek();
                                    }
                                } finally {
                                    //wlock.unlock();
                                }


                                inputCharacteristic.setValue(wroteData);
                                if (connectedGatt.writeCharacteristic(inputCharacteristic)) {
                                    isWriteReturn = false;
                                    isWriteMTUExtend = 0;
                                    //wlock.lock();
                                    try {
                                        for (int i = 0; i < size; i++) {
                                            _writeQueue.poll();
                                        }
                                    } finally {
                                        //wlock.unlock();
                                    }

                                } else {
                                    isWriteReturn = true;
                                    isWriteMTUExtend = 2;
                                }
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
        //rlock.lock();
        return _readQueue;
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
